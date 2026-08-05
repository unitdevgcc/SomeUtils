package dev.c0redev.someutils.armor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArmorPieceTest {

    @Test
    void emptyHasNoDurability() {
        ArmorPiece empty = new ArmorPiece(ArmorSlot.HELMET, true, "", 0, 0);
        assertTrue(empty.empty());
        assertEquals(0, empty.remaining());
    }

    @Test
    void durabilityRemaining() {
        ArmorPiece piece = new ArmorPiece(ArmorSlot.CHEST, false, "iron chestplate", 40, 240);
        assertFalse(piece.empty());
        assertEquals(200, piece.remaining());
    }

    @Test
    void unbreakableNoMax() {
        ArmorPiece piece = new ArmorPiece(ArmorSlot.BOOTS, false, "leather boots", 0, 0);
        assertEquals(0, piece.remaining());
    }

    @Test
    void scoreboardUsesMaterialAndDiscreteDurability() {
        ArmorPiece piece = new ArmorPiece(ArmorSlot.CHEST, false, "diamond chestplate", 264, 528);
        assertEquals("diamond_chestplate", piece.materialKey());
        assertEquals(8, ArmorScoreboardGlyphs.durabilityFrame(piece));
        assertEquals(0, ArmorScoreboardGlyphs.durabilityFrame(
                new ArmorPiece(ArmorSlot.CHEST, true, "", 0, 0)));
        assertFalse(ArmorScoreboardGlyphs.iconWithBar(piece).isBlank());
        ArmorPiece worn = new ArmorPiece(ArmorSlot.CHEST, false, "diamond chestplate", 0, 528);
        assertNotEquals(ArmorScoreboardGlyphs.iconWithBar(piece), ArmorScoreboardGlyphs.iconWithBar(worn));
    }

    @Test
    void percentBrokenLowAndStatusGlyphs() {
        ArmorPiece half = new ArmorPiece(ArmorSlot.HELMET, false, "iron helmet", 50, 100);
        assertEquals(50, half.percent());
        assertFalse(half.broken());
        assertTrue(half.low(50));
        assertFalse(half.low(20));

        ArmorPiece broken = new ArmorPiece(ArmorSlot.BOOTS, false, "iron boots", 100, 100);
        assertTrue(broken.broken());
        assertEquals(0, broken.percent());
        assertEquals(ArmorScoreboardGlyphs.broken(ArmorSlot.BOOTS), ArmorScoreboardGlyphs.iconWithBar(broken));

        ArmorPiece empty = new ArmorPiece(ArmorSlot.LEGS, true, "", 0, 0);
        assertEquals(ArmorScoreboardGlyphs.missing(ArmorSlot.LEGS), ArmorScoreboardGlyphs.iconWithBar(empty));
        assertFalse(ArmorScoreboardGlyphs.offhandIcon().isBlank());
    }

}
