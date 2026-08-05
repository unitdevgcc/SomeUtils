package dev.c0redev.someutils.gui;

import dev.c0redev.someutils.SomeUtilsPlugin;
import dev.c0redev.someutils.lang.Language;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;


public final class SettingsMenu implements Listener {

    private static final int SIZE = 45;
    private static final int SLOT_LOGO = 4;
    private static final int SLOT_HEADER_LANG = 9;
    private static final int SLOT_LANG_AUTO = 11;
    private static final int SLOT_LANG_RU = 13;
    private static final int SLOT_LANG_EN = 15;
    private static final int SLOT_HEADER_FEATURES = 27;
    private static final int SLOT_JADE = 29;
    private static final int SLOT_ARMOR = 31;
    private static final int SLOT_ARMOR_ACCENT = 33;
    private static final int SLOT_ARMOR_PRIMARY = 34;
    private static final int SLOT_ARMOR_SECONDARY = 35;
    private static final int SLOT_ARMOR_COMPACT = 28;
    private static final int SLOT_ARMOR_PULSE = 30;
    private static final int SLOT_ARMOR_OFFHAND = 32;
    private static final int SLOT_JADE_COMPACT = 36;
    private static final int SLOT_JADE_ICONS = 37;
    private static final int SLOT_JADE_DETAILS = 38;
    private static final int SLOT_JADE_SPACING = 39;
    private static final int SLOT_JADE_OFFSET = 41;
    private static final int SLOT_CLOSE = 40;

    private final SomeUtilsPlugin plugin;

    public SettingsMenu(SomeUtilsPlugin plugin) { this.plugin = plugin; }

    public void open(Player player) {
        SettingsHolder holder = new SettingsHolder();
        Inventory inventory = Bukkit.createInventory(holder, SIZE,
                Component.text(plugin.getLanguageService().tr(player, "settings"), NamedTextColor.DARK_GREEN, TextDecoration.BOLD));
        holder.setInventory(inventory);

        ItemStack frame = pane(Material.GRAY_STAINED_GLASS_PANE, "");
        ItemStack divider = pane(Material.LIME_STAINED_GLASS_PANE, "divider");
        for (int slot = 0; slot < SIZE; slot++) inventory.setItem(slot, frame);
        for (int slot = 9; slot < 18; slot++) inventory.setItem(slot, divider);

        inventory.setItem(SLOT_LOGO, logo());

        Language selected = plugin.getLanguageService().getPreference(player);
        inventory.setItem(SLOT_LANG_AUTO, langOption(player, "lang_auto", Material.COMPASS, "language.auto",
                selected == Language.AUTO, plugin.getLanguageService().tr(player, "language.auto.lore")));
        inventory.setItem(SLOT_LANG_RU, langOption(player, "lang_ru", Material.RED_BANNER, "language.ru",
                selected == Language.RU, "Русский"));
        inventory.setItem(SLOT_LANG_EN, langOption(player, "lang_en", Material.BLUE_BANNER, "language.en",
                selected == Language.EN, "English"));

        boolean jadeEnabled = plugin.getJadeManager().isEnabledFor(player);
        inventory.setItem(SLOT_JADE, jadeToggle(player, jadeEnabled));
        boolean armorEnabled = plugin.getArmorHudManager().isEnabledFor(player);
        inventory.setItem(SLOT_ARMOR, armorToggle(player, armorEnabled));
        inventory.setItem(SLOT_ARMOR_ACCENT, borderColor(player, "accent"));
        inventory.setItem(SLOT_ARMOR_PRIMARY, borderColor(player, "primary"));
        inventory.setItem(SLOT_ARMOR_SECONDARY, borderColor(player, "secondary"));
        inventory.setItem(SLOT_ARMOR_COMPACT, armorOption("Compact", plugin.getPluginConfig().isArmorHudCompact()));
        inventory.setItem(SLOT_ARMOR_PULSE, armorOption("Critical cracks", plugin.getPluginConfig().getArmorHudPulseThreshold() > 0));
        inventory.setItem(SLOT_ARMOR_OFFHAND, armorOption("Offhand", plugin.getPluginConfig().isArmorHudShowOffhand()));
        inventory.setItem(SLOT_JADE_COMPACT, armorOption("Jade compact", plugin.getPluginConfig().isJadeCompact()));
        inventory.setItem(SLOT_JADE_ICONS, armorOption("Jade icons", plugin.getPluginConfig().isJadeShowIcons()));
        inventory.setItem(SLOT_JADE_DETAILS, armorOption("Jade details", plugin.getPluginConfig().isJadeShowDetails()));
        inventory.setItem(SLOT_JADE_SPACING, armorOption("Jade spacing", plugin.getPluginConfig().getJadeLineGapBars() > 0));
        inventory.setItem(SLOT_JADE_OFFSET, jadeOffset());

        inventory.setItem(SLOT_CLOSE, closeButton(player));

        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof SettingsHolder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        switch (event.getRawSlot()) {
        case SLOT_LANG_AUTO -> selectLanguage(player, Language.AUTO);
        case SLOT_LANG_RU -> selectLanguage(player, Language.RU);
        case SLOT_LANG_EN -> selectLanguage(player, Language.EN);
        case SLOT_JADE -> {
            plugin.getJadeManager().toggle(player);
            open(player);
        }
        case SLOT_ARMOR -> {
            plugin.getArmorHudManager().toggle(player);
            open(player);
        }
        case SLOT_ARMOR_ACCENT -> cycleBorder(player, "accent");
        case SLOT_ARMOR_PRIMARY -> cycleBorder(player, "primary");
        case SLOT_ARMOR_SECONDARY -> cycleBorder(player, "secondary");
        case SLOT_ARMOR_COMPACT -> toggleArmorConfig(player, "armor-hud.compact");
        case SLOT_ARMOR_PULSE -> togglePulse(player);
        case SLOT_ARMOR_OFFHAND -> toggleArmorConfig(player, "armor-hud.show-offhand");
        case SLOT_JADE_COMPACT -> toggleJadeConfig(player, "jade.compact");
        case SLOT_JADE_ICONS -> toggleJadeConfig(player, "jade.show-icons");
        case SLOT_JADE_DETAILS -> toggleJadeConfig(player, "jade.show-details");
        case SLOT_JADE_SPACING -> toggleJadeSpacing(player);
        case SLOT_JADE_OFFSET -> cycleJadeOffset(player);
        case SLOT_CLOSE -> player.closeInventory();
        default -> {
        }
        }
    }

    private void selectLanguage(Player player, Language language) {
        plugin.getLanguageService().set(player, language);
        plugin.getJadeManager().refresh(player);
        player.sendMessage(Component.text(plugin.getLanguageService().tr(player, "settings.saved"), NamedTextColor.GREEN));
        open(player);
    }

    private ItemStack langOption(Player player, String model, Material fallback, String labelKey, boolean selected, String subLabel) {
        Component name = Component.text(plugin.getLanguageService().tr(player, labelKey),
                selected ? NamedTextColor.GREEN : NamedTextColor.WHITE, TextDecoration.BOLD);
        List<Component> lore = List.of(
                Component.text(subLabel, NamedTextColor.GRAY),
                Component.empty(),
                selected
                        ? Component.text("● " + plugin.getLanguageService().tr(player, "language.active"), NamedTextColor.GREEN)
                        : Component.text("○ " + plugin.getLanguageService().tr(player, "language.click"), NamedTextColor.DARK_GRAY)
        );
        ItemStack item = customItem(fallback, model, name, lore);
        if (selected) {
            ItemMeta meta = item.getItemMeta();
            meta.setEnchantmentGlintOverride(true);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack jadeToggle(Player player, boolean enabled) {
        Component name = Component.text(plugin.getLanguageService().tr(player, "jade"),
                enabled ? NamedTextColor.GREEN : NamedTextColor.GRAY, TextDecoration.BOLD);
        List<Component> lore = List.of(
                Component.text(plugin.getLanguageService().tr(player, enabled ? "jade.lore.on" : "jade.lore.off"), NamedTextColor.GRAY),
                Component.empty(),
                Component.text((enabled ? "● " : "○ ") + plugin.getLanguageService().tr(player, enabled ? "jade.on" : "jade.off"),
                        enabled ? NamedTextColor.GREEN : NamedTextColor.RED),
                Component.text(plugin.getLanguageService().tr(player, "jade.click"), NamedTextColor.DARK_GRAY)
        );
        ItemStack item = customItem(enabled ? Material.ENDER_EYE : Material.ENDER_PEARL, enabled ? "jade_on" : "jade_off", name, lore);
        if (enabled) {
            ItemMeta meta = item.getItemMeta();
            meta.setEnchantmentGlintOverride(true);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack armorToggle(Player player, boolean enabled) {
        Component name = Component.text(plugin.getLanguageService().tr(player, "armor"),
                enabled ? NamedTextColor.GREEN : NamedTextColor.GRAY, TextDecoration.BOLD);
        List<Component> lore = List.of(
                Component.text(plugin.getLanguageService().tr(player, enabled ? "armor.lore.on" : "armor.lore.off"), NamedTextColor.GRAY),
                Component.empty(),
                Component.text((enabled ? "● " : "○ ") + plugin.getLanguageService().tr(player, enabled ? "armor.on" : "armor.off"),
                        enabled ? NamedTextColor.GREEN : NamedTextColor.RED),
                Component.text(plugin.getLanguageService().tr(player, "armor.click"), NamedTextColor.DARK_GRAY)
        );
        ItemStack item = customItem(enabled ? Material.IRON_CHESTPLATE : Material.LEATHER_CHESTPLATE,
                enabled ? "armor_on" : "armor_off", name, lore);
        if (enabled) {
            ItemMeta meta = item.getItemMeta();
            meta.setEnchantmentGlintOverride(true);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack borderColor(Player player, String channel) {
        String key = "armor-hud.border." + channel;
        String color = plugin.getConfig().getString(key, "#84BB63");
        TextColor exact = exactColor(color);
        Component name = Component.text("HUD " + channel, exact, TextDecoration.BOLD);
        List<Component> lore = List.of(
                Component.text("■  " + color.toUpperCase(java.util.Locale.ROOT), exact),
                Component.text("RGB " + rgb(color), NamedTextColor.GRAY),
                Component.text("Click to cycle", NamedTextColor.DARK_GRAY)
        );
        Material material = switch (channel) {
            case "primary" -> Material.LIME_DYE;
            case "secondary" -> Material.GRAY_DYE;
            default -> Material.GREEN_DYE;
        };
        return customItem(material, "armor_border_" + channel, name, lore);
    }

    private static TextColor exactColor(String value) {
        try {
            return TextColor.fromHexString(value);
        } catch (RuntimeException ignored) {
            return NamedTextColor.WHITE;
        }
    }

    private static String rgb(String value) {
        try {
            int rgb = Integer.parseInt(value.substring(1), 16);
            return (rgb >> 16 & 255) + ", " + (rgb >> 8 & 255) + ", " + (rgb & 255);
        } catch (RuntimeException ignored) {
            return "invalid";
        }
    }

    private void cycleBorder(Player player, String channel) {
        String key = "armor-hud.border." + channel;
        String current = plugin.getConfig().getString(key, "#84BB63");
        String[] palette = {"#84BB63", "#62D7FF", "#D68CFF", "#FFB84D", "#FF5D73", "#E8F1FF"};
        int index = 0;
        for (int i = 0; i < palette.length; i++) {
            if (palette[i].equalsIgnoreCase(current)) {
                index = (i + 1) % palette.length;
                break;
            }
        }
        plugin.getConfig().set(key, palette[index]);
        plugin.saveConfig();
        plugin.reloadAll();
        open(player);
    }

    private ItemStack armorOption(String label, boolean enabled) {
        return customItem(enabled ? Material.LIME_DYE : Material.GRAY_DYE, "armor_border_primary",
                Component.text(label, enabled ? NamedTextColor.GREEN : NamedTextColor.GRAY, TextDecoration.BOLD),
                List.of(Component.text(enabled ? "ON" : "OFF", enabled ? NamedTextColor.GREEN : NamedTextColor.RED),
                        Component.text("Click to toggle", NamedTextColor.DARK_GRAY)));
    }

    private void toggleArmorConfig(Player player, String path) {
        plugin.getConfig().set(path, !plugin.getConfig().getBoolean(path, true));
        plugin.saveConfig();
        plugin.reloadAll();
        open(player);
    }

    private void togglePulse(Player player) {
        int current = plugin.getPluginConfig().getArmorHudPulseThreshold();
        plugin.getConfig().set("armor-hud.pulse-threshold", current > 0 ? 0 : 20);
        plugin.saveConfig();
        plugin.reloadAll();
        open(player);
    }

    private void toggleJadeConfig(Player player, String path) {
        plugin.getConfig().set(path, !plugin.getConfig().getBoolean(path, true));
        plugin.saveConfig();
        reloadJade();
        open(player);
    }

    private void toggleJadeSpacing(Player player) {
        plugin.getConfig().set("jade.line-gap-bars", plugin.getPluginConfig().getJadeLineGapBars() > 0 ? 0 : 1);
        plugin.saveConfig();
        reloadJade();
        open(player);
    }

    private ItemStack jadeOffset() {
        int value = plugin.getPluginConfig().getJadeVerticalOffsetBars();
        return customItem(Material.COMPASS, "jade_on",
                Component.text("Jade vertical: " + value, NamedTextColor.AQUA, TextDecoration.BOLD),
                List.of(Component.text("Click: 0-4", NamedTextColor.DARK_GRAY)));
    }

    private void cycleJadeOffset(Player player) {
        int value = (plugin.getPluginConfig().getJadeVerticalOffsetBars() + 1) % 5;
        plugin.getConfig().set("jade.vertical-offset-bars", value);
        plugin.saveConfig();
        reloadJade();
        open(player);
    }

    private void reloadJade() {
        plugin.getPluginConfig().reload();
        plugin.getJadeManager().start();
    }

    private ItemStack closeButton(Player player) {
        return customItem(Material.BARRIER, "close",
                Component.text(plugin.getLanguageService().tr(player, "settings.close"), NamedTextColor.RED, TextDecoration.BOLD),
                List.of());
    }

    private ItemStack logo() {
        ItemStack item = customItem(Material.NETHER_STAR, "logo",
                Component.text("SomeUtils", NamedTextColor.DARK_GREEN, TextDecoration.BOLD), List.of());
        ItemMeta meta = item.getItemMeta();
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        return item;
    }

    private static final ItemFlag[] HIDE_FLAGS = {
        ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE,
        ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_DYE,
        ItemFlag.HIDE_ARMOR_TRIM, ItemFlag.HIDE_STORED_ENCHANTS,
    };

    private ItemStack pane(Material type, String model) {
        ItemStack item = ItemStack.of(type);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.empty());
        meta.addItemFlags(HIDE_FLAGS);
        if (!model.isEmpty()) {
            meta.setItemModel(new NamespacedKey("someutils", "gui/" + model));
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack customItem(Material fallback, String model, Component name, List<Component> lore) {
        ItemStack item = ItemStack.of(fallback);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        meta.lore(lore.stream().map(line -> line.decoration(TextDecoration.ITALIC, false)).toList());
        meta.addItemFlags(HIDE_FLAGS);
        meta.setItemModel(new NamespacedKey("someutils", "gui/" + model));
        item.setItemMeta(meta);
        return item;
    }
}
