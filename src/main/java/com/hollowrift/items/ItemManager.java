package com.hollowrift.items;

import com.hollowrift.HollowRift;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class ItemManager {

    private final HollowRift plugin;

    // PDC Keys for identifying custom items
    public static final NamespacedKey ECHOING_CORE_KEY = new NamespacedKey("hollowrift", "echoing_core");
    public static final NamespacedKey RIFTBLADE_KEY = new NamespacedKey("hollowrift", "riftblade");

    public ItemManager(HollowRift plugin) {
        this.plugin = plugin;
    }

    /**
     * Creates the Echoing Core custom item.
     * A glowing Echo Shard with custom display name and lore.
     */
    public ItemStack createEchoingCore() {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // Display name: <aqua><bold>Echoing Core
            meta.displayName(Component.text("Echoing Core", NamedTextColor.AQUA)
                    .decoration(TextDecoration.BOLD, true));

            // Lore
            meta.lore(List.of(
                    Component.text("A resonant fragment pulsing", NamedTextColor.GRAY),
                    Component.text("with the energy of the void.", NamedTextColor.GRAY),
                    Component.empty(),
                    Component.text("Used to forge the Riftblade", NamedTextColor.DARK_AQUA)
                            .decoration(TextDecoration.ITALIC, true)
            ));

            // Make it glow (enchanted effect without actual enchants)
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            // Mark as custom item via PDC
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(ECHOING_CORE_KEY, PersistentDataType.BYTE, (byte) 1);

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Creates the Riftblade custom item.
     * A Netherite Sword with Sharpness V, custom name, and lore.
     */
    public ItemStack createRiftblade() {
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // Display name: <dark_aqua><bold>Riftblade
            meta.displayName(Component.text("Riftblade", NamedTextColor.DARK_AQUA)
                    .decoration(TextDecoration.BOLD, true));

            // Lore
            meta.lore(List.of(
                    Component.text("A blade forged from the echoes", NamedTextColor.GRAY),
                    Component.text("of the Deep Dark itself.", NamedTextColor.GRAY),
                    Component.empty(),
                    Component.text("Passive: Resonance Stacks", NamedTextColor.YELLOW)
                            .decoration(TextDecoration.BOLD, true),
                    Component.text("Every 3rd hit releases a sonic boom.", NamedTextColor.GRAY),
                    Component.empty(),
                    Component.text("Active: Phase Slash (Right-Click)", NamedTextColor.YELLOW)
                            .decoration(TextDecoration.BOLD, true),
                    Component.text("Dash forward, damaging enemies in path.", NamedTextColor.GRAY),
                    Component.text("Cooldown: 15 seconds", NamedTextColor.DARK_GRAY)
            ));

            // Sharpness V
            meta.addEnchant(Enchantment.SHARPNESS, 5, true);

            // Additional useful enchants for a boss weapon
            meta.addEnchant(Enchantment.UNBREAKING, 3, true);
            meta.addEnchant(Enchantment.MENDING, 1, true);

            // Mark as custom item via PDC
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(RIFTBLADE_KEY, PersistentDataType.BYTE, (byte) 1);

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Checks if an ItemStack is an Echoing Core.
     */
    public boolean isEchoingCore(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.has(ECHOING_CORE_KEY, PersistentDataType.BYTE);
    }

    /**
     * Checks if an ItemStack is a Riftblade.
     */
    public boolean isRiftblade(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.has(RIFTBLADE_KEY, PersistentDataType.BYTE);
    }

    /**
     * Gets the custom item by type name (for admin command).
     */
    public ItemStack getCustomItem(String type) {
        return switch (type.toLowerCase()) {
            case "core" -> createEchoingCore();
            case "blade" -> createRiftblade();
            default -> null;
        };
    }
}