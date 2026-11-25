package com.hollowrift;

import com.hollowrift.altar.AltarListener;
import com.hollowrift.altar.TrackerTask;
import com.hollowrift.boss.BossManager;
import com.hollowrift.combat.RiftbladeListener;
import com.hollowrift.commands.GetCommandHandler;
import com.hollowrift.items.ItemManager;
import com.hollowrift.items.RecipeListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class HollowRift extends JavaPlugin {

    private ItemManager itemManager;
    private RiftbladeListener riftbladeListener;
    private BossManager bossManager;
    private TrackerTask trackerTask;

    @Override
    public void onEnable() {
        // Initialize ItemManager first (needed by other components)
        this.itemManager = new ItemManager(this);

        // Initialize BossManager
        this.bossManager = new BossManager(this);

        // Register listeners
        getServer().getPluginManager().registerEvents(new RecipeListener(this), this);
        this.riftbladeListener = new RiftbladeListener(this);
        getServer().getPluginManager().registerEvents(riftbladeListener, this);
        getServer().getPluginManager().registerEvents(bossManager, this);

        // Register Step 4 listeners
        getServer().getPluginManager().registerEvents(new AltarListener(this), this);

        // Start tracker task for Riftblade ping
        this.trackerTask = new TrackerTask(this);
        trackerTask.start();

        // Register commands
        GetCommandHandler commandHandler = new GetCommandHandler(this);
        getCommand("hollowrift").setExecutor(commandHandler);
        getCommand("hollowrift").setTabCompleter(commandHandler);

        getLogger().info("HollowRift has been enabled!");
    }

    @Override
    public void onDisable() {
        if (riftbladeListener != null) {
            riftbladeListener.cleanup();
        }
        if (trackerTask != null) {
            trackerTask.cancel();
        }
        getLogger().info("HollowRift has been disabled!");
    }

    public ItemManager getItemManager() {
        return itemManager;
    }

    public BossManager getBossManager() {
        return bossManager;
    }
}