package dev.c0redev.someutils.armor;

import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.Map;

public final class ArmorSnapshot {

    private final Map<ArmorSlot, ArmorPiece> pieces;

    ArmorSnapshot(Map<ArmorSlot, ArmorPiece> pieces) {
        this.pieces = pieces;
    }

    public static ArmorSnapshot capture(Player player) {
        EntityEquipment eq = player.getEquipment();
        Map<ArmorSlot, ArmorPiece> map = new EnumMap<>(ArmorSlot.class);
        map.put(ArmorSlot.HELMET, ArmorPiece.of(ArmorSlot.HELMET, item(eq, ArmorSlot.HELMET)));
        map.put(ArmorSlot.CHEST, ArmorPiece.of(ArmorSlot.CHEST, item(eq, ArmorSlot.CHEST)));
        map.put(ArmorSlot.LEGS, ArmorPiece.of(ArmorSlot.LEGS, item(eq, ArmorSlot.LEGS)));
        map.put(ArmorSlot.BOOTS, ArmorPiece.of(ArmorSlot.BOOTS, item(eq, ArmorSlot.BOOTS)));
        map.put(ArmorSlot.MAIN_HAND, ArmorPiece.of(ArmorSlot.MAIN_HAND, eq == null ? null : eq.getItemInMainHand()));
        map.put(ArmorSlot.OFF_HAND, ArmorPiece.of(ArmorSlot.OFF_HAND, eq == null ? null : eq.getItemInOffHand()));
        return new ArmorSnapshot(map);
    }

    private static ItemStack item(EntityEquipment eq, ArmorSlot slot) {
        if (eq == null) {
            return null;
        }
        return switch (slot) {
            case HELMET -> eq.getHelmet();
            case CHEST -> eq.getChestplate();
            case LEGS -> eq.getLeggings();
            case BOOTS -> eq.getBoots();
            case MAIN_HAND, OFF_HAND -> null;
        };
    }

    public ArmorPiece get(ArmorSlot slot) {
        return pieces.get(slot);
    }

    public String fingerprint() {
        StringBuilder sb = new StringBuilder(64);
        for (ArmorSlot slot : ArmorSlot.values()) {
            ArmorPiece p = pieces.get(slot);
            sb.append(p.empty()).append('|').append(p.name()).append('|')
                    .append(p.damage()).append('|').append(p.maxDurability()).append(';');
        }
        return sb.toString();
    }
}
