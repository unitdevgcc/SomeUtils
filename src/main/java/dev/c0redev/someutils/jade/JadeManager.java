package dev.c0redev.someutils.jade;

import dev.c0redev.someutils.SomeUtilsPlugin;
import dev.c0redev.someutils.config.PluginConfig;
import io.papermc.paper.event.block.BlockBreakProgressUpdateEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockBreakEvent;
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
    private final Map<UUID, BreakProgress> breaking = new ConcurrentHashMap<>();
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
        breaking.clear();
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

        Float breakProgress = getBreakProgress(player, target);
        String fingerprint = target.getFingerprint() + '|' + breakProgress + '|' + plugin.getLanguageService().getEffective(player);
        if (fingerprint.equals(lastHud.get(player.getUniqueId()))) {
            return;
        }
        lastHud.put(player.getUniqueId(), fingerprint);

        boolean packLoaded = plugin.getPackServer() != null && plugin.getPackServer().isLoaded(player);
        List<HudLine> lines = lineBuilder.buildLines(player, target, packLoaded, breakProgress);
        HudBossBarPresenter presenter = bars.computeIfAbsent(player.getUniqueId(), id -> new HudBossBarPresenter(player, lineBuilder));
        presenter.update(player, lines, packLoaded,
                plugin.getPluginConfig().getJadeVerticalOffsetBars(),
                plugin.getPluginConfig().getJadeLineGapBars());
    }

    @EventHandler
    public void onBreakProgress(BlockBreakProgressUpdateEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        float progress = Math.max(0.0f, Math.min(1.0f, event.getProgress()));
        if (progress <= 0.0f) {
            breaking.remove(player.getUniqueId());
        } else {
            breaking.put(player.getUniqueId(), new BreakProgress(event.getBlock().getLocation(), progress));
        }
        lastHud.remove(player.getUniqueId());
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        breaking.remove(event.getPlayer().getUniqueId());
        lastHud.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onBreakAbort(BlockDamageAbortEvent event) {
        breaking.remove(event.getPlayer().getUniqueId());
        lastHud.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        disabled.remove(event.getPlayer().getUniqueId());
        breaking.remove(event.getPlayer().getUniqueId());
        hideHud(event.getPlayer());
    }

    private Float getBreakProgress(Player player, TargetInfo target) {
        if (target.getType() != TargetInfo.Type.BLOCK) {
            return null;
        }
        BreakProgress progress = breaking.get(player.getUniqueId());
        if (progress == null || !progress.location.equals(target.getBlock().getLocation())) {
            return null;
        }
        return progress.value;
    }

    private void hideHud(Player player) {
        lastHud.remove(player.getUniqueId());
        HudBossBarPresenter presenter = bars.remove(player.getUniqueId());
        if (presenter != null) {
            presenter.remove(player);
        }
    }

    private record BreakProgress(Location location, float value) {
    }
}
