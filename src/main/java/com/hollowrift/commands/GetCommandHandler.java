package com.hollowrift.commands;

import com.hollowrift.HollowRift;
import com.hollowrift.boss.BossManager;
import com.hollowrift.boss.HollowWardenBoss;
import com.hollowrift.items.ItemManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GetCommandHandler implements CommandExecutor, TabCompleter {

    private final HollowRift plugin;
    private final ItemManager itemManager;
    private final BossManager bossManager;

    public GetCommandHandler(HollowRift plugin) {
        this.plugin = plugin;
        this.itemManager = plugin.getItemManager();
        this.bossManager = plugin.getBossManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("hollowrift.admin")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "give" -> {
                return handleGiveCommand(sender, args);
            }
            case "spawnboss" -> {
                return handleSpawnBossCommand(sender, args);
            }
            default -> {
                sender.sendMessage(Component.text("Unknown subcommand: " + subCommand, NamedTextColor.RED));
                sendUsage(sender);
                return true;
            }
        }
    }

    private boolean handleGiveCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /hollowrift give <core|blade> [player]", NamedTextColor.YELLOW));
            return true;
        }

        String itemType = args[1].toLowerCase();
        ItemStack item = itemManager.getCustomItem(itemType);

        if (item == null) {
            sender.sendMessage(Component.text("Invalid item type. Use 'core' or 'blade'.", NamedTextColor.RED));
            return true;
        }

        Player target;
        if (args.length >= 3) {
            target = plugin.getServer().getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found: " + args[2], NamedTextColor.RED));
                return true;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(Component.text("You must specify a player when running from console.", NamedTextColor.RED));
            return true;
        }

        // Give the item
        target.getInventory().addItem(item);

        String itemName = itemType.equals("core") ? "Echoing Core" : "Riftblade";
        sender.sendMessage(Component.text("Gave ", NamedTextColor.GREEN)
                .append(Component.text(itemName, NamedTextColor.AQUA))
                .append(Component.text(" to ", NamedTextColor.GREEN))
                .append(Component.text(target.getName(), NamedTextColor.YELLOW)));

        if (!target.equals(sender)) {
            target.sendMessage(Component.text("You received ", NamedTextColor.GREEN)
                    .append(Component.text(itemName, NamedTextColor.AQUA))
                    .append(Component.text(" from an admin.", NamedTextColor.GREEN)));
        }

        return true;
    }

    private boolean handleSpawnBossCommand(CommandSender sender, String[] args) {
        Player target;
        if (args.length >= 2) {
            target = plugin.getServer().getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found: " + args[1], NamedTextColor.RED));
                return true;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(Component.text("You must specify a player when running from console.", NamedTextColor.RED));
            return true;
        }

        if (bossManager.hasActiveBoss()) {
            sender.sendMessage(Component.text("A Hollow Warden is already active!", NamedTextColor.RED));
            return true;
        }

        HollowWardenBoss boss = bossManager.spawnBoss(target.getLocation());
        if (boss == null) {
            sender.sendMessage(Component.text("Failed to spawn boss (one may already exist).", NamedTextColor.RED));
            return true;
        }

        sender.sendMessage(Component.text("Spawned ", NamedTextColor.GREEN)
                .append(Component.text("The Hollow Warden", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD))
                .append(Component.text(" at ", NamedTextColor.GREEN))
                .append(Component.text(target.getName() + "'s location", NamedTextColor.YELLOW)));

        // Broadcast to server
        plugin.getServer().broadcast(Component.text("⚠ ", NamedTextColor.RED)
                .append(Component.text("The Hollow Warden has awakened at ", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD))
                .append(Component.text(target.getLocation().getBlockX() + ", " + target.getLocation().getBlockY() + ", " + target.getLocation().getBlockZ(), NamedTextColor.YELLOW))
                .append(Component.text(" in " + target.getWorld().getName(), NamedTextColor.AQUA)));

        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("HollowRift Commands:", NamedTextColor.GOLD, TextDecoration.BOLD));
        sender.sendMessage(Component.text("  /hollowrift give <core|blade> [player]", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  /hollowrift spawnboss [player]", NamedTextColor.YELLOW));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!sender.hasPermission("hollowrift.admin")) {
            return completions;
        }

        if (args.length == 1) {
            // First argument: subcommand
            completions.addAll(Arrays.asList("give", "spawnboss"));
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("give")) {
                completions.addAll(Arrays.asList("core", "blade"));
            } else if (subCommand.equals("spawnboss")) {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            // Third argument for give: player name
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                completions.add(player.getName());
            }
        }

        String lastArg = args.length > 0 ? args[args.length - 1] : "";
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(lastArg.toLowerCase()))
                .toList();
    }
}