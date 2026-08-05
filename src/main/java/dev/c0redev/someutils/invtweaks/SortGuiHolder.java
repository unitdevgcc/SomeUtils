package dev.c0redev.someutils.invtweaks;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

final class SortGuiHolder implements InventoryHolder {
    final SortSession session;
    private Inventory inventory;

    SortGuiHolder(SortSession session) {
        this.session = session;
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
