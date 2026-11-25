package com.hollowrift.altar;

import com.hollowrift.HollowRift;
import com.hollowrift.boss.BossManager;
import com.hollowrift.items.ItemManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class AltarListener implements Listener {

    private final HollowRift plugin;
    private final BossManager bossManager;
    private final ItemManager itemManager;

    // Altar structure: SCULK_SHRIEKER at center, 4 CHISELED_DEEPSLATE at N,S,E,W 2 blocks away
    private static final Material SHRIEKER = Material.SCULK_SHRIEKER;
    private static final Material DEEPSLATE = Material.CHISELED_DEEPSLATE;
    private static final Material SOUL_LANTERN = Material.SOUL_LANTERN;
    private static final int ALTAR_RADIUS = 2;
    private static final BlockFace[] CARDINALS = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};

    public AltarListener(HollowRift plugin) {
        this.plugin = plugin;
        this.bossManager = plugin.getBossManager();
        this.itemManager = plugin.getItemManager();
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only main hand right-click on block
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();

        // Check if clicked block is a Sculk Shrieker
        if (clickedBlock.getType() != SHRIEKER) return;

        // Check if player is holding a Soul Lantern
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand.getType() != SOUL_LANTERN) return;

        // Verify altar structure
        if (!isValidAltar(clickedBlock)) {
            player.sendActionBar(Component.text("⚠ The altar is incomplete!", NamedTextColor.RED));
            clickedBlock.getWorld().playSound(clickedBlock.getLocation(), Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 0.5f, 0.5f);
            return;
        }

        // Valid altar! Consume Soul Lantern and spawn boss
        consumeSoulLantern(player);
        activateAltar(player, clickedBlock.getLocation());
    }

    private boolean isValidAltar(Block shrieker) {
        // Check 4 chiseled deepslate blocks at cardinal directions, 2 blocks away
        for (BlockFace face : CARDINALS) {
            Block relative = shrieker.getRelative(face, ALTAR_RADIUS);
            if (relative.getType() != DEEPSLATE) {
                return false;
            }
        }
        return true;
    }

    private void consumeSoulLantern(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        } else {
            mainHand.setAmount(mainHand.getAmount() - 1);
        }
    }

    private void activateAltar(Player player, Location shriekerLoc) {
        Location center = shriekerLoc.clone().add(0.5, 0, 0.5);
        World world = center.getWorld();

        // Visual effects
        world.strikeLightningEffect(center); // Cosmetic lightning
        world.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.5f);
        world.playSound(center, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 1.0f, 1.5f);

        // Particles
        for (int i = 0; i < 50; i++) {
            double angle = Math.random() * 2 * Math.PI;
            double radius = Math.random() * 3;
            Location particleLoc = center.clone().add(
                    Math.cos(angle) * radius,
                    Math.random() * 2,
                    Math.sin(angle) * radius
            );
            world.spawnParticle(Particle.SCULK_SOUL, particleLoc, 1);
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 1);
        }

        // Broadcast coordinates to server
        String coords = String.format("%d, %d, %d", 
                center.getBlockX(), center.getBlockY(), center.getBlockZ());
        Bukkit.broadcast(Component.text("⚠ ", NamedTextColor.RED)
                .append(Component.text("The Hollow Warden has been summoned at ", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD))
                .append(Component.text(coords, NamedTextColor.YELLOW))
                .append(Component.text(" in " + world.getName(), NamedTextColor.AQUA)));

        // Also send to player who activated
        player.sendMessage(Component.text("You have awakened ", NamedTextColor.GREEN)
                .append(Component.text("The Hollow Warden", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD))
                .append(Component.text("!", NamedTextColor.GREEN)));

        // Spawn the boss at the shrieker location (slightly above ground)
        Location spawnLoc = center.clone().add(0, 1, 0);
        
        // Small delay for dramatic effect
        new BukkitRunnable() {
            @Override
            public void run() {
                if (bossManager.hasActiveBoss()) {
                    Bukkit.broadcast(Component.text("A Hollow Warden is already active!", NamedTextColor.RED));
                    return;
                }
                bossManager.spawnBoss(spawnLoc);
            }
        }.runTaskLater(plugin, 20L); // 1 second delay
    }
}