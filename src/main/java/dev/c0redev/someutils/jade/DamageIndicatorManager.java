package dev.c0redev.someutils.jade;

import dev.c0redev.someutils.SomeUtilsPlugin;
import dev.c0redev.someutils.config.PluginConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static dev.c0redev.someutils.jade.HudGlyphs.CRIT_ICON;
import static dev.c0redev.someutils.jade.HudGlyphs.DEFAULT_FONT;
import static dev.c0redev.someutils.jade.HudGlyphs.HUD_FONT;

public final class DamageIndicatorManager implements Listener {

    private static final int MAX_ACTIVE = 48;

    private final SomeUtilsPlugin plugin;
    private final AtomicInteger active = new AtomicInteger();
    private final Set<BukkitTask> tasks = ConcurrentHashMap.newKeySet();
    private final Set<TextDisplay> displays = ConcurrentHashMap.newKeySet();

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
        if (active.get() >= MAX_ACTIVE) {
            return;
        }
        spawn(target, damage, event.isCritical(), cfg);
    }

    public void shutdown() {
        for (BukkitTask task : tasks) {
            task.cancel();
        }
        tasks.clear();
        for (TextDisplay display : displays) {
            if (display.isValid()) {
                display.remove();
            }
        }
        displays.clear();
        active.set(0);
    }

    private void spawn(LivingEntity target, double damage, boolean critical, PluginConfig cfg) {
        int next = active.incrementAndGet();
        if (next > MAX_ACTIVE) {
            active.decrementAndGet();
            return;
        }

        Location origin = target.getEyeLocation().add(
                (Math.random() - 0.5) * 0.6,
                0.2,
                (Math.random() - 0.5) * 0.6);

        String amount = String.format(Locale.ROOT, "%.1f", Math.round(damage * 10.0) / 10.0);
        TextColor color = TextColor.fromHexString(
                critical ? cfg.getDamageIndicatorCritColor() : cfg.getDamageIndicatorNormalColor());
        if (color == null) {
            color = critical ? TextColor.color(255, 77, 77) : TextColor.color(255, 225, 77);
        }
        float scale = critical ? (float) cfg.getDamageIndicatorCritScale() : 1.0f;

        Component text = critical
                ? Component.text(CRIT_ICON).font(HUD_FONT).append(Component.text(" " + amount, color).font(DEFAULT_FONT))
                : Component.text(amount, color).font(DEFAULT_FONT);

        final TextDisplay display;
        try {
            display = target.getWorld().spawn(origin, TextDisplay.class, entity -> {
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
        } catch (Exception e) {
            active.updateAndGet(v -> Math.max(0, v - 1));
            return;
        }
        displays.add(display);

        animate(display, origin, cfg.getDamageIndicatorRiseHeight(), cfg.getDamageIndicatorDurationTicks());
    }

    private void animate(TextDisplay display, Location origin, double riseHeight, int durationTicks) {
        BukkitTask[] holder = new BukkitTask[1];
        holder[0] = new BukkitRunnable() {
            private int tick;

            @Override
            public void run() {
                if (!display.isValid() || tick >= durationTicks) {
                    if (display.isValid()) {
                        display.remove();
                    }
                    displays.remove(display);
                    cancel();
                    BukkitTask self = holder[0];
                    if (self != null) {
                        tasks.remove(self);
                    }
                    active.updateAndGet(v -> Math.max(0, v - 1));
                    return;
                }
                double progress = tick / (double) durationTicks;
                display.teleport(origin.clone().add(0, riseHeight * progress, 0));
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
        tasks.add(holder[0]);
    }
}
