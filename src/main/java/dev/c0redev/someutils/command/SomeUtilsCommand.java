package dev.c0redev.someutils.command;

import dev.c0redev.someutils.SomeUtilsPlugin;
import dev.c0redev.someutils.invtweaks.InventorySorter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class SomeUtilsCommand implements CommandExecutor, TabCompleter {

    private final SomeUtilsPlugin plugin;

    public SomeUtilsCommand(SomeUtilsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                plugin.getSettingsMenu().open(player);
            } else {
                sendHelp(sender);
            }
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help" -> sendHelp(sender);
            case "sort" -> handleSort(sender, args);
            case "jade" -> handleJade(sender);
            case "armor" -> handleArmor(sender);
            case "refill" -> handleRefill(sender);
            case "reload" -> handleReload(sender);
            case "pack" -> handlePack(sender);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void handleSort(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players", NamedTextColor.RED));
            return;
        }
        if (!sender.hasPermission("someutils.use")) {
            sender.sendMessage(Component.text("No permission", NamedTextColor.RED));
            return;
        }
        if (!plugin.getPluginConfig().isInvTweaksEnabled()) {
            sender.sendMessage(Component.text("InvTweaks disabled", NamedTextColor.RED));
            return;
        }

        InventorySorter.SortMode mode = parseMode(args, 1);
        if (!InventorySorter.sortOpen(
                player,
                mode,
                plugin.getPluginConfig().isSortPlayerInventory(),
                plugin.getPluginConfig().isSortHotbar()
        )) {
            player.sendMessage(Component.text("Unsupported container", NamedTextColor.RED));
            return;
        }
        player.sendMessage(Component.text("Sorted (" + mode.name().toLowerCase(Locale.ROOT) + ")", NamedTextColor.GREEN));
    }

    private static InventorySorter.SortMode parseMode(String[] args, int index) {
        if (args.length <= index) {
            return InventorySorter.SortMode.DEFAULT;
        }
        return switch (args[index].toLowerCase(Locale.ROOT)) {
            case "columns", "col" -> InventorySorter.SortMode.COLUMNS;
            case "stack", "stackonly" -> InventorySorter.SortMode.STACK_ONLY;
            default -> InventorySorter.SortMode.DEFAULT;
        };
    }

    private void handleJade(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players", NamedTextColor.RED));
            return;
        }
        boolean enabled = plugin.getJadeManager().toggle(player);
        player.sendMessage(Component.text(plugin.getLanguageService().tr(player, enabled ? "jade.on" : "jade.off"), NamedTextColor.YELLOW));
    }

    private void handleArmor(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players", NamedTextColor.RED));
            return;
        }
        boolean enabled = plugin.getArmorHudManager().toggle(player);
        player.sendMessage(Component.text(plugin.getLanguageService().tr(player, enabled ? "armor.on" : "armor.off"), NamedTextColor.YELLOW));
    }

    private void handleRefill(CommandSender sender) {
        if (!sender.hasPermission("someutils.admin")) {
            sender.sendMessage(Component.text("No permission", NamedTextColor.RED));
            return;
        }
        boolean current = plugin.getPluginConfig().isRefillEnabled();
        plugin.getConfig().set("invtweaks.refill-enabled", !current);
        plugin.saveConfig();
        plugin.getPluginConfig().reload();
        sender.sendMessage(Component.text("Refill: " + (!current ? "on" : "off"), NamedTextColor.YELLOW));
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("someutils.admin")) {
            sender.sendMessage(Component.text("No permission", NamedTextColor.RED));
            return;
        }
        plugin.reloadAll();
        sender.sendMessage(Component.text("SomeUtils reloaded", NamedTextColor.GREEN));
    }

    private void handlePack(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players", NamedTextColor.RED));
            return;
        }
        if (plugin.getPackServer() == null || !plugin.getPackServer().isReady()) {
            player.sendMessage(Component.text("Resource pack is not ready", NamedTextColor.RED));
            return;
        }
        plugin.getPackServer().sendPack(player);
        player.sendMessage(Component.text("Resource pack request sent", NamedTextColor.GREEN));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("SomeUtils commands:", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("/su sort [default|columns|stack]", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/su jade", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/su armor", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/su refill", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/su pack", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/su reload", NamedTextColor.GRAY));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("sort", "jade", "armor", "refill", "reload", "pack", "help"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("sort")) {
            return filter(Arrays.asList("default", "columns", "stack"), args[1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> values, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String value : values) {
            if (value.startsWith(p)) {
                out.add(value);
            }
        }
        return out;
    }
}
