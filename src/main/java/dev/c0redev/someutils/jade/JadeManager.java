package dev.c0redev.someutils.jade;

import dev.c0redev.someutils.SomeUtilsPlugin;
import dev.c0redev.someutils.config.PluginConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class JadeManager implements Listener {

    private final SomeUtilsPlugin plugin;
    private final HudLineBuilder lineBuilder;
    private final Map<UUID, Boolean> disabled = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastHud = new ConcurrentHashMap<>();
    private final Map<UUID, HudBossBarPresenter> bars = new ConcurrentHashMap<>();
    private BukkitTask task;

    public JadeManager(SomeUtilsPlugin plugin) {
        this.plugin = plugin;
        this.lineBuilder = new HudLineBuilder(plugin);
    }

    public void start() {
        stop();
        task = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::tick,
                0L,
                plugin.getPluginConfig().getJadeIntervalTicks()
        );
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            hideHud(player);
        }
        lastHud.clear();
    }

    public boolean toggle(Player player) {
        boolean nowDisabled = !disabled.getOrDefault(player.getUniqueId(), false);
        if (nowDisabled) {
            disabled.put(player.getUniqueId(), true);
            hideHud(player);
        } else {
            disabled.remove(player.getUniqueId());
        }
        return !nowDisabled;
    }

    public boolean isEnabledFor(Player player) {
        return !disabled.getOrDefault(player.getUniqueId(), false);
    }

    public void refresh(Player player) {
        lastHud.remove(player.getUniqueId());
    }

    private void tick() {
        PluginConfig cfg = plugin.getPluginConfig();
        if (!cfg.isJadeEnabled()) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isEnabledFor(player)) {
                render(player, JadeTargetLocator.locate(player, cfg));
            }
        }
    }

    private void render(Player player, TargetInfo target) {
        if (target.isEmpty()) {
            hideHud(player);
            return;
        }

        String fingerprint = target.getFingerprint() + '|' + plugin.getLanguageService().getEffective(player);
        if (fingerprint.equals(lastHud.get(player.getUniqueId()))) {
            return;
        }
        lastHud.put(player.getUniqueId(), fingerprint);

        boolean packLoaded = plugin.getPackServer() != null && plugin.getPackServer().isLoaded(player);
        List<HudLine> lines = lineBuilder.buildLines(player, target, packLoaded);
        HudBossBarPresenter presenter = bars.computeIfAbsent(player.getUniqueId(), id -> new HudBossBarPresenter(player, lineBuilder));
        presenter.update(player, lines, packLoaded, plugin.getPluginConfig().getJadeVerticalOffsetBars());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        disabled.remove(event.getPlayer().getUniqueId());
        hideHud(event.getPlayer());
    }

    private void hideHud(Player player) {
        lastHud.remove(player.getUniqueId());
        HudBossBarPresenter presenter = bars.remove(player.getUniqueId());
        if (presenter != null) {
            presenter.remove(player);
        }
    }
}
