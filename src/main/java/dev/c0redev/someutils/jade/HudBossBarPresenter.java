package dev.c0redev.someutils.jade;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBossBar;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

final class HudBossBarPresenter implements HudPresenter {

    private static final int MAX_ROWS = 4;
    private static final int MAX_OFFSET_ROWS = 4;
    private static final int MAX_GAP_BARS = 2;
    private static final int MAX_SLOTS = MAX_OFFSET_ROWS + MAX_ROWS * (1 + MAX_GAP_BARS);

    private final UUID[] ids = new UUID[MAX_SLOTS];
    private final HudLineBuilder lineBuilder;
    private int usedSlots;

    HudBossBarPresenter(Player player, HudLineBuilder lineBuilder) {
        this.lineBuilder = lineBuilder;
        for (int slot = 0; slot < ids.length; slot++) {
            ids[slot] = UUID.nameUUIDFromBytes(("someutils:waila:" + player.getUniqueId() + ':' + slot)
                    .getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    public void update(Player player, List<HudLine> lines, boolean packLoaded, int requestedOffset, int lineGapBars) {
        int slot = 0;
        for (int spacer = 0; spacer < requestedOffset; spacer++) {
            sendSlot(player, slot++, Component.empty(), slot <= usedSlots);
        }
        for (int row = 0; row < lines.size(); row++) {
            if (row > 0) {
                for (int gap = 0; gap < lineGapBars; gap++) {
                    sendSlot(player, slot++, Component.empty(), slot <= usedSlots);
                }
            }
            Component title = lineBuilder.decorate(lines.get(row), packLoaded);
            sendSlot(player, slot++, title, slot <= usedSlots);
        }
        for (int leftover = slot; leftover < usedSlots; leftover++) {
            send(player, new WrapperPlayServerBossBar(ids[leftover], WrapperPlayServerBossBar.Action.REMOVE));
        }
        usedSlots = slot;
    }

    @Override
    public void remove(Player player) {
        for (int slot = 0; slot < usedSlots; slot++) {
            send(player, new WrapperPlayServerBossBar(ids[slot], WrapperPlayServerBossBar.Action.REMOVE));
        }
        usedSlots = 0;
    }

    private void sendSlot(Player player, int slot, Component title, boolean alreadyActive) {
        if (alreadyActive) {
            WrapperPlayServerBossBar packet = new WrapperPlayServerBossBar(ids[slot], WrapperPlayServerBossBar.Action.UPDATE_TITLE);
            packet.setTitle(title);
            send(player, packet);
        } else {
            WrapperPlayServerBossBar packet = new WrapperPlayServerBossBar(ids[slot], WrapperPlayServerBossBar.Action.ADD);
            packet.setTitle(title);
            packet.setHealth(0.0f);
            packet.setColor(BossBar.Color.WHITE);
            packet.setOverlay(BossBar.Overlay.PROGRESS);
            packet.setFlags(EnumSet.noneOf(BossBar.Flag.class));
            send(player, packet);
        }
    }

    private static void send(Player player, WrapperPlayServerBossBar packet) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }
}
