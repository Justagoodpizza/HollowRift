package com.hollowrift.combat;

import com.hollowrift.HollowRift;
import com.hollowrift.items.ItemManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RiftbladeListener implements Listener {

    private final HollowRift plugin;
    private final ItemManager itemManager;

    // Track resonance stacks: playerUUID -> targetUUID -> (count, lastHitTime)
    private final Map<UUID, Map<UUID, ResonanceData>> resonanceStacks = new HashMap<>();

    // Phase Slash cooldowns: playerUUID -> expiryTime
    private final Map<UUID, Long> phaseSlashCooldowns = new HashMap<>();

    private static final int RESONANCE_THRESHOLD = 3; // Every 3rd hit
    private static final long RESONANCE_TIMEOUT_MS = 4000; // 4 seconds
    private static final long PHASE_SLASH_COOLDOWN_MS = 15000; // 15 seconds
    private static final double PHASE_SLASH_DASH_DISTANCE = 8.0;
    private static final double PHASE_SLASH_DAMAGE = 8.0;
    private static final double PHASE_SLASH_RADIUS = 2.0;
    private static final int SPEED_DURATION_TICKS = 40; // 2 seconds

    public RiftbladeListener(HollowRift plugin) {
        this.plugin = plugin;
        this.itemManager = plugin.getItemManager();
    }

    // ==================== PASSIVE: Resonance Stacks ====================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (!itemManager.isRiftblade(mainHand)) return;

        UUID playerId = player.getUniqueId();
        UUID targetId = target.getUniqueId();
        long now = System.currentTimeMillis();

        // Get or create player's resonance map
        Map<UUID, ResonanceData> playerTargets = resonanceStacks.computeIfAbsent(playerId, k -> new HashMap<>());
        ResonanceData data = playerTargets.get(targetId);

        // Reset if target switched or timeout exceeded
        if (data == null || now - data.lastHitTime > RESONANCE_TIMEOUT_MS) {
            data = new ResonanceData();
            playerTargets.put(targetId, data);
        }

        // Increment stack
        data.stackCount++;
        data.lastHitTime = now;

        // Clean up old entries for this player (targets not hit recently)
        playerTargets.entrySet().removeIf(entry -> now - entry.getValue().lastHitTime > RESONANCE_TIMEOUT_MS);

        // Check for resonance proc (every 3rd hit)
        if (data.stackCount >= RESONANCE_THRESHOLD) {
            triggerResonance(player, target);
            data.stackCount = 0; // Reset after proc
        }
    }

    private void triggerResonance(Player player, LivingEntity target) {
        // Safety checks
        if (target == null || !target.isValid() || target.isDead()) return;
        if (player == null || !player.isValid()) return;
        
        Location loc = target.getLocation().add(0, target.getHeight() / 2.0, 0);

        // Sound & Particles
        loc.getWorld().playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 1.0f);
        loc.getWorld().spawnParticle(Particle.SONIC_BOOM, loc, 1);

        // Armor-piercing damage: 6.0 extra damage + knockback
        // Use direct health reduction to bypass armor, but clamp at 0.1
        double currentHealth = target.getHealth();
        double newHealth = Math.max(0.1, currentHealth - 6.0);
        
        // Check if this would kill the target
        boolean wouldKill = newHealth <= 0.1;
        target.setHealth(newHealth);
        
        // Apply knockback away from player (even if dead, for effect)
        Vector knockback = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(0.8);
        knockback.setY(0.4);
        target.setVelocity(target.getVelocity().add(knockback));

        // If this would kill, apply tiny damage for kill credit/loot
        if (wouldKill && target.isValid() && !target.isDead()) {
            target.damage(0.1, player);
        }

        // Visual feedback to player
        player.sendActionBar(Component.text("⚡ Resonance Boom!", NamedTextColor.AQUA, TextDecoration.BOLD));
    }

    // ==================== ACTIVE: Phase Slash ====================

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only main hand right-click (not off-hand, not block interaction)
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction().name().startsWith("LEFT_")) return;
        if (event.getAction().name().startsWith("RIGHT_CLICK_BLOCK")) return; // Allow RIGHT_CLICK_AIR only

        Player player = event.getPlayer();
        ItemStack mainHand = player.getInventory().getItemInMainHand();

        if (!itemManager.isRiftblade(mainHand)) return;

        long now = System.currentTimeMillis();
        Long cooldownExpiry = phaseSlashCooldowns.get(player.getUniqueId());

        if (cooldownExpiry != null && now < cooldownExpiry) {
            long remaining = (cooldownExpiry - now + 999) / 1000; // Round up to seconds
            player.sendActionBar(Component.text("⏳ Phase Slash cooldown: " + remaining + "s", NamedTextColor.RED));
            return;
        }

        // Execute Phase Slash
        executePhaseSlash(player);

        // Set cooldown
        phaseSlashCooldowns.put(player.getUniqueId(), now + PHASE_SLASH_COOLDOWN_MS);

        // Schedule action bar cooldown countdown
        new BukkitRunnable() {
            int secondsLeft = (int) (PHASE_SLASH_COOLDOWN_MS / 1000);
            @Override
            public void run() {
                if (!player.isOnline() || !player.isValid()) {
                    cancel();
                    return;
                }
                if (secondsLeft <= 0) {
                    player.sendActionBar(Component.text("✦ Phase Slash ready!", NamedTextColor.GREEN, TextDecoration.BOLD));
                    cancel();
                } else {
                    player.sendActionBar(Component.text("⏳ Phase Slash: " + secondsLeft + "s", NamedTextColor.YELLOW));
                    secondsLeft--;
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Every second
    }

    private void executePhaseSlash(Player player) {
        Location start = player.getLocation().clone();
        Vector direction = player.getLocation().getDirection().normalize();
        
        // Calculate end position (8 blocks forward)
        Location end = start.clone().add(direction.clone().multiply(PHASE_SLASH_DASH_DISTANCE));

        // Safe vertical boost: find safe Y at destination
        end = findSafeDestination(start, end);

        // Dash parameters: 8 blocks over 10 ticks = 0.8 blocks/tick
        final int dashTicks = 10;
        final double speed = PHASE_SLASH_DASH_DISTANCE / dashTicks; // 0.8 blocks/tick
        final Vector dashVelocity = direction.clone().multiply(speed);
        dashVelocity.setY(0.3); // Small upward boost

        // Dash over multiple ticks using a runnable
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isValid() || !player.isOnline() || ticks >= dashTicks) {
                    cancel();
                    return;
                }
                
                // Apply velocity each tick
                player.setVelocity(dashVelocity);
                player.setFallDistance(0); // Prevent fall damage
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L); // Every tick

        // Spawn SCULK_SOUL particles along path
        spawnPathParticles(start, end);

        // Damage enemies along path
        damageEnemiesAlongPath(player, start, end, direction);

        // Give Speed II for 2 seconds
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, SPEED_DURATION_TICKS, 1, false, false, true));

        // Sound feedback
        start.getWorld().playSound(start, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
        end.getWorld().playSound(end, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);

        player.sendActionBar(Component.text("⚔ Phase Slash!", NamedTextColor.DARK_AQUA, TextDecoration.BOLD));
    }

    /**
     * Finds a safe destination by checking for blocks at the target location
     * and adjusting Y to avoid suffocation.
     */
    private Location findSafeDestination(Location start, Location preferred) {
        Location test = preferred.clone();
        
        // Check if destination is inside a block
        for (int yOffset = 0; yOffset <= 3; yOffset++) {
            test.setY(preferred.getY() + yOffset);
            if (isLocationClear(test)) {
                return test;
            }
        }
        
        // Fallback: try going up from start location
        for (int y = 0; y <= 5; y++) {
            test.setY(start.getY() + y);
            if (isLocationClear(test)) {
                test.setX(preferred.getX());
                test.setZ(preferred.getZ());
                return test;
            }
        }
        
        return preferred; // Last resort
    }

    private boolean isLocationClear(Location loc) {
        // Check the block at feet and head level
        return loc.getBlock().getType().isAir() 
            && loc.clone().add(0, 1, 0).getBlock().getType().isAir()
            && loc.clone().add(0, 2, 0).getBlock().getType().isAir();
    }

    private void spawnPathParticles(Location start, Location end) {
        Vector direction = end.toVector().subtract(start.toVector()).normalize();
        double distance = start.distance(end);
        
        new BukkitRunnable() {
            double progress = 0;
            @Override
            public void run() {
                if (progress >= distance) {
                    cancel();
                    return;
                }
                Location particleLoc = start.clone().add(direction.clone().multiply(progress));
                particleLoc.getWorld().spawnParticle(Particle.SCULK_SOUL, particleLoc, 3, 0.2, 0.2, 0.2, 0.05);
                progress += 0.5;
            }
        }.runTaskTimer(plugin, 0L, 1L); // Every tick
    }

    private void damageEnemiesAlongPath(Player player, Location start, Location end, Vector direction) {
        double distance = start.distance(end);
        double step = 0.5; // Check every 0.5 blocks
        
        for (double d = 0; d <= distance; d += step) {
            Location checkLoc = start.clone().add(direction.clone().multiply(d));
            
            // Find nearby entities within radius
            for (Entity entity : checkLoc.getWorld().getNearbyEntities(checkLoc, PHASE_SLASH_RADIUS, PHASE_SLASH_RADIUS, PHASE_SLASH_RADIUS)) {
                if (entity instanceof LivingEntity living 
                    && !entity.equals(player) 
                    && !entity.hasMetadata("NPC")
                    && living.isValid()
                    && !living.isDead()) {
                    
                    // Apply damage
                    living.damage(PHASE_SLASH_DAMAGE, player);
                    
                    // Small knockback (only if still valid after damage)
                    if (living.isValid() && !living.isDead()) {
                        Vector kb = entity.getLocation().toVector().subtract(checkLoc.toVector()).normalize().multiply(0.5);
                        kb.setY(0.3);
                        living.setVelocity(living.getVelocity().add(kb));
                    }
                    
                    // Particle on hit
                    if (entity.isValid()) {
                        entity.getWorld().spawnParticle(Particle.SCULK_SOUL, entity.getLocation().add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0.1);
                    }
                }
            }
        }
    }

    // ==================== Cleanup ====================

    /**
     * Call this on plugin disable to clean up tasks.
     */
    public void cleanup() {
        resonanceStacks.clear();
        phaseSlashCooldowns.clear();
    }

    // ==================== Inner Classes ====================

    private static class ResonanceData {
        int stackCount = 0;
        long lastHitTime = 0;
    }
}