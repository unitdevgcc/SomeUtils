package dev.c0redev.someutils.armor;

import dev.c0redev.someutils.SomeUtilsPlugin;
import dev.c0redev.someutils.config.PluginConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import java.util.logging.Level;

public final class ArmorHudManager implements Listener {

    private final SomeUtilsPlugin plugin;
    private final Map<UUID, Boolean> disabled = new ConcurrentHashMap<>();
    private final Map<UUID, ArmorHudPresenter> presenters = new ConcurrentHashMap<>();
    private final Map<UUID, Long> generations = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastApplied = new ConcurrentHashMap<>();
    private BukkitTask task;
    private boolean warnedMissingPe;

    public ArmorHudManager(SomeUtilsPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        PluginConfig cfg = plugin.getPluginConfig();
        if (!cfg.isArmorHudEnabled()) {
            return;
        }
        if (cfg.isArmorHudTextDisplay() && !plugin.isPacketEventsPresent()) {
            if (!warnedMissingPe) {
                plugin.getLogger().warning("Armor HUD text_display needs packetevents; using scoreboard fallback");
                warnedMissingPe = true;
            }
        }
        task = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::tick,
                0L,
                Math.max(1L, cfg.getArmorHudIntervalTicks())
        );
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            generations.merge(player.getUniqueId(), 1L, Long::sum);
            hide(player);
        }
        lastApplied.clear();
    }

    public boolean toggle(Player player) {
        boolean nowDisabled = !disabled.getOrDefault(player.getUniqueId(), false);
        if (nowDisabled) {
            disabled.put(player.getUniqueId(), true);
            generations.merge(player.getUniqueId(), 1L, Long::sum);
            hide(player);
        } else {
            disabled.remove(player.getUniqueId());
        }
        return !nowDisabled;
    }

    public boolean isEnabledFor(Player player) {
        return !disabled.getOrDefault(player.getUniqueId(), false);
    }

    public void refresh(Player player) {
        lastApplied.remove(player.getUniqueId());
        generations.merge(player.getUniqueId(), 1L, Long::sum);
    }

    private void tick() {
        PluginConfig cfg = plugin.getPluginConfig();
        if (!cfg.isArmorHudEnabled()) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isEnabledFor(player)) {
                continue;
            }
            render(player, cfg);
        }
    }

    private void render(Player player, PluginConfig cfg) {
        ArmorSnapshot snap = ArmorSnapshot.capture(player);
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        Vector right = direction.clone().crossProduct(new Vector(0, 1, 0));
        if (right.lengthSquared() < 1.0e-6) right = new Vector(1, 0, 0);
        else right.normalize();
        double horizontal = cfg.isArmorHudRight() ? 0.55 : -0.55;
        Location pos = eye.clone().add(direction.multiply(1.6)).add(right.multiply(horizontal)).add(0, -0.35, 0);
        long generation = generations.getOrDefault(player.getUniqueId(), 0L);
        long sequence = lastApplied.getOrDefault(player.getUniqueId(), 0L) + 1L;
        apply(new ArmorHudRender(
                player.getUniqueId(), generation, sequence,
                new ArmorHudPosition(pos.getX(), pos.getY(), pos.getZ(), eye.getYaw(), eye.getPitch()),
                snap, emptyLabel(player, null), cfg.isArmorHudRight(),
                cfg.isArmorHudCompact(), cfg.getArmorHudPulseThreshold(), cfg.isArmorHudShowOffhand()));
    }

    private void apply(ArmorHudRender render) {
        Player player = Bukkit.getPlayer(render.playerId());
        if (player == null || !player.isOnline()
                || render.generation() != generations.getOrDefault(render.playerId(), 0L)
                || render.sequence() < lastApplied.getOrDefault(render.playerId(), 0L)) return;
        lastApplied.put(render.playerId(), render.sequence());
        PluginConfig cfg = plugin.getPluginConfig();
        ArmorHudPresenter presenter = presenters.get(render.playerId());
        boolean textPresenter = presenter instanceof ArmorTextDisplayPresenter;
        if (presenter == null || textPresenter != cfg.isArmorHudTextDisplay()) {
            hide(player);
            presenter = createPresenter(cfg);
            presenters.put(render.playerId(), presenter);
        }
        try {
            presenter.update(player, render);
        } catch (NoClassDefFoundError | Exception e) {
            plugin.getLogger().log(Level.WARNING, "Armor HUD presenter failed, scoreboard fallback", e);
            hide(player);
            ArmorHudPresenter fallback = new ArmorScoreboardPresenter();
            presenters.put(render.playerId(), fallback);
            fallback.update(player, render);
        }
    }

    private String emptyLabel(Player player, String global) {
        if (global != null) {
            return global;
        }
        return plugin.getLanguageService().tr(player, "hud.empty");
    }

    private ArmorHudPresenter createPresenter(PluginConfig cfg) {
        if (cfg.isArmorHudTextDisplay() && plugin.isPacketEventsPresent()) {
            try {
                return new ArmorTextDisplayPresenter();
            } catch (NoClassDefFoundError | ExceptionInInitializerError e) {
                plugin.getLogger().log(Level.WARNING, "Armor text_display unavailable", e);
            }
        }
        return new ArmorScoreboardPresenter();
    }

    private void hide(Player player) {
        lastApplied.remove(player.getUniqueId());
        ArmorHudPresenter presenter = presenters.remove(player.getUniqueId());
        if (presenter != null) {
            try {
                presenter.remove(player);
            } catch (NoClassDefFoundError ignored) {
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        disabled.remove(event.getPlayer().getUniqueId());
        generations.merge(event.getPlayer().getUniqueId(), 1L, Long::sum);
        hide(event.getPlayer());
    }
}
