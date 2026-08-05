package dev.c0redev.someutils.jade;

import dev.c0redev.someutils.config.PluginConfig;
import dev.c0redev.someutils.lang.LanguageService;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.util.Locale;

public final class EntityDataProvider {

    private EntityDataProvider() {
    }

    public static TargetInfo getEntityInfo(Entity entity, PluginConfig cfg,
                                           LanguageService languageService, Player player) {
        if (entity == null) {
            return TargetInfo.empty();
        }

        String name = entity.customName() != null
                ? net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(entity.customName())
                : entity.getName();
        StringBuilder subtitle = new StringBuilder();
        StringBuilder detail = new StringBuilder();

        if (entity instanceof LivingEntity living) {
            if (cfg.isShowHealth()) {
                double health = living.getHealth();
                double max = maxHealth(living);
                append(subtitle, languageService.tr(player, "hud.health") + " "
                        + format(health) + "/" + format(max));
            }

            if (cfg.isShowPotions() && !living.getActivePotionEffects().isEmpty()) {
                StringBuilder potions = new StringBuilder(languageService.tr(player, "hud.potions"));
                for (PotionEffect effect : living.getActivePotionEffects()) {
                    potions.append(' ').append(effect.getType().getKey().getKey());
                }
                append(detail, potions.toString());
            }
        }

        if (entity instanceof Ageable ageable && !ageable.isAdult()) {
            append(subtitle, languageService.tr(player, "hud.baby"));
        }

        if (cfg.isShowVillager() && entity instanceof Villager villager) {
            append(subtitle, villager.getProfession().getKey().getKey() + " L" + villager.getVillagerLevel());
        }

        if (cfg.isShowHorse() && entity instanceof AbstractHorse horse) {
            double speed = attr(horse, Attribute.MOVEMENT_SPEED) * 42.16;
            double jump = attr(horse, Attribute.JUMP_STRENGTH);
            double jumpBlocks = -0.1817584952 * jump * jump * jump + 3.689713992 * jump * jump + 2.128599134 * jump - 0.343930367;
            append(detail, languageService.tr(player, "hud.speed") + " " + format(speed)
                    + " m/s | " + languageService.tr(player, "hud.jump") + " " + format(jumpBlocks) + " m");
        }

        return TargetInfo.ofEntity(entity, name, subtitle.toString(), detail.toString());
    }

    private static double maxHealth(LivingEntity living) {
        AttributeInstance inst = living.getAttribute(Attribute.MAX_HEALTH);
        return inst == null ? living.getHealth() : inst.getValue();
    }

    private static double attr(LivingEntity living, Attribute attribute) {
        AttributeInstance inst = living.getAttribute(attribute);
        return inst == null ? 0.0 : inst.getValue();
    }

    private static void append(StringBuilder sb, String part) {
        if (sb.length() > 0) {
            sb.append(" §8| ");
        }
        sb.append(part);
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
