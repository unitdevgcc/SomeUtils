package dev.c0redev.someutils.armor;

import java.util.List;

public final class ArmorScoreboardGlyphs {
    public static final int ICON_WITH_BAR_BASE = 0xE200;
    public static final int FRAME_TOP_BASE = 0xE600;
    public static final int FRAME_RAIL_BASE = 0xE620;
    public static final int FRAME_BOTTOM_BASE = 0xE640;
    public static final int ICON_BACKTRACK = 0xE662;
    public static final int TITLE_BASE = 0xE680;
    public static final int MISSING_BASE = 0xE690;
    public static final int BROKEN_BASE = 0xE694;
    public static final int OFFHAND_ICON = 0xE698;
    public static final int PERCENT_BACKGROUND_BASE = 0xE6A0;
    public static final int BAR_FRAMES = 15;
    public static final int ANIMATION_FRAMES = 32;
    public static final int PERCENT_TEXT_BASE = PERCENT_BACKGROUND_BASE + 5 * 3 * ANIMATION_FRAMES;
    public static final int PERCENT_TEXT_BACKTRACK = PERCENT_TEXT_BASE + 101;
    public static final int CRACK_BASE = PERCENT_TEXT_BACKTRACK + 1;
    public static final int CRACK_FRAMES = 8;
    public static final int CRACK_BACKTRACK = CRACK_BASE + CRACK_FRAMES;

    private static final List<String> MATERIALS = List.of(
            "leather_helmet", "leather_chestplate", "leather_leggings", "leather_boots",
            "chainmail_helmet", "chainmail_chestplate", "chainmail_leggings", "chainmail_boots",
            "iron_helmet", "iron_chestplate", "iron_leggings", "iron_boots",
            "golden_helmet", "golden_chestplate", "golden_leggings", "golden_boots",
            "diamond_helmet", "diamond_chestplate", "diamond_leggings", "diamond_boots",
            "netherite_helmet", "netherite_chestplate", "netherite_leggings", "netherite_boots",
            "copper_helmet", "copper_chestplate", "copper_leggings", "copper_boots",
            "turtle_helmet", "elytra"
    );

    private ArmorScoreboardGlyphs() {
    }

    public static List<String> materials() {
        return MATERIALS;
    }

    public static String iconWithBar(ArmorPiece piece) {
        int frame = durabilityFrame(piece);
        if (piece.empty()) {
            return missing(piece.slot());
        }
        if (piece.broken()) {
            return broken(piece.slot());
        }
        int index = MATERIALS.indexOf(piece.materialKey());
        return index < 0 ? missing(piece.slot())
                : glyph(ICON_WITH_BAR_BASE + index * (BAR_FRAMES + 1) + frame);
    }

    public static String missing(ArmorSlot slot) {
        return glyph(MISSING_BASE + bodyIndex(slot));
    }

    public static String broken(ArmorSlot slot) {
        return glyph(BROKEN_BASE + bodyIndex(slot));
    }

    public static String offhandIcon() {
        return glyph(OFFHAND_ICON);
    }

    public static String percentBackground(ArmorSlot slot, int percent, int animationFrame) {
        int state = percent <= 20 ? 2 : percent <= 50 ? 1 : 0;
        int slotIdx = percentIndex(slot);
        int base = PERCENT_BACKGROUND_BASE + (slotIdx * 3 * ANIMATION_FRAMES)
                + (state * ANIMATION_FRAMES) + Math.floorMod(animationFrame, ANIMATION_FRAMES);
        return glyph(base);
    }


    public static String percentText(int percent) {
        return glyph(PERCENT_TEXT_BASE + Math.clamp(percent, 0, 100));
    }

    public static String percentTextBacktrack() {
        return glyph(PERCENT_TEXT_BACKTRACK);
    }

    public static String crack(int animationFrame) {
        return glyph(CRACK_BASE + Math.floorMod(animationFrame / 2, CRACK_FRAMES));
    }

    public static String crackBacktrack() {
        return glyph(CRACK_BACKTRACK);
    }

    private static int bodyIndex(ArmorSlot slot) {
        return switch (slot) {
            case HELMET -> 0;
            case CHEST -> 1;
            case LEGS -> 2;
            case BOOTS -> 3;
            case MAIN_HAND, OFF_HAND -> 0;
        };
    }

    private static int percentIndex(ArmorSlot slot) {
        return slot == ArmorSlot.OFF_HAND || slot == ArmorSlot.MAIN_HAND ? 4 : bodyIndex(slot);
    }

    public static int durabilityFrame(ArmorPiece piece) {
        if (piece.empty() || piece.maxDurability() <= 0) {
            return 0;
        }
        return Math.clamp(Math.round(piece.remaining() * BAR_FRAMES / (float) piece.maxDurability()), 0, BAR_FRAMES);
    }

    public static String frameTop(int animationFrame) {
        return glyph(FRAME_TOP_BASE + Math.floorMod(animationFrame, ANIMATION_FRAMES));
    }

    public static String frameRail(int animationFrame) {
        return glyph(FRAME_RAIL_BASE + Math.floorMod(animationFrame, ANIMATION_FRAMES));
    }

    public static String frameBottom(int animationFrame) {
        return glyph(FRAME_BOTTOM_BASE + Math.floorMod(animationFrame, ANIMATION_FRAMES));
    }

    public static String iconBacktrack() {
        return glyph(ICON_BACKTRACK);
    }

    public static String glyph(int codePoint) {
        return new String(Character.toChars(codePoint));
    }
}
