package dev.c0redev.someutils.jade;

import org.bukkit.entity.Player;

import java.util.List;

interface HudPresenter {
    void update(Player player, List<HudLine> lines, boolean packLoaded, int requestedOffset, int lineGapBars);

    void remove(Player player);
}
