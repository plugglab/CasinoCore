package com.casinocore.utils;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class CasinoNpcListener implements Listener {

    private final CasinoNpcManager npcManager;

    public CasinoNpcListener(CasinoNpcManager npcManager) {
        this.npcManager = npcManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!npcManager.isCasinoNpc(event.getRightClicked())) {
            return;
        }

        event.setCancelled(true);
        npcManager.openNpcGame(event.getPlayer(), event.getRightClicked());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractAt(PlayerInteractAtEntityEvent event) {
        if (!npcManager.isCasinoNpc(event.getRightClicked())) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!npcManager.isCasinoNpc(event.getEntity())) {
            return;
        }

        event.setCancelled(true);
    }
}
