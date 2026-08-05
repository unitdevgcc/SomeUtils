package dev.c0redev.someutils.armor;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

public record ArmorPiece(ArmorSlot slot, boolean empty, String name, int damage, int maxDurability) {

    public static ArmorPiece of(ArmorSlot slot, ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return new ArmorPiece(slot, true, "", 0, 0);
        }
        int max = stack.getType().getMaxDurability();
        int dmg = 0;
        ItemMeta meta = stack.getItemMeta();
        if (meta instanceof Damageable damageable && max > 0) {
            dmg = Math.max(0, damageable.getDamage());
        }
        String name = stack.getType().name().toLowerCase().replace('_', ' ');
        return new ArmorPiece(slot, false, name, dmg, max);
    }

    public int remaining() {
        if (empty || maxDurability <= 0) {
            return 0;
        }
        return Math.max(0, maxDurability - damage);
    }

    public int percent() {
        if (empty) {
            return 0;
        }
        if (maxDurability <= 0) {
            return 100;
        }
        return Math.clamp(Math.round(remaining() * 100f / maxDurability), 0, 100);
    }

    public boolean broken() {
        return !empty && maxDurability > 0 && remaining() <= 0;
    }

    public boolean low(int thresholdPercent) {
        return !empty && maxDurability > 0 && percent() <= thresholdPercent;
    }

    public String materialKey() {
        return name.replace(' ', '_');
    }

}
