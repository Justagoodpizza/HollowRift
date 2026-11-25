package com.hollowrift.items;

import com.hollowrift.HollowRift;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;

public class RecipeListener implements Listener {

    private final HollowRift plugin;
    private final ItemManager itemManager;

    // Key for our custom recipe
    private static final NamespacedKey RIFTBLADE_RECIPE_KEY = new NamespacedKey("hollowrift", "riftblade_crafting");

    public RecipeListener(HollowRift plugin) {
        this.plugin = plugin;
        this.itemManager = plugin.getItemManager();
        registerCustomRecipes();
    }

    /**
     * Registers the custom Riftblade crafting recipe.
     * Recipe shape:
     *  E B E
     *  B S B
     *  E C E
     * Where:
     *  E = Echo Shard
     *  B = Breeze Rod
     *  S = Netherite Sword
     *  C = Echoing Core
     */
    private void registerCustomRecipes() {
        // Remove any existing recipe with this key to avoid duplicates on reload
        Bukkit.removeRecipe(RIFTBLADE_RECIPE_KEY);

        ShapedRecipe recipe = new ShapedRecipe(RIFTBLADE_RECIPE_KEY, itemManager.createRiftblade());

        // Shape:
        // E B E
        // B S B
        // E C E
        recipe.shape(
                "EBE",
                "BSB",
                "ECE"
        );

        // Ingredients
        recipe.setIngredient('E', Material.ECHO_SHARD);
        recipe.setIngredient('B', Material.BREEZE_ROD);
        recipe.setIngredient('S', Material.NETHERITE_SWORD);
        recipe.setIngredient('C', itemManager.createEchoingCore()); // Custom item as ingredient

        Bukkit.addRecipe(recipe);
    }

    /**
     * Prevents Echoing Core from being used in vanilla crafting recipes.
     * This event fires when a crafting table result is being computed.
     */
    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        Recipe recipe = event.getRecipe();
        if (recipe == null) return;

        // Check if the recipe is our custom one by comparing result item
        ItemStack result = recipe.getResult();
        if (result != null && itemManager.isRiftblade(result)) {
            return; // Allow our custom recipe
        }

        // Check if any ingredient in the crafting matrix is an Echoing Core
        ItemStack[] matrix = event.getInventory().getMatrix();
        for (ItemStack item : matrix) {
            if (itemManager.isEchoingCore(item)) {
                // Clear the result to prevent crafting
                event.getInventory().setResult(new ItemStack(Material.AIR));
                break;
            }
        }
    }
}