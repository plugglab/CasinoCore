package com.casinocore.integrations.citizens;

import com.casinocore.core.CasinoPlugin;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class CitizensCasinoListener implements Listener {

    private final CasinoPlugin plugin;

    public CitizensCasinoListener(CasinoPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRightClick(NPCRightClickEvent event) {
        NPC npc = event.getNPC();
        if (!npc.hasTrait(CasinoGameTrait.class)) {
            return;
        }

        CasinoGameTrait trait = npc.getTrait(CasinoGameTrait.class);
        if (trait.getGameName() == null || trait.getGameName().isBlank()) {
            return;
        }

        event.setCancelled(true);
        plugin.getCasinoNpcManager().openNpcGameByName(event.getClicker(), trait.getGameName());
    }
}
