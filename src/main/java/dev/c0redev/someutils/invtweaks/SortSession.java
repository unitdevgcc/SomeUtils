package dev.c0redev.someutils.invtweaks;

import org.bukkit.Location;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;


final class SortSession {

    final Inventory source;
    int page;
    boolean switching;
    volatile int windowId = -1;


    private final ItemStack[] snapshot;


    private final boolean[] pendingWrite;

    private final int pageCapacity;

    SortSession(Inventory source, boolean evenPageMode) {
        this.source = source;
        this.snapshot = new ItemStack[source.getSize()];
        this.pendingWrite = new boolean[source.getSize()];
        int full = Math.min(45, source.getSize());
        if (evenPageMode && full > 0) {
            int pageCount = (int) Math.ceil(source.getSize() / (double) full);
            this.pageCapacity = pageCount <= 1 ? full : (int) Math.ceil(source.getSize() / (double) pageCount);
        } else {
            this.pageCapacity = full;
        }
    }

    ItemStack snapshotAt(int sourceSlot) {
        return snapshot[sourceSlot];
    }

    void updateSnapshot(int sourceSlot, ItemStack item) {
        snapshot[sourceSlot] = item == null || item.getType().isAir() ? null : item.clone();
    }

    boolean isPending(int sourceSlot) {
        return pendingWrite[sourceSlot];
    }


    void markPagePending() {
        int start = page * capacity();
        for (int i = 0; i < capacity() && start + i < source.getSize(); i++) {
            pendingWrite[start + i] = true;
        }
    }

    void clearPagePending() {
        int start = page * capacity();
        for (int i = 0; i < capacity() && start + i < source.getSize(); i++) {
            pendingWrite[start + i] = false;
        }
    }

    int capacity() {
        return pageCapacity;
    }

    int pages() {
        return (int) Math.ceil(source.getSize() / (double) capacity());
    }


    boolean matches(Inventory other) {
        if (other == null) {
            return false;
        }
        if (other == source) {
            return true;
        }
        if (source instanceof DoubleChestInventory dci) {
            if (other == dci.getLeftSide() || other == dci.getRightSide()) {
                return true;
            }
        }
        InventoryHolder ourHolder = source.getHolder();
        InventoryHolder otherHolder = other.getHolder();
        if (ourHolder != null && ourHolder == otherHolder) {
            return true;
        }
        Location ourLoc = source.getLocation();
        Location otherLoc = other.getLocation();
        return ourLoc != null && ourLoc.equals(otherLoc);
    }
}
