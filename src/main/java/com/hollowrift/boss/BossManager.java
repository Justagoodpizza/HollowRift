package com.hollowrift.boss;

import com.hollowrift.HollowRift;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

public class BossManager implements Listener {

    private final HollowRift plugin;
    private HollowWardenBoss currentBoss;

    public BossManager(HollowRift plugin) {
        this.plugin = plugin;
    }

    /**
     * Spawns the Hollow Warden at the given location.
     * @param location The location to spawn the boss
     * @return The created HollowWardenBoss instance, or null if a boss already exists
     */
    public HollowWardenBoss spawnBoss(Location location) {
        if (currentBoss != null && currentBoss.getWarden().isValid() && !currentBoss.getWarden().isDead()) {
            return null; // Boss already alive
        }
        
        currentBoss = new HollowWardenBoss(plugin, this, location);
        return currentBoss;
    }

    /**
     * Called when the boss dies.
     */
    public void onBossDeath(HollowWardenBoss boss) {
        if (currentBoss == boss) {
            currentBoss = null;
        }
    }

    /**
     * Gets the currently active boss, if any.
     */
    public HollowWardenBoss getCurrentBoss() {
        if (currentBoss != null && currentBoss.getWarden().isValid() && !currentBoss.getWarden().isDead()) {
            return currentBoss;
        }
        currentBoss = null;
        return null;
    }

    /**
     * Checks if a boss is currently active.
     */
    public boolean hasActiveBoss() {
        return getCurrentBoss() != null;
    }

    // ==================== Event Listeners ====================

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        HollowWardenBoss boss = getCurrentBoss();
        if (boss == null) return;
        
        if (event.getEntity().equals(boss.getWarden())) {
            boss.onBossDamaged(event);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        HollowWardenBoss boss = getCurrentBoss();
        if (boss != null && event.getEntity().equals(boss.getWarden())) {
            boss.onDeath();
        }
    }

    // ==================== Utility ====================

    /**
     * Broadcasts a message to all players (for boss announcements).
     */
    public void broadcast(Component message) {
        plugin.getServer().broadcast(message);
    }
}