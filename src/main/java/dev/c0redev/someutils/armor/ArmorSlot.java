package dev.c0redev.someutils.armor;

public enum ArmorSlot {
    HELMET,
    CHEST,
    LEGS,
    BOOTS,
    MAIN_HAND,
    OFF_HAND;

    public String shortLabel() {
        return switch (this) {
            case HELMET -> "H";
            case CHEST -> "C";
            case LEGS -> "L";
            case BOOTS -> "B";
            case MAIN_HAND -> "M";
            case OFF_HAND -> "O";
        };
    }
}
