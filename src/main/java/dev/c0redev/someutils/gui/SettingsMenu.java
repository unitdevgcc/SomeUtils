package dev.c0redev.someutils.gui;

import dev.c0redev.someutils.SomeUtilsPlugin;
import dev.c0redev.someutils.lang.Language;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
    private static final int SLOT_CLOSE = 40;

    private static final int[] PANEL_SLOTS = {10, 12, 14, 16, 28, 30};

    private final SomeUtilsPlugin plugin;

    public SettingsMenu(SomeUtilsPlugin plugin) { this.plugin = plugin; }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(new SettingsHolder(), SIZE,
                Component.text(plugin.getLanguageService().tr(player, "settings"), NamedTextColor.DARK_GREEN, TextDecoration.BOLD));

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
