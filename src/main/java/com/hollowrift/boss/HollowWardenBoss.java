package com.hollowrift.boss;

import com.hollowrift.HollowRift;
import com.hollowrift.items.ItemManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class HollowWardenBoss {

    private final HollowRift plugin;
    private final BossManager bossManager;
    private final Warden warden;
    private final BossBar bossBar;
    private final Location spawnLocation;

    // Shield mechanic
    private boolean shieldActive = true;
    private final List<Entity> shieldNodes = new ArrayList<>();
    private long shieldDisabledUntil = 0;
    private BukkitRunnable shieldRegenTask;

    // Mechanics tasks
    private BukkitRunnable vibrationSlamTask;
    private BukkitRunnable soulTetherTask;
    private BukkitRunnable bossBarUpdateTask;

    // Soul tether targets
    private final List<UUID> tetheredPlayers = new ArrayList<>();

    // Legacy serializer for entity custom names
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();

    // Constants
    private static final double MAX_HEALTH = 600.0;
    private static final int BOSS_BAR_RANGE = 60;
    private static final int SHIELD_NODE_COUNT = 3;
    private static final int SHIELD_NODE_RADIUS = 10;
    private static final long SHIELD_DISABLE_DURATION = 20000; // 20 seconds
    private static final int VIBRATION_SLAM_INTERVAL = 12000; // 12 seconds
    private static final int VIBRATION_SLAM_WARNING = 3000; // 3 seconds
    private static final int VIBRATION_SLAM_RADIUS = 12;
    private static final int SOUL_TETHER_INTERVAL = 15000; // 15 seconds
    private static final int SOUL_TETHER_RADIUS = 15;
    private static final double SOUL_TETHER_HEAL_PER_SEC = 10.0;

    public HollowWardenBoss(HollowRift plugin, BossManager bossManager, Location spawnLocation) {
        this.plugin = plugin;
        this.bossManager = bossManager;
        this.spawnLocation = spawnLocation.clone();
        this.warden = spawnWarden();
        this.bossBar = createBossBar();
        startMechanics();
    }

    private Warden spawnWarden() {
        Warden warden = spawnLocation.getWorld().spawn(spawnLocation, Warden.class, entity -> {
            entity.setPersistent(true);
            // Entity custom names in Paper 1.21.4 expect String, use legacy serializer
            entity.setCustomName(LEGACY_SERIALIZER.serialize(
                    Component.text("The Hollow Warden", NamedTextColor.DARK_PURPLE)
                            .decoration(TextDecoration.BOLD, true)
            ));
            entity.setCustomNameVisible(true);
            entity.setRemoveWhenFarAway(false);
            
            // Set max health
            Objects.requireNonNull(entity.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(MAX_HEALTH);
            entity.setHealth(MAX_HEALTH);
            
            // Metadata for identification
            entity.setMetadata("hollowrift_boss", new FixedMetadataValue(plugin, true));
            entity.setMetadata("hollowrift_shield_active", new FixedMetadataValue(plugin, true));
        });

        // Spawn shield nodes (glowing armor stands)
        spawnShieldNodes(warden.getLocation());
        
        return warden;
    }

    private void spawnShieldNodes(Location center) {
        for (int i = 0; i < SHIELD_NODE_COUNT; i++) {
            double angle = (2 * Math.PI * i) / SHIELD_NODE_COUNT;
            double radius = 3 + ThreadLocalRandom.current().nextDouble(2);
            Location nodeLoc = center.clone().add(
                    Math.cos(angle) * radius,
                    2 + ThreadLocalRandom.current().nextDouble(2),
                    Math.sin(angle) * radius
            );

            ArmorStand node = nodeLoc.getWorld().spawn(nodeLoc, ArmorStand.class, stand -> {
                stand.setVisible(false);
                stand.setGravity(false);
                stand.setInvulnerable(true);
                stand.setMarker(true);
                stand.setCustomNameVisible(false);
                stand.setMetadata("hollowrift_shield_node", new FixedMetadataValue(plugin, true));
            });
            
            shieldNodes.add(node);
        }
        
        // Visual effect for nodes
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!warden.isValid() || shieldNodes.isEmpty()) {
                    cancel();
                    return;
                }
                for (Entity node : shieldNodes) {
                    if (node.isValid()) {
                        node.getWorld().spawnParticle(Particle.ENCHANTED_HIT, node.getLocation(), 2, 0.2, 0.2, 0.2, 0.1);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    private BossBar createBossBar() {
        // BossBar in Paper 1.21.4 expects String title
        BossBar bar = Bukkit.createBossBar(
                "The Hollow Warden",
                BarColor.PURPLE,
                BarStyle.SOLID
        );
        bar.setProgress(1.0);
        bar.setVisible(true);
        return bar;
    }

    private void startMechanics() {
        // Boss bar update task
        bossBarUpdateTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!warden.isValid() || warden.isDead()) {
                    cleanup();
                    cancel();
                    return;
                }
                updateBossBar();
                updateShieldVisuals();
            }
        };
        bossBarUpdateTask.runTaskTimer(plugin, 0L, 20L); // Every second

        // Vibration Slam mechanic (below 50% HP)
        vibrationSlamTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!warden.isValid() || warden.isDead()) {
                    cancel();
                    return;
                }
                double healthPercent = warden.getHealth() / MAX_HEALTH;
                if (healthPercent <= 0.5) {
                    triggerVibrationSlam();
                }
            }
        };
        vibrationSlamTask.runTaskTimer(plugin, VIBRATION_SLAM_INTERVAL, VIBRATION_SLAM_INTERVAL);

        // Soul Tether mechanic (below 20% HP)
        soulTetherTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!warden.isValid() || warden.isDead()) {
                    cancel();
                    return;
                }
                double healthPercent = warden.getHealth() / MAX_HEALTH;
                if (healthPercent <= 0.2) {
                    triggerSoulTether();
                }
            }
        };
        soulTetherTask.runTaskTimer(plugin, SOUL_TETHER_INTERVAL, SOUL_TETHER_INTERVAL);
    }

    private void updateBossBar() {
        double progress = warden.getHealth() / MAX_HEALTH;
        bossBar.setProgress(progress);
        
        // Update players in range
        for (Player player : warden.getWorld().getPlayers()) {
            if (player.getLocation().distance(warden.getLocation()) <= BOSS_BAR_RANGE) {
                bossBar.addPlayer(player);
            } else {
                bossBar.removePlayer(player);
            }
        }
    }

    private void updateShieldVisuals() {
        if (shieldActive) {
            warden.getWorld().spawnParticle(Particle.ENCHANTED_HIT, warden.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0.1);
            
            // Update shield node particles
            for (Entity node : shieldNodes) {
                if (node.isValid()) {
                    node.getWorld().spawnParticle(Particle.END_ROD, node.getLocation(), 2, 0.3, 0.3, 0.3, 0.05);
                }
            }
        }
    }

    // ==================== Shield Mechanic ====================

    public void onShieldNodeDestroyed(Entity node) {
        if (!shieldNodes.remove(node)) return;
        
        // Use a sound that exists in 1.21.4
        node.getWorld().playSound(node.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);
        node.getWorld().spawnParticle(Particle.EXPLOSION, node.getLocation(), 1);
        node.remove();
        
        if (shieldNodes.isEmpty()) {
            disableShield();
        }
    }

    private void disableShield() {
        shieldActive = false;
        shieldDisabledUntil = System.currentTimeMillis() + SHIELD_DISABLE_DURATION;
        warden.setMetadata("hollowrift_shield_active", new FixedMetadataValue(plugin, false));
        
        // Broadcast shield down
        Bukkit.broadcast(Component.text("🛡 The Hollow Warden's Sonic Shield has been disabled!", NamedTextColor.YELLOW)
                .decoration(TextDecoration.BOLD, true));
        warden.getWorld().playSound(warden.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 0.5f);
        
        // Schedule shield regeneration
        if (shieldRegenTask != null) shieldRegenTask.cancel();
        shieldRegenTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!warden.isValid() || warden.isDead()) {
                    cancel();
                    return;
                }
                regenerateShield();
            }
        };
        shieldRegenTask.runTaskLater(plugin, SHIELD_DISABLE_DURATION / 50); // Convert ms to ticks
    }

    private void regenerateShield() {
        if (!warden.isValid() || warden.isDead()) return;
        
        shieldActive = true;
        warden.setMetadata("hollowrift_shield_active", new FixedMetadataValue(plugin, true));
        shieldNodes.clear();
        spawnShieldNodes(warden.getLocation());
        
        Bukkit.broadcast(Component.text("🛡 The Hollow Warden's Sonic Shield has regenerated!", NamedTextColor.RED)
                .decoration(TextDecoration.BOLD, true));
        warden.getWorld().playSound(warden.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 1.5f);
    }

    public boolean isShieldActive() {
        if (System.currentTimeMillis() >= shieldDisabledUntil) {
            shieldActive = true;
        }
        return shieldActive;
    }

    public void onBossDamaged(EntityDamageByEntityEvent event) {
        if (!isShieldActive()) return;
        
        // Only reduce melee damage
        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK ||
            event.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
            event.setDamage(event.getDamage() * 0.1); // 90% reduction
            warden.getWorld().spawnParticle(Particle.ENCHANTED_HIT, warden.getLocation().add(0, 1.5, 0), 10, 0.5, 0.5, 0.5, 0.2);
        }
    }

    // ==================== Vibration Slam (below 50% HP) ====================

    private void triggerVibrationSlam() {
        Location center = warden.getLocation();
        
        // Warning phase
        warden.getWorld().playSound(center, Sound.BLOCK_ANVIL_LAND, 2.0f, 0.5f);
        
        // Particle ring warning
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = VIBRATION_SLAM_WARNING / 50; // 60 ticks = 3 seconds
            
            @Override
            public void run() {
                if (!warden.isValid() || warden.isDead()) {
                    cancel();
                    return;
                }
                
                // Expanding ring particles
                double radius = (ticks / (double) maxTicks) * VIBRATION_SLAM_RADIUS;
                for (int i = 0; i < 36; i += 2) {
                    double angle = Math.toRadians(i);
                    Location particleLoc = center.clone().add(
                            Math.cos(angle) * radius, 0.1, Math.sin(angle) * radius
                    );
                    particleLoc.getWorld().spawnParticle(Particle.SCULK_CHARGE_POP, particleLoc, 1);
                }
                
                if (ticks >= maxTicks) {
                    executeVibrationSlam(center);
                    cancel();
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void executeVibrationSlam(Location center) {
        // Sound
        center.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 0.8f);
        center.getWorld().spawnParticle(Particle.SONIC_BOOM, center, 1);
        center.getWorld().spawnParticle(Particle.SCULK_SOUL, center, 50, VIBRATION_SLAM_RADIUS, 2, VIBRATION_SLAM_RADIUS, 0.2);
        
        // Affect nearby grounded players
        for (Player player : center.getWorld().getPlayers()) {
            if (player.getLocation().distance(center) <= VIBRATION_SLAM_RADIUS && player.isOnGround()) {
                // Launch upward
                Vector velocity = player.getVelocity().setY(1.1);
                player.setVelocity(velocity);
                
                // Apply Darkness + Slowness II for 4 seconds (80 ticks)
                player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 80, 0, false, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 1, false, false, true));
                
                // sendTitle in 1.21.4 expects Strings
                player.sendTitle(
                        "VIBRATION SLAM",
                        "The ground shakes violently!",
                        5, 40, 10
                );
            }
        }
    }

    // ==================== Soul Tether (below 20% HP) ====================

    private void triggerSoulTether() {
        if (!warden.isValid() || warden.isDead()) return;
        
        // Find 2 nearest players within reasonable range
        List<Player> nearbyPlayers = new ArrayList<>();
        for (Player player : warden.getWorld().getPlayers()) {
            if (player.getLocation().distance(warden.getLocation()) <= 50) {
                nearbyPlayers.add(player);
            }
        }
        
        nearbyPlayers.sort(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(warden.getLocation())));
        
        tetheredPlayers.clear();
        for (int i = 0; i < Math.min(2, nearbyPlayers.size()); i++) {
            tetheredPlayers.add(nearbyPlayers.get(i).getUniqueId());
        }
        
        if (tetheredPlayers.isEmpty()) return;
        
        // Visual tether effect
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!warden.isValid() || warden.isDead() || tetheredPlayers.isEmpty()) {
                    cancel();
                    return;
                }
                
                for (UUID uuid : tetheredPlayers) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null || !player.isOnline() || !player.isValid()) continue;
                    
                    // Draw particle line
                    Location wardenLoc = warden.getLocation().add(0, 1.5, 0);
                    Location playerLoc = player.getLocation().add(0, 1, 0);
                    Vector direction = playerLoc.toVector().subtract(wardenLoc.toVector()).normalize();
                    double distance = wardenLoc.distance(playerLoc);
                    
                    for (double d = 0; d < distance; d += 0.5) {
                        Location particleLoc = wardenLoc.clone().add(direction.clone().multiply(d));
                        warden.getWorld().spawnParticle(Particle.SCULK_CHARGE_POP, particleLoc, 1);
                    }
                    
                    // Heal boss if player is within tether range
                    if (distance <= SOUL_TETHER_RADIUS) {
                        double newHealth = Math.min(MAX_HEALTH, warden.getHealth() + SOUL_TETHER_HEAL_PER_SEC / 20.0);
                        warden.setHealth(newHealth);
                        
                        // Visual heal indicator
                        warden.getWorld().spawnParticle(Particle.HEART, wardenLoc, 3, 0.3, 0.3, 0.3, 0.05);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ==================== Death Handling ====================

    public void onDeath() {
        // Drop Echoing Cores (1-2)
        int coreCount = 1 + ThreadLocalRandom.current().nextInt(2); // 1 or 2
        ItemManager itemManager = plugin.getItemManager();
        
        if (itemManager == null) {
            plugin.getLogger().severe("ItemManager is null! Cannot drop Echoing Cores.");
        } else {
            for (int i = 0; i < coreCount; i++) {
                Location dropLoc = warden.getLocation().add(
                        ThreadLocalRandom.current().nextDouble(-1, 1),
                        1,
                        ThreadLocalRandom.current().nextDouble(-1, 1)
                );
                ItemStack core = itemManager.createEchoingCore();
                if (core != null) {
                    warden.getWorld().dropItemNaturally(dropLoc, core);
                } else {
                    plugin.getLogger().warning("createEchoingCore() returned null!");
                }
            }
        }
        
        // Broadcast death message
        Bukkit.broadcast(Component.text("☠ The Hollow Warden has been defeated!", NamedTextColor.GREEN)
                .decoration(TextDecoration.BOLD, true));
        Bukkit.broadcast(Component.text(coreCount + " Echoing Core" + (coreCount > 1 ? "s" : "") + " dropped!", NamedTextColor.AQUA));
        
        // Cleanup
        cleanup();
        bossManager.onBossDeath(this);
    }

    private void cleanup() {
        // Cancel all tasks
        if (bossBarUpdateTask != null) bossBarUpdateTask.cancel();
        if (vibrationSlamTask != null) vibrationSlamTask.cancel();
        if (soulTetherTask != null) soulTetherTask.cancel();
        if (shieldRegenTask != null) shieldRegenTask.cancel();
        
        // Remove boss bar
        bossBar.removeAll();
        
        // Remove shield nodes
        for (Entity node : shieldNodes) {
            if (node.isValid()) node.remove();
        }
        shieldNodes.clear();
        
        // Clear tethered players
        tetheredPlayers.clear();
    }

    public Warden getWarden() {
        return warden;
    }

    public Location getSpawnLocation() {
        return spawnLocation.clone();
    }
}