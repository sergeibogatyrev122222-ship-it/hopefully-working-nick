package com.nickplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class NickCommand implements CommandExecutor, TabCompleter {

    private final NickPlugin plugin;
    private final NickManager nickManager;

    public NickCommand(NickPlugin plugin, NickManager nickManager) {
        this.plugin = plugin;
        this.nickManager = nickManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use /nick.");
            return true;
        }

        Player player = (Player) sender;

        if (!nickManager.canUseNick(player)) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use /nick.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /nick <name> | /nick off"
                    + (nickManager.canGiveNick(player) ? " | /nick give <player>" : ""));
            return true;
        }

        // /nick give <player>
        if (args[0].equalsIgnoreCase("give")) {
            if (!nickManager.canGiveNick(player)) {
                player.sendMessage(ChatColor.RED + "You don't have permission to grant /nick access.");
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(ChatColor.YELLOW + "Usage: /nick give <playername>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage(ChatColor.RED + "Player '" + args[1] + "' is not online.");
                return true;
            }
            if (nickManager.isGranted(target.getUniqueId())) {
                player.sendMessage(ChatColor.YELLOW + target.getName() + " already has /nick access.");
                return true;
            }
            nickManager.grantNick(target.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "Granted /nick access to " + target.getName() + ".");
            target.sendMessage(ChatColor.GREEN + "You have been granted access to use /nick!");
            return true;
        }

        // /nick off
        if (args[0].equalsIgnoreCase("off")) {
            if (!nickManager.hasNick(player.getUniqueId())) {
                player.sendMessage(ChatColor.YELLOW + "You don't have an active nickname.");
                return true;
            }
            nickManager.removeNick(player);
            player.sendMessage(ChatColor.GREEN + "Your nickname has been removed. You are now known as "
                    + ChatColor.WHITE + player.getName() + ChatColor.GREEN + ".");
            return true;
        }

        // /nick <name>
        String nick = args[0];

        if (nick.length() < 1 || nick.length() > 16) {
            player.sendMessage(ChatColor.RED + "Nickname must be between 1 and 16 characters.");
            return true;
        }
        if (!nick.matches("[a-zA-Z0-9_]+")) {
            player.sendMessage(ChatColor.RED + "Nickname can only contain letters, numbers, and underscores.");
            return true;
        }

        Player existing = Bukkit.getPlayerExact(nick);
        if (existing != null && !existing.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "That name is already taken by another player.");
            return true;
        }

        nickManager.setNick(player, nick);
        player.sendMessage(ChatColor.GREEN + "Your nickname has been set to "
                + ChatColor.WHITE + nick + ChatColor.GREEN + ".");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) return new ArrayList<>();
        Player player = (Player) sender;

        if (args.length == 1) {
            List<String> options = new ArrayList<>(Arrays.asList("off"));
            if (nickManager.canGiveNick(player)) options.add("give");
            return options.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give") && nickManager.canGiveNick(player)) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}
