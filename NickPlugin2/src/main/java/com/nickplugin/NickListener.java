package com.nickplugin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class NickListener implements Listener {

    private final NickManager nickManager;

    public NickListener(NickManager nickManager) {
        this.nickManager = nickManager;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Reload and reapply saved nick on join
        nickManager.loadNickForPlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Nick is saved to file — no need to clear on quit
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        String deathMessage = event.getDeathMessage();
        if (deathMessage == null) return;

        // Replace victim real name with nick in death message
        if (nickManager.hasNick(victim.getUniqueId())) {
            deathMessage = deathMessage.replace(victim.getName(), nickManager.getNick(victim.getUniqueId()));
        }

        // Replace killer real name with nick in death message
        if (victim.getKiller() != null) {
            Player killer = victim.getKiller();
            if (nickManager.hasNick(killer.getUniqueId())) {
                deathMessage = deathMessage.replace(killer.getName(), nickManager.getNick(killer.getUniqueId()));
            }
        }

        event.setDeathMessage(deathMessage);
    }
}
