package dev.c0redev.someutils.invtweaks;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Barrel;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;


final class SortGuiRenderer {

    static final int CONTROL_ROW = 9;

    private SortGuiRenderer() {
    }

    static Inventory open(Player player, SortSession session) {
        int rows = Math.min(6, 1 + (int) Math.ceil(session.capacity() / 9.0));
        Component title = containerTitle(session.source).color(NamedTextColor.DARK_GREEN);
        if (session.pages() > 1) {
            title = title.append(Component.text("  " + (session.page + 1) + "/" + session.pages(), NamedTextColor.GRAY));
        }
        Inventory gui = Bukkit.createInventory(new SortGuiHolder(session), rows * 9, title);
        drawControls(gui);
        int start = session.page * session.capacity();
        for (int i = 0; i < session.capacity() && start + i < session.source.getSize(); i++) {
            ItemStack item = clone(session.source.getItem(start + i));
            gui.setItem(CONTROL_ROW + i, item);
            session.updateSnapshot(start + i, item);
        }
        player.openInventory(gui);
        return gui;
    }


    static void savePage(Inventory gui, SortSession session) {
        int start = session.page * session.capacity();
        for (int i = 0; i < session.capacity() && start + i < session.source.getSize(); i++) {
            int slot = start + i;
            ItemStack inGui = clone(gui.getItem(CONTROL_ROW + i));
            if (itemEquals(inGui, session.snapshotAt(slot))) {
                continue;
            }
            session.source.setItem(slot, inGui);
            session.updateSnapshot(slot, inGui);
        }
    }


    static void syncSourceToGui(SortSession session, Inventory gui) {
        int start = session.page * session.capacity();
        for (int i = 0; i < session.capacity() && start + i < session.source.getSize(); i++) {
            int slot = start + i;
            if (session.isPending(slot)) {
                continue;
            }
            ItemStack src = clone(session.source.getItem(slot));
            ItemStack cur = gui.getItem(CONTROL_ROW + i);
            if (!itemEquals(src, cur)) {
                gui.setItem(CONTROL_ROW + i, src);
            }
            session.updateSnapshot(slot, src);
        }
    }

    private static boolean itemEquals(ItemStack a, ItemStack b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.isSimilar(b) && a.getAmount() == b.getAmount();
    }

    private static void drawControls(Inventory gui) {
        ItemStack fill = ItemStack.of(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = 0; slot < CONTROL_ROW; slot++) {
            gui.setItem(slot, fill);
        }
        gui.setItem(1, ItemStack.of(Material.HOPPER));
        gui.setItem(2, ItemStack.of(Material.COMPASS));
        gui.setItem(3, ItemStack.of(Material.IRON_NUGGET));
        gui.setItem(6, ItemStack.of(Material.ARROW));
        gui.setItem(7, ItemStack.of(Material.SPECTRAL_ARROW));
        gui.setItem(8, ItemStack.of(Material.BARRIER));
    }

    private static ItemStack clone(ItemStack item) {
        return item == null || item.getType().isAir() ? null : item.clone();
    }


    private static Component containerTitle(Inventory source) {
        InventoryHolder holder = source.getHolder();
        if (holder instanceof DoubleChest) {
            return Component.translatable("container.chestDouble");
        }
        if (holder instanceof Chest chest) {
            return Component.translatable(chest.getType().translationKey());
        }
        if (holder instanceof Barrel barrel) {
            return Component.translatable(barrel.getType().translationKey());
        }
        if (holder instanceof ShulkerBox shulker) {
            return Component.translatable(shulker.getType().translationKey());
        }
        if (holder instanceof StorageMinecart) {
            return Component.translatable("entity.minecraft.chest_minecart");
        }
        return source.getType().defaultTitle();
    }
}
