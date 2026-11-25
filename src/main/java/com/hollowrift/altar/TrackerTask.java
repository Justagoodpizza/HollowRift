package com.hollowrift.altar;

import com.hollowrift.HollowRift;
import com.hollowrift.items.ItemManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class TrackerTask extends BukkitRunnable {

    private final HollowRift plugin;
    private final ItemManager itemManager;

    // Ping interval: 5 minutes = 5 * 60 * 20 = 6000 ticks
    private static final long PING_INTERVAL_TICKS = 6000L;
    // Round coordinates to nearest 250 blocks
    private static final int ROUNDING_FACTOR = 250;

    public TrackerTask(HollowRift plugin) {
        this.plugin = plugin;
        this.itemManager = plugin.getItemManager();
    }

    @Override
    public void run() {
        if (!plugin.isEnabled()) {
            cancel();
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (hasRiftblade(player)) {
                broadcastRiftbladeLocation(player);
            }
        }
    }

    private boolean hasRiftblade(Player player) {
        // Check main inventory
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && itemManager.isRiftblade(item)) {
                return true;
            }
        }
        // Check ender chest
        for (ItemStack item : player.getEnderChest().getContents()) {
            if (item != null && itemManager.isRiftblade(item)) {
                return true;
            }
        }
        return false;
    }

    private void broadcastRiftbladeLocation(Player player) {
        Location loc = player.getLocation();
        
        // Round coordinates to nearest 250
        int roundedX = roundToNearest(loc.getBlockX(), ROUNDING_FACTOR);
        int roundedY = roundToNearest(loc.getBlockY(), ROUNDING_FACTOR);
        int roundedZ = roundToNearest(loc.getBlockZ(), ROUNDING_FACTOR);
        
        String worldName = loc.getWorld().getName();
        String coords = String.format("%d, %d, %d", roundedX, roundedY, roundedZ);

        // Broadcast hint to all players
        Bukkit.broadcast(Component.text("📡 ", NamedTextColor.AQUA)
                .append(Component.text("Riftblade resonance detected...", NamedTextColor.DARK_AQUA, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text("World: ", NamedTextColor.GRAY))
                .append(Component.text(worldName, NamedTextColor.YELLOW))
                .append(Component.text(" | Approx: ", NamedTextColor.GRAY))
                .append(Component.text(coords, NamedTextColor.YELLOW)));
    }

    private int roundToNearest(int value, int factor) {
        if (value >= 0) {
            return ((value + factor / 2) / factor) * factor;
        } else {
            return -(((-value + factor / 2) / factor) * factor);
        }
    }

    public void start() {
        this.runTaskTimer(plugin, PING_INTERVAL_TICKS, PING_INTERVAL_TICKS);
    }
}