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
        ItemStack broken = event.getBrokenItem();
        int slot = player.getInventory().getHeldItemSlot();
        Material type = broken.getType();
        Bukkit.getScheduler().runTask(plugin, () -> refill(player, slot, type));
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
        int slot = player.getInventory().getHeldItemSlot();
        Material type = item.getType();
        Bukkit.getScheduler().runTask(plugin, () -> refill(player, slot, type));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!plugin.getPluginConfig().isRefillEnabled()) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() != Material.AIR && hand.getAmount() > 0) {
            return;
        }
        Material type = event.getItemInHand().getType();
        int slot = player.getInventory().getHeldItemSlot();
        Bukkit.getScheduler().runTask(plugin, () -> refill(player, slot, type));
    }

    private void refill(Player player, int slot, Material type) {
        if (type == null || type.isAir()) {
            return;
        }
        PlayerInventory inv = player.getInventory();
        ItemStack current = inv.getItem(slot);
        if (current != null && !current.getType().isAir()) {
            return;
        }

        for (int i = 9; i < 36; i++) {
            ItemStack candidate = inv.getItem(i);
            if (candidate == null || candidate.getType() != type) {
                continue;
            }
            inv.setItem(slot, candidate.clone());
            inv.setItem(i, null);
            player.updateInventory();
            return;
        }
    }
}
