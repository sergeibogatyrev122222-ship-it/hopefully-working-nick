package com.nickplugin;

import org.bukkit.plugin.java.JavaPlugin;

public class NickPlugin extends JavaPlugin {

    private NickManager nickManager;

    @Override
    public void onEnable() {
        nickManager = new NickManager(this);

        NickCommand nickCommand = new NickCommand(this, nickManager);
        getCommand("nick").setExecutor(nickCommand);
        getCommand("nick").setTabCompleter(nickCommand);

        getServer().getPluginManager().registerEvents(new NickListener(nickManager), this);

        getLogger().info("NickPlugin enabled!");
    }

    @Override
    public void onDisable() {
        if (nickManager != null) {
            nickManager.restoreAll();
        }
        getLogger().info("NickPlugin disabled!");
    }

    public NickManager getNickManager() {
        return nickManager;
    }
}
