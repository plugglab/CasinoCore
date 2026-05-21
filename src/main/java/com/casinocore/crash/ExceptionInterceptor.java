package com.casinocore.crash;

import com.destroystokyo.paper.event.server.ServerExceptionEvent;
import com.destroystokyo.paper.exception.ServerException;
import com.destroystokyo.paper.exception.ServerPluginException;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.List;

/**
 * Central integration point for the corrupted stacktrace system.
 */
public final class ExceptionInterceptor implements Listener {

    private static final boolean ENABLED = true;
    private static final boolean INTERCEPT_COMMANDS = true;
    private static final boolean INSTALL_UNCAUGHT_HANDLER = true;
    private static final boolean ANSI = true;
    private static final boolean RED_BACKGROUND = true;
    private static final boolean UNICODE_NOISE = true;
    private static final boolean ANTI_DECOMPILER_ENABLED = true;
    private static final boolean ANTI_DECOMPILER_CRASH = false;

    private final Plugin plugin;
    private final CorruptedLogger logger;

    public ExceptionInterceptor(Plugin plugin) {
        this.plugin = plugin;
        this.logger = new CorruptedLogger(plugin);
    }

    public void initialize() {
        if (!ENABLED) {
            return;
        }

        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        if (INSTALL_UNCAUGHT_HANDLER) {
            installUncaughtHandler();
        }

        if (ANTI_DECOMPILER_ENABLED) {
            new AntiDecompiler(plugin, logger).enforce(ANTI_DECOMPILER_CRASH);
        }

        logger.info("corrupted exception renderer armed");
    }

    public void bindCommand(String commandName, CommandExecutor executor, TabCompleter completer) {
        PluginCommand command = plugin.getServer().getPluginCommand(commandName);
        if (command == null) {
            return;
        }

        if (!ENABLED || !INTERCEPT_COMMANDS) {
            command.setExecutor(executor);
            if (completer != null) {
                command.setTabCompleter(completer);
            }
            return;
        }

        command.setExecutor(wrap(executor, "command/" + commandName));
        if (completer != null) {
            command.setTabCompleter(wrap(completer, "tab/" + commandName));
        }
    }

    public Runnable wrap(Runnable runnable, String context) {
        return () -> {
            try {
                runnable.run();
            } catch (Throwable throwable) {
                intercept(throwable, context);
                throw throwable;
            }
        };
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onServerException(ServerExceptionEvent event) {
        if (!ENABLED) {
            return;
        }

        ServerException serverException = event.getException();
        Throwable cause = serverException.getCause() != null ? serverException.getCause() : serverException;
        intercept(cause, describe(serverException));
    }

    public void intercept(Throwable throwable, String context) {
        CrashFormatter formatter = new CrashFormatter(
            plugin,
            ANSI,
            RED_BACKGROUND,
            UNICODE_NOISE,
            System.nanoTime() ^ throwable.hashCode()
        );
        List<String> lines = formatter.format(throwable, context);
        logger.dump(lines);
    }

    private void installUncaughtHandler() {
        UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            intercept(throwable, "uncaught/" + thread.getName());
            if (previous != null) {
                previous.uncaughtException(thread, throwable);
            }
        });
    }

    private String describe(ServerException serverException) {
        if (serverException instanceof ServerPluginException pluginException) {
            return "paper/" + pluginException.getResponsiblePlugin().getName();
        }
        return "paper/server";
    }

    private CommandExecutor wrap(CommandExecutor delegate, String context) {
        return new CommandExecutor() {
            @Override
            public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                                     @NotNull String label, @NotNull String[] args) {
                try {
                    return delegate.onCommand(sender, command, label, args);
                } catch (Throwable throwable) {
                    intercept(throwable, context);
                    throw throwable;
                }
            }
        };
    }

    private TabCompleter wrap(TabCompleter delegate, String context) {
        return (sender, command, alias, args) -> {
            try {
                return delegate.onTabComplete(sender, command, alias, args);
            } catch (Throwable throwable) {
                intercept(throwable, context);
                throw throwable;
            }
        };
    }
}
