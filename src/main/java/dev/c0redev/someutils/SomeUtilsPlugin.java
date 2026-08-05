package dev.c0redev.someutils;

import dev.c0redev.someutils.armor.ArmorHudManager;
import dev.c0redev.someutils.command.SomeUtilsCommand;
import dev.c0redev.someutils.config.PluginConfig;
import dev.c0redev.someutils.invtweaks.SortContainerGui;
import dev.c0redev.someutils.invtweaks.RefillListener;
import dev.c0redev.someutils.jade.DamageIndicatorManager;
import dev.c0redev.someutils.jade.JadeManager;
import dev.c0redev.someutils.lang.LanguageService;
import dev.c0redev.someutils.pack.ResourcePackServer;
import dev.c0redev.someutils.gui.SettingsMenu;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class SomeUtilsPlugin extends JavaPlugin {

    private static SomeUtilsPlugin instance;
    private PluginConfig pluginConfig;
    private JadeManager jadeManager;
    private ArmorHudManager armorHudManager;
    private ResourcePackServer packServer;
    private SortContainerGui sortContainerGui;
    private LanguageService languageService;
    private SettingsMenu settingsMenu;
    private DamageIndicatorManager damageIndicatorManager;
    private boolean packetEventsPresent;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        pluginConfig = new PluginConfig(this);
        pluginConfig.reload();
        languageService = new LanguageService(this);

        packetEventsPresent = Bukkit.getPluginManager().getPlugin("packetevents") != null;

        getServer().getPluginManager().registerEvents(new RefillListener(this), this);
        sortContainerGui = new SortContainerGui(this);
        getServer().getPluginManager().registerEvents(sortContainerGui, this);
        if (packetEventsPresent) {
            sortContainerGui.registerPacketVisuals();
        }

        jadeManager = new JadeManager(this);
        getServer().getPluginManager().registerEvents(jadeManager, this);
        if (pluginConfig.isJadeEnabled()) {
            jadeManager.start();
        }

        armorHudManager = new ArmorHudManager(this);
        getServer().getPluginManager().registerEvents(armorHudManager, this);
        if (pluginConfig.isArmorHudEnabled()) {
            armorHudManager.start();
        }

        damageIndicatorManager = new DamageIndicatorManager(this);
        getServer().getPluginManager().registerEvents(damageIndicatorManager, this);

        packServer = new ResourcePackServer(this);
        if (pluginConfig.isResourcePackEnabled()) {
            packServer.start();
        }

        settingsMenu = new SettingsMenu(this);
        getServer().getPluginManager().registerEvents(settingsMenu, this);

        SomeUtilsCommand command = new SomeUtilsCommand(this);
        var cmd = getCommand("someutils");
        if (cmd != null) {
            cmd.setExecutor(command);
            cmd.setTabCompleter(command);
        }

        if (!packetEventsPresent) {
            getLogger().warning("packetevents absent — Jade HUD and packet inventory visuals disabled");
        }
        getLogger().info("SomeUtils enabled for 1.21.11");
    }

    @Override
    public void onDisable() {
        if (jadeManager != null) {
            jadeManager.stop();
        }
        if (armorHudManager != null) {
            armorHudManager.stop();
        }
        if (damageIndicatorManager != null) {
            damageIndicatorManager.shutdown();
        }
        if (packServer != null) {
            packServer.stop();
        }
        if (sortContainerGui != null) {
            sortContainerGui.unregisterPacketVisuals();
        }
    }

    public void reloadAll() {
        reloadConfig();
        pluginConfig.reload();
        if (languageService != null) {
            languageService.reload();
        }

        if (jadeManager != null) {
            jadeManager.stop();
            if (pluginConfig.isJadeEnabled()) {
                jadeManager.start();
            }
        }

        if (armorHudManager != null) {
            armorHudManager.stop();
            if (pluginConfig.isArmorHudEnabled()) {
                armorHudManager.start();
            }
        }

        if (packServer != null) {
            packServer.stop();
            if (pluginConfig.isResourcePackEnabled()) {
                packServer.start();
            }
        }
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public JadeManager getJadeManager() {
        return jadeManager;
    }

    public ArmorHudManager getArmorHudManager() {
        return armorHudManager;
    }

    public ResourcePackServer getPackServer() {
        return packServer;
    }

    public LanguageService getLanguageService() {
        return languageService;
    }

    public SettingsMenu getSettingsMenu() {
        return settingsMenu;
    }

    public boolean isPacketEventsPresent() {
        return packetEventsPresent;
    }

    public SortContainerGui getSortContainerGui() {
        return sortContainerGui;
    }

    public static SomeUtilsPlugin getInstance() {
        return instance;
    }
}
