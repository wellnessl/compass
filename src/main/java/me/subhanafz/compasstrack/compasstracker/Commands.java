package me.subhanafz.compasstrack.compasstracker;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class Commands implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String name = cmd.getName();

        if (!sender.hasPermission("compasstrack." + (name.equalsIgnoreCase("startTracking")
                ? "starttracking" : name))) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }

        switch (name.toLowerCase()) {
            case "givecompass":
                if (sender instanceof Player p) {
                    p.getInventory().addItem(new ItemStack(Material.COMPASS));
                }
                return true;

            case "starttracking":
                if (TrackerManager.toggleTracking()) {
                    sender.sendMessage(ChatColor.GREEN + "Tracking has been enabled!");
                } else {
                    sender.sendMessage(ChatColor.RED + "Tracking has been disabled!");
                }
                return true;

            case "addspeedrunners": {
                if (args.length == 0) {
                    sender.sendMessage(ChatColor.RED + "You do not contain any arguments in the command");
                    sender.sendMessage("/addSpeedrunners [Player]");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[0]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "This player is not inside the server!");
                    return true;
                }
                if (!CompassTracker.Speedrunners.add(target.getUniqueId())) {
                    sender.sendMessage(ChatColor.RED + "You already have this player inside the table.");
                    return true;
                }
                sender.sendMessage(ChatColor.GREEN + "This player has been added successfully!");
                return true;
            }

            case "removespeedrunners": {
                if (args.length == 0) {
                    sender.sendMessage(ChatColor.RED + "You have not specified any arguments!");
                    sender.sendMessage("/removeSpeedrunners [Player]");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[0]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "This player is not inside the server!");
                    return true;
                }
                CompassTracker.Speedrunners.remove(target.getUniqueId());
                sender.sendMessage(ChatColor.GREEN + "This player has been removed successfully!");
                return true;
            }

            case "getspeedrunners": {
                StringBuilder sb = new StringBuilder();
                for (UUID id : CompassTracker.Speedrunners) {
                    Player p = Bukkit.getPlayer(id);
                    String n = p != null ? p.getName() : Bukkit.getOfflinePlayer(id).getName();
                    if (n == null) n = id.toString();
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(n);
                }
                sender.sendMessage(sb.toString());
                return true;
            }
        }
        return false;
    }
}
