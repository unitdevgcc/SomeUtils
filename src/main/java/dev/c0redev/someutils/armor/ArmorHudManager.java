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
import java.util.logging.Level;

public final class ArmorHudManager implements Listener {

    private final SomeUtilsPlugin plugin;
    private final Map<UUID, Boolean> disabled = new ConcurrentHashMap<>();
    private final Map<UUID, ArmorScoreboardPresenter> presenters = new ConcurrentHashMap<>();
    private BukkitTask task;

    public ArmorHudManager(SomeUtilsPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        PluginConfig cfg = plugin.getPluginConfig();
        if (!cfg.isArmorHudEnabled()) {
            return;
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
            hide(player);
        }
    }

    public boolean toggle(Player player) {
        boolean nowDisabled = !disabled.getOrDefault(player.getUniqueId(), false);
        if (nowDisabled) {
            disabled.put(player.getUniqueId(), true);
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
        hide(player);
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
        ArmorScoreboardPresenter presenter = presenters.computeIfAbsent(
                player.getUniqueId(), ignored -> new ArmorScoreboardPresenter());
        try {
            presenter.update(player, ArmorSnapshot.capture(player), cfg.isArmorHudCompact(),
                    cfg.getArmorHudPulseThreshold(), cfg.isArmorHudShowOffhand());
        } catch (RuntimeException e) {
            plugin.getLogger().log(Level.WARNING, "Armor HUD update failed for " + player.getName(), e);
            hide(player);
        }
    }

    private void hide(Player player) {
        ArmorScoreboardPresenter presenter = presenters.remove(player.getUniqueId());
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
        hide(event.getPlayer());
    }
}
