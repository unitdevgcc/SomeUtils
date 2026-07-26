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


final class HudBossBarPresenter {

    private static final int MAX_ROWS = 4;
    private static final int MAX_OFFSET_ROWS = 4;

    private final UUID[] ids = new UUID[MAX_ROWS + MAX_OFFSET_ROWS];
    private final HudLineBuilder lineBuilder;
    private int active;
    private int offsetRows;

    HudBossBarPresenter(Player player, HudLineBuilder lineBuilder) {
        this.lineBuilder = lineBuilder;
        for (int row = 0; row < ids.length; row++) {
            ids[row] = UUID.nameUUIDFromBytes(("someutils:waila:" + player.getUniqueId() + ':' + row)
                    .getBytes(StandardCharsets.UTF_8));
        }
    }

    void update(Player player, List<HudLine> lines, boolean packLoaded, int requestedOffset) {
        if (active > 0 && offsetRows != requestedOffset) {
            for (int row = 0; row < active; row++) {
                send(player, new WrapperPlayServerBossBar(ids[row + offsetRows], WrapperPlayServerBossBar.Action.REMOVE));
            }
            active = 0;
        }
        while (offsetRows < requestedOffset) {
            addSpacer(player, offsetRows++);
        }
        while (offsetRows > requestedOffset) {
            send(player, new WrapperPlayServerBossBar(ids[--offsetRows], WrapperPlayServerBossBar.Action.REMOVE));
        }
        for (int row = 0; row < lines.size(); row++) {
            Component title = lineBuilder.decorate(lines.get(row), packLoaded);
            if (row >= active) {
                WrapperPlayServerBossBar packet = new WrapperPlayServerBossBar(ids[row + offsetRows], WrapperPlayServerBossBar.Action.ADD);
                packet.setTitle(title);
                packet.setHealth(0.0f);
                packet.setColor(BossBar.Color.WHITE);
                packet.setOverlay(BossBar.Overlay.PROGRESS);
                packet.setFlags(EnumSet.noneOf(BossBar.Flag.class));
                send(player, packet);
            } else {
                WrapperPlayServerBossBar packet = new WrapperPlayServerBossBar(ids[row + offsetRows], WrapperPlayServerBossBar.Action.UPDATE_TITLE);
                packet.setTitle(title);
                send(player, packet);
            }
        }
        for (int row = lines.size(); row < active; row++) {
            send(player, new WrapperPlayServerBossBar(ids[row + offsetRows], WrapperPlayServerBossBar.Action.REMOVE));
        }
        active = lines.size();
    }

    void remove(Player player) {
        for (int row = 0; row < active; row++) {
            send(player, new WrapperPlayServerBossBar(ids[row + offsetRows], WrapperPlayServerBossBar.Action.REMOVE));
        }
        for (int row = 0; row < offsetRows; row++) {
            send(player, new WrapperPlayServerBossBar(ids[row], WrapperPlayServerBossBar.Action.REMOVE));
        }
        active = 0;
        offsetRows = 0;
    }

    private void addSpacer(Player player, int row) {
        WrapperPlayServerBossBar spacer = new WrapperPlayServerBossBar(ids[row], WrapperPlayServerBossBar.Action.ADD);
        spacer.setTitle(Component.empty());
        spacer.setHealth(0.0f);
        spacer.setColor(BossBar.Color.WHITE);
        spacer.setOverlay(BossBar.Overlay.PROGRESS);
        spacer.setFlags(EnumSet.noneOf(BossBar.Flag.class));
        send(player, spacer);
    }

    private static void send(Player player, WrapperPlayServerBossBar packet) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }
}
