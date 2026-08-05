package dev.c0redev.someutils.invtweaks;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class InventorySorter {

    public enum SortMode {
        DEFAULT,
        COLUMNS,
        STACK_ONLY
    }

    private InventorySorter() {
    }

    public static boolean sortOpen(Player player, SortMode mode, boolean sortPlayerInventory, boolean sortHotbar) {
        InventoryView view = player.getOpenInventory();
        Inventory top = view.getTopInventory();

        if (top != null && top.getHolder() != player && top.getSize() > 0) {
            if (!SortContainerGui.isSupported(top)) {
                return false;
            }
            sortInventory(top, mode);
        } else if (sortPlayerInventory) {
            sortPlayerInventory(player.getInventory(), mode, sortHotbar);
        } else {
            return false;
        }
        player.updateInventory();
        return true;
    }

    public static void sortInventory(Inventory inv, SortMode mode) {
        if (mode == SortMode.STACK_ONLY) {
            stackInPlace(inv, 0, inv.getSize());
            return;
        }

        List<ItemStack> items = extract(inv, 0, inv.getSize());
        items = stackItems(items);
        items.sort(itemComparator());
        clearRange(inv, 0, inv.getSize());

        if (mode == SortMode.COLUMNS && isColumnLayoutSupported(inv.getSize())) {
            fillByColumns(inv, items, inv.getSize() / 9, 9, 0);
        } else {
            for (int i = 0; i < items.size() && i < inv.getSize(); i++) {
                inv.setItem(i, items.get(i));
            }
        }
    }

    static boolean isColumnLayoutSupported(int size) {
        return size > 0 && size % 9 == 0;
    }

    public static void sortPlayerInventory(PlayerInventory inv, SortMode mode, boolean sortHotbar) {
        int first = sortHotbar ? 0 : 9;
        int last = 36;
        if (mode == SortMode.STACK_ONLY) {
            stackInPlace(inv, first, last);
            return;
        }

        List<ItemStack> items = extract(inv, first, last);
        items = stackItems(items);
        items.sort(itemComparator());
        clearRange(inv, first, last);

        int rows = sortHotbar ? 4 : 3;
        int span = last - first;
        if (mode == SortMode.COLUMNS && span % 9 == 0) {
            fillByColumns(inv, items, rows, 9, first);
        } else {
            for (int i = 0; i < items.size() && i < span; i++) {
                inv.setItem(i + first, items.get(i));
            }
        }
    }

    private static List<ItemStack> extract(Inventory inv, int from, int to) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = from; i < to; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR && !item.getType().isAir()) {
                items.add(item.clone());
            }
        }
        return items;
    }

    private static void clearRange(Inventory inv, int from, int to) {
        for (int i = from; i < to; i++) {
            inv.setItem(i, null);
        }
    }

    private static void fillByColumns(Inventory inv, List<ItemStack> items, int rows, int cols, int offset) {
        int idx = 0;
        for (int col = 0; col < cols && idx < items.size(); col++) {
            for (int row = 0; row < rows && idx < items.size(); row++) {
                inv.setItem(offset + row * cols + col, items.get(idx++));
            }
        }
    }

    private static void stackInPlace(Inventory inv, int from, int to) {
        for (int i = from; i < to; i++) {
            ItemStack base = inv.getItem(i);
            if (base == null || base.getType().isAir()) {
                continue;
            }
            for (int j = i + 1; j < to; j++) {
                ItemStack other = inv.getItem(j);
                if (other == null || other.getType().isAir() || !base.isSimilar(other)) {
                    continue;
                }
                int space = base.getMaxStackSize() - base.getAmount();
                if (space <= 0) {
                    break;
                }
                int add = Math.min(space, other.getAmount());
                base.setAmount(base.getAmount() + add);
                other.setAmount(other.getAmount() - add);
                if (other.getAmount() <= 0) {
                    inv.setItem(j, null);
                }
            }
        }
    }

    private static List<ItemStack> stackItems(List<ItemStack> items) {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack item : items) {
            boolean merged = false;
            for (ItemStack existing : result) {
                if (!existing.isSimilar(item)) {
                    continue;
                }
                int space = existing.getMaxStackSize() - existing.getAmount();
                if (space <= 0) {
                    continue;
                }
                int add = Math.min(space, item.getAmount());
                existing.setAmount(existing.getAmount() + add);
                item.setAmount(item.getAmount() - add);
                if (item.getAmount() <= 0) {
                    merged = true;
                    break;
                }
            }
            if (!merged && item.getAmount() > 0) {
                result.add(item);
            }
        }
        return result;
    }

    private static Comparator<ItemStack> itemComparator() {
        return (a, b) -> {
            int cat = Integer.compare(category(a.getType()), category(b.getType()));
            if (cat != 0) {
                return cat;
            }
            int name = a.getType().name().compareTo(b.getType().name());
            if (name != 0) {
                return name;
            }
            return Integer.compare(b.getAmount(), a.getAmount());
        };
    }

    private static int category(Material mat) {
        String n = mat.name();
        if (n.contains("SWORD") || n.contains("BOW") || n.contains("CROSSBOW")
                || n.contains("TRIDENT") || n.contains("MACE")) return 1;
        if (n.contains("HELMET") || n.contains("CHESTPLATE") || n.contains("LEGGINGS")
                || n.contains("BOOTS") || n.contains("SHIELD")) return 2;
        if (n.contains("PICKAXE") || n.contains("AXE") || n.contains("SHOVEL")
                || n.contains("HOE") || n.contains("SHEARS") || n.contains("FLINT_AND_STEEL")) return 3;
        if (mat.isEdible() || n.contains("POTION") || n.contains("GOLDEN_APPLE")
                || n.contains("BOTTLE")) return 4;
        if (n.contains("ORE") || n.contains("INGOT") || n.contains("RAW_")
                || n.contains("DIAMOND") || n.contains("EMERALD") || n.contains("NUGGET")
                || n.contains("SCRAP") || n.contains("AMETHYST")) return 5;
        if (n.contains("REDSTONE") || n.contains("REPEATER") || n.contains("COMPARATOR")
                || n.contains("PISTON") || n.contains("OBSERVER") || n.contains("HOPPER")
                || n.contains("DROPPER") || n.contains("DISPENSER")) return 6;
        if (mat.isBlock()) return 7;
        return 10;
    }
}
