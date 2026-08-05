package dev.c0redev.someutils.invtweaks;

import dev.c0redev.someutils.SomeUtilsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class RefillListener implements Listener {

    private final SomeUtilsPlugin plugin;

    public RefillListener(SomeUtilsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(PlayerItemBreakEvent event) {
        if (!plugin.getPluginConfig().isRefillEnabled()) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack broken = event.getBrokenItem().clone();
        int slot = resolveBrokenSlot(player, broken);
        Bukkit.getScheduler().runTask(plugin, () -> refill(player, slot, broken));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (!plugin.getPluginConfig().isRefillEnabled()) {
            return;
        }
        ItemStack item = event.getItem();
        if (item.getAmount() > 1) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack template = item.clone();
        int slot = resolveHandSlot(player, event.getHand());
        Bukkit.getScheduler().runTask(plugin, () -> refill(player, slot, template));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!plugin.getPluginConfig().isRefillEnabled()) {
            return;
        }
        Player player = event.getPlayer();
        EquipmentSlot hand = event.getHand();
        ItemStack remaining = hand == EquipmentSlot.OFF_HAND
                ? player.getInventory().getItemInOffHand()
                : player.getInventory().getItemInMainHand();
        if (remaining.getType() != Material.AIR && remaining.getAmount() > 0) {
            return;
        }
        ItemStack template = event.getItemInHand().clone();
        int slot = resolveHandSlot(player, hand);
        Bukkit.getScheduler().runTask(plugin, () -> refill(player, slot, template));
    }

    private void refill(Player player, int slot, ItemStack template) {
        if (slot < 0 || template == null || template.getType().isAir()) {
            return;
        }
        PlayerInventory inv = player.getInventory();
        ItemStack current = slot == 40 ? inv.getItemInOffHand() : inv.getItem(slot);
        if (current != null && !current.getType().isAir()) {
            return;
        }
        for (int i = 9; i < 36; i++) {
            ItemStack candidate = inv.getItem(i);
            if (candidate == null || candidate.getType().isAir() || !matchesForRefill(template, candidate)) {
                continue;
            }
            ItemStack moved = candidate.clone();
            inv.setItem(i, null);
            if (slot == 40) {
                inv.setItemInOffHand(moved);
            } else {
                inv.setItem(slot, moved);
            }
            player.updateInventory();
            return;
        }
    }

    private static int resolveHandSlot(Player player, EquipmentSlot hand) {
        if (hand == EquipmentSlot.OFF_HAND) {
            return 40;
        }
        return player.getInventory().getHeldItemSlot();
    }

    private static int resolveBrokenSlot(Player player, ItemStack broken) {
        PlayerInventory inv = player.getInventory();
        ItemStack main = inv.getItemInMainHand();
        ItemStack off = inv.getItemInOffHand();
        boolean mainMatches = main != null && !main.getType().isAir() && matchesForRefill(broken, main);
        boolean offMatches = off != null && !off.getType().isAir() && matchesForRefill(broken, off);
        if (mainMatches != offMatches) {
            return mainMatches ? inv.getHeldItemSlot() : 40;
        }
        boolean mainEmpty = main == null || main.getType().isAir();
        boolean offEmpty = off == null || off.getType().isAir();
        if (mainEmpty != offEmpty) {
            return mainEmpty ? inv.getHeldItemSlot() : 40;
        }
        if (mainEmpty && offEmpty) {
            return -1;
        }
        // событие не сообщает EquipmentSlot, при неоднозначности не трогаем инвентарь
        return -1;
    }

    static boolean matchesForRefill(ItemStack expected, ItemStack candidate) {
        ItemStack normalizedExpected = withoutDamage(expected);
        ItemStack normalizedCandidate = withoutDamage(candidate);
        return normalizedExpected.isSimilar(normalizedCandidate);
    }

    private static ItemStack withoutDamage(ItemStack item) {
        ItemStack copy = item.clone();
        ItemMeta meta = copy.getItemMeta();
        if (meta instanceof Damageable damageable) {
            damageable.setDamage(0);
            copy.setItemMeta(meta);
        }
        return copy;
    }
}
