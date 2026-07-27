package dev.c0redev.someutils.jade;

import dev.c0redev.someutils.SomeUtilsPlugin;
import dev.c0redev.someutils.config.PluginConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import static dev.c0redev.someutils.jade.HudGlyphs.CRIT_ICON;
import static dev.c0redev.someutils.jade.HudGlyphs.DEFAULT_FONT;
import static dev.c0redev.someutils.jade.HudGlyphs.HUD_FONT;

public final class DamageIndicatorManager implements Listener {

    private final SomeUtilsPlugin plugin;

    public DamageIndicatorManager(SomeUtilsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        PluginConfig cfg = plugin.getPluginConfig();
        if (!cfg.isDamageIndicatorEnabled()) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }
        boolean isPlayer = target instanceof Player;
        if (isPlayer && !cfg.isDamageIndicatorShowPlayers()) {
            return;
        }
        if (!isPlayer && !cfg.isDamageIndicatorShowMobs()) {
            return;
        }
        double damage = event.getFinalDamage();
        if (damage <= 0.0) {
            return;
        }
        spawn(target, damage, event.isCritical(), cfg);
    }

    private void spawn(LivingEntity target, double damage, boolean critical, PluginConfig cfg) {
        Location origin = target.getEyeLocation().add(
                (Math.random() - 0.5) * 0.6,
                0.2,
                (Math.random() - 0.5) * 0.6);

        String amount = String.valueOf(Math.round(damage * 10.0) / 10.0);
        TextColor color = TextColor.fromHexString(critical ? cfg.getDamageIndicatorCritColor() : cfg.getDamageIndicatorNormalColor());
        float scale = critical ? (float) cfg.getDamageIndicatorCritScale() : 1.0f;

        Component text = critical
                ? Component.text(CRIT_ICON).font(HUD_FONT).append(Component.text(" " + amount, color).font(DEFAULT_FONT))
                : Component.text(amount, color).font(DEFAULT_FONT);

        TextDisplay display = target.getWorld().spawn(origin, TextDisplay.class, entity -> {
            entity.text(text);
            entity.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
            entity.setShadowed(false);
            entity.setSeeThrough(true);
            entity.setDefaultBackground(false);
            entity.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
            entity.setPersistent(false);
            entity.setGravity(false);
            entity.setInterpolationDuration(4);
            entity.setInterpolationDelay(0);
            entity.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f),
                    new AxisAngle4f(0f, 0f, 0f, 1f),
                    new Vector3f(scale, scale, scale),
                    new AxisAngle4f(0f, 0f, 0f, 1f)));
        });

        animate(display, origin, cfg.getDamageIndicatorRiseHeight(), cfg.getDamageIndicatorDurationTicks());
    }

    private void animate(TextDisplay display, Location origin, double riseHeight, int durationTicks) {
        new BukkitRunnable() {
            private int tick;

            @Override
            public void run() {
                if (!display.isValid() || tick >= durationTicks) {
                    if (display.isValid()) {
                        display.remove();
                    }
                    cancel();
                    return;
                }
                double progress = tick / (double) durationTicks;
                display.teleport(origin.clone().add(0, riseHeight * progress, 0));
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
