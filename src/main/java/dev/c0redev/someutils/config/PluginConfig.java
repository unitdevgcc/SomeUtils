package dev.c0redev.someutils.config;

import dev.c0redev.someutils.SomeUtilsPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public final class PluginConfig {

    private final SomeUtilsPlugin plugin;

    private boolean invTweaksEnabled;
    private boolean guiControls;
    private boolean refillEnabled;
    private boolean sortPlayerInventory;
    private boolean sortHotbar;
    private boolean evenPageMode;

    private boolean jadeEnabled;
    private int jadeIntervalTicks;
    private double jadeRange;
    private int jadeVerticalOffsetBars;
    private int jadeLineGapBars;
    private boolean showBlocks;
    private boolean showEntities;
    private boolean showHealth;
    private boolean showTool;
    private boolean showGrowth;
    private boolean showContainer;
    private boolean showRedstone;
    private boolean showFurnace;
    private boolean showBeehive;
    private boolean showVillager;
    private boolean showHorse;
    private boolean showPotions;

    private boolean damageIndicatorEnabled;
    private boolean damageIndicatorShowPlayers;
    private boolean damageIndicatorShowMobs;
    private int damageIndicatorDurationTicks;
    private double damageIndicatorRiseHeight;
    private double damageIndicatorCritScale;
    private String damageIndicatorNormalColor;
    private String damageIndicatorCritColor;

    private boolean resourcePackEnabled;
    private boolean vanillaTextShader;
    private String resourcePackBindAddress;
    private int resourcePackPort;
    private String resourcePackUrl;
    private boolean resourcePackForce;
    private String resourcePackPrompt;

    public PluginConfig(SomeUtilsPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        FileConfiguration cfg = plugin.getConfig();

        invTweaksEnabled = cfg.getBoolean("invtweaks.enabled", true);
        guiControls = cfg.getBoolean("invtweaks.gui-controls", true);
        refillEnabled = cfg.getBoolean("invtweaks.refill-enabled", true);
        sortPlayerInventory = cfg.getBoolean("invtweaks.sort-player-inventory", true);
        sortHotbar = cfg.getBoolean("invtweaks.sort-hotbar", false);
        evenPageMode = !"full".equalsIgnoreCase(cfg.getString("invtweaks.page-mode", "even"));

        jadeEnabled = cfg.getBoolean("jade.enabled", true);
        jadeIntervalTicks = Math.max(1, cfg.getInt("jade.interval-ticks", 2));
        jadeRange = Math.max(1.0, cfg.getDouble("jade.range", 6.0));
        jadeVerticalOffsetBars = Math.max(0, Math.min(4, cfg.getInt("jade.vertical-offset-bars", 1)));
        jadeLineGapBars = Math.max(0, Math.min(2, cfg.getInt("jade.line-gap-bars", 0)));
        showBlocks = cfg.getBoolean("jade.show-blocks", true);
        showEntities = cfg.getBoolean("jade.show-entities", true);
        showHealth = cfg.getBoolean("jade.show-health", true);
        showTool = cfg.getBoolean("jade.show-tool", true);
        showGrowth = cfg.getBoolean("jade.show-growth", true);
        showContainer = cfg.getBoolean("jade.show-container", true);
        showRedstone = cfg.getBoolean("jade.show-redstone", true);
        showFurnace = cfg.getBoolean("jade.show-furnace", true);
        showBeehive = cfg.getBoolean("jade.show-beehive", true);
        showVillager = cfg.getBoolean("jade.show-villager", true);
        showHorse = cfg.getBoolean("jade.show-horse", true);
        showPotions = cfg.getBoolean("jade.show-potions", true);

        damageIndicatorEnabled = cfg.getBoolean("damage-indicator.enabled", true);
        damageIndicatorShowPlayers = cfg.getBoolean("damage-indicator.show-for-players", true);
        damageIndicatorShowMobs = cfg.getBoolean("damage-indicator.show-for-mobs", true);
        damageIndicatorDurationTicks = Math.max(5, cfg.getInt("damage-indicator.duration-ticks", 20));
        damageIndicatorRiseHeight = Math.max(0.1, cfg.getDouble("damage-indicator.rise-height", 1.2));
        damageIndicatorCritScale = Math.max(1.0, cfg.getDouble("damage-indicator.crit-scale", 1.5));
        damageIndicatorNormalColor = cfg.getString("damage-indicator.normal-color", "#ffe14d");
        damageIndicatorCritColor = cfg.getString("damage-indicator.crit-color", "#ff4d4d");

        resourcePackEnabled = cfg.getBoolean("resource-pack.enabled", true);
        vanillaTextShader = cfg.getBoolean("resource-pack.vanilla-text-shader", true);
        resourcePackBindAddress = cfg.getString("resource-pack.bind-address", "0.0.0.0");
        resourcePackPort = cfg.getInt("resource-pack.http-port", 8089);
        resourcePackUrl = cfg.getString("resource-pack.public-url", "");
        resourcePackForce = cfg.getBoolean("resource-pack.force", false);
        resourcePackPrompt = cfg.getString("resource-pack.prompt", "SomeUtils resource pack");
    }

    public boolean isInvTweaksEnabled() { return invTweaksEnabled; }
    public boolean isGuiControls() { return guiControls; }
    public boolean isRefillEnabled() { return refillEnabled; }
    public boolean isSortPlayerInventory() { return sortPlayerInventory; }
    public boolean isSortHotbar() { return sortHotbar; }
    public boolean isEvenPageMode() { return evenPageMode; }

    public boolean isJadeEnabled() { return jadeEnabled; }
    public int getJadeIntervalTicks() { return jadeIntervalTicks; }
    public double getJadeRange() { return jadeRange; }
    public int getJadeVerticalOffsetBars() { return jadeVerticalOffsetBars; }
    public int getJadeLineGapBars() { return jadeLineGapBars; }
    public boolean isShowBlocks() { return showBlocks; }
    public boolean isShowEntities() { return showEntities; }
    public boolean isShowHealth() { return showHealth; }
    public boolean isShowTool() { return showTool; }
    public boolean isShowGrowth() { return showGrowth; }
    public boolean isShowContainer() { return showContainer; }
    public boolean isShowRedstone() { return showRedstone; }
    public boolean isShowFurnace() { return showFurnace; }
    public boolean isShowBeehive() { return showBeehive; }
    public boolean isShowVillager() { return showVillager; }
    public boolean isShowHorse() { return showHorse; }
    public boolean isShowPotions() { return showPotions; }

    public boolean isDamageIndicatorEnabled() { return damageIndicatorEnabled; }
    public boolean isDamageIndicatorShowPlayers() { return damageIndicatorShowPlayers; }
    public boolean isDamageIndicatorShowMobs() { return damageIndicatorShowMobs; }
    public int getDamageIndicatorDurationTicks() { return damageIndicatorDurationTicks; }
    public double getDamageIndicatorRiseHeight() { return damageIndicatorRiseHeight; }
    public double getDamageIndicatorCritScale() { return damageIndicatorCritScale; }
    public String getDamageIndicatorNormalColor() { return damageIndicatorNormalColor; }
    public String getDamageIndicatorCritColor() { return damageIndicatorCritColor; }

    public boolean isResourcePackEnabled() { return resourcePackEnabled; }
    public boolean isVanillaTextShader() { return vanillaTextShader; }
    public String getResourcePackBindAddress() { return resourcePackBindAddress; }
    public int getResourcePackPort() { return resourcePackPort; }
    public String getResourcePackUrl() { return resourcePackUrl; }
    public boolean isResourcePackForce() { return resourcePackForce; }
    public String getResourcePackPrompt() { return resourcePackPrompt; }
}
