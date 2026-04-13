package com.nickplugin;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class NickManager {

    private final NickPlugin plugin;
    private final Map<UUID, String> activeNicks = new HashMap<>();
    private final Set<UUID> grantedPlayers = new HashSet<>();
    private File nickFile;
    private FileConfiguration nickData;

    public NickManager(NickPlugin plugin) {
        this.plugin = plugin;
        loadNickFile();
    }

    private void loadNickFile() {
        nickFile = new File(plugin.getDataFolder(), "nicks.yml");
        if (!nickFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                nickFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        nickData = YamlConfiguration.loadConfiguration(nickFile);
    }

    private void saveNickFile() {
        try {
            nickData.save(nickFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean canUseNick(Player player) {
        return player.hasPermission("nick.use")
                || player.hasPermission("nick.op")
                || grantedPlayers.contains(player.getUniqueId());
    }

    public boolean canGiveNick(Player player) {
        return player.hasPermission("nick.op") || player.hasPermission("nick.give");
    }

    public void setNick(Player player, String nick) {
        activeNicks.put(player.getUniqueId(), nick);
        applyNick(player, nick);
        nickData.set(player.getUniqueId().toString(), nick);
        saveNickFile();
    }

    public void applyNick(Player player, String nick) {
        Component nickComp = Component.text(nick);

        // Chat display name
        player.displayName(nickComp);

        // Tab list name
        player.playerListName(nickComp);

        // Nametag above head — use scoreboard team trick for Paper 1.21
        setNametagViaTeam(player, nick);
    }

    private void setNametagViaTeam(Player player, String nick) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = "nick_" + player.getName();

        // Remove from any existing nick team
        Team existing = scoreboard.getTeam(teamName);
        if (existing != null) {
            existing.unregister();
        }

        // Create a new team for this player's nick
        Team team = scoreboard.registerNewTeam(teamName);
        team.prefix(Component.text(nick));
        team.suffix(Component.empty());

        // Hide the real username by making prefix = nick and suffix = empty
        // and setting display name via displayName
        team.addPlayer(player);

        // Also set via Adventure API for nametag
        player.customName(Component.text(nick));
        player.setCustomNameVisible(true);
    }

    private void removeNametagTeam(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = "nick_" + player.getName();
        Team existing = scoreboard.getTeam(teamName);
        if (existing != null) {
            existing.unregister();
        }
        player.customName(null);
        player.setCustomNameVisible(false);
    }

    public void restorePlayerName(Player player) {
        Component realName = Component.text(player.getName());
        player.displayName(realName);
        player.playerListName(realName);
        removeNametagTeam(player);
    }

    public void loadNickForPlayer(Player player) {
        String savedNick = nickData.getString(player.getUniqueId().toString());
        if (savedNick != null) {
            activeNicks.put(player.getUniqueId(), savedNick);
            // Small delay to ensure player is fully loaded before applying
            Bukkit.getScheduler().runTaskLater(plugin, () -> applyNick(player, savedNick), 5L);
        }
    }

    public void removeNick(Player player) {
        activeNicks.remove(player.getUniqueId());
        nickData.set(player.getUniqueId().toString(), null);
        saveNickFile();
        restorePlayerName(player);
    }

    public boolean hasNick(UUID uuid) {
        return activeNicks.containsKey(uuid);
    }

    public String getNick(UUID uuid) {
        return activeNicks.get(uuid);
    }

    public String getDisplayName(Player player) {
        return activeNicks.getOrDefault(player.getUniqueId(), player.getName());
    }

    public void grantNick(UUID uuid) {
        grantedPlayers.add(uuid);
    }

    public boolean isGranted(UUID uuid) {
        return grantedPlayers.contains(uuid);
    }

    public void restoreAll() {
        for (UUID uuid : activeNicks.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                restorePlayerName(player);
            }
        }
        activeNicks.clear();
    }
}
