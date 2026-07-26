package dev.c0redev.someutils.invtweaks;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

final class SortGuiHolder implements InventoryHolder {

    final SortSession session;

    SortGuiHolder(SortSession session) {
        this.session = session;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
