package com.example.marriagegender;

import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.java.JavaPlugin;

public final class MarriageGenderPlugin extends JavaPlugin {

    private Messages messages;
    private DataManager dataManager;
    private EconomyManager economyManager;
    private RequestManager requestManager;

    @Override
    public void onEnable() {
        this.messages = new Messages();
        this.dataManager = new DataManager(this);
        this.dataManager.load();

        this.economyManager = new EconomyManager(this);
        if (!economyManager.setupEconomy()) {
            getLogger().severe("Vault or an economy plugin was not found. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.requestManager = new RequestManager();

        CommandManager commandManager = new CommandManager(this);
        registerCommand("gender", commandManager);
        registerCommand("sex", commandManager);
        registerCommand("marry", commandManager);
        registerCommand("divorce", commandManager);

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getScheduler().runTaskTimer(this, requestManager::cleanup, 20L, 20L);
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.save();
        }
    }

    private void registerCommand(String name, TabExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        } else {
            getLogger().warning("Command not found in plugin.yml: " + name);
        }
    }

    public Messages getMessages() {
        return messages;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public RequestManager getRequestManager() {
        return requestManager;
    }
}
