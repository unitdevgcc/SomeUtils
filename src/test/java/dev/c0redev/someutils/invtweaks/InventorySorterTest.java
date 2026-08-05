package dev.c0redev.someutils.invtweaks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// sort/stack требуют Bukkit ItemStack — без runtime только контракт enum
class InventorySorterTest {

    @Test
    void sortModesAreStable() {
        assertArrayEquals(
                new InventorySorter.SortMode[]{
                        InventorySorter.SortMode.DEFAULT,
                        InventorySorter.SortMode.COLUMNS,
                        InventorySorter.SortMode.STACK_ONLY
                },
                InventorySorter.SortMode.values()
        );
        assertEquals(InventorySorter.SortMode.DEFAULT, InventorySorter.SortMode.valueOf("DEFAULT"));
        assertNotNull(InventorySorter.SortMode.COLUMNS.name());
    }

    @Test
    void columnsRequireACompleteNineSlotGrid() {
        assertEquals(false, InventorySorter.isColumnLayoutSupported(5));
        assertEquals(false, InventorySorter.isColumnLayoutSupported(10));
        assertEquals(true, InventorySorter.isColumnLayoutSupported(27));
    }

}
