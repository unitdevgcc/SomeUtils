package dev.c0redev.someutils.armor;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;

final class ArmorScoreboardPresenter implements ArmorHudPresenter {
    private static final String OBJ = "su_armor";
    private static final Key FONT = Key.key("someutils", "armor_scoreboard");
    private static final ArmorSlot[] BODY = {
            ArmorSlot.HELMET, ArmorSlot.CHEST, ArmorSlot.LEGS, ArmorSlot.BOOTS
    };
    private static final String[] ENTRY_CODES = {
            "§0", "§1", "§2", "§3", "§4", "§5", "§6", "§7", "§8", "§9", "§a", "§b", "§c", "§d"
    };

    private Scoreboard board;
    private Scoreboard previousBoard;
    private Objective objective;
    private final List<Team> lines = new ArrayList<>();
    private String lastFingerprint = "";
    private ArmorSnapshot lastSnapshot;
    private boolean lastCompact;
    private int lastPulseThreshold = -1;
    private boolean lastShowOffhand;
    private int ticks;
    private int lastAnimFrame = -1;
    private String lastLayoutKey = "";

    @Override
    public void update(Player player, ArmorHudRender render) {
        ensure(player);
        ArmorSnapshot snapshot = render.snapshot();
        String fingerprint = snapshot.fingerprint()
                + "|" + render.compact()
                + "|" + render.pulseThreshold()
                + "|" + render.showOffhand();

        boolean snapshotChanged = !fingerprint.equals(lastFingerprint);
        if (snapshotChanged) {
            lastSnapshot = snapshot;
            lastFingerprint = fingerprint;
            lastCompact = render.compact();
            lastPulseThreshold = render.pulseThreshold();
            lastShowOffhand = render.showOffhand();
        }

        int animFrame = ticks++ % ArmorScoreboardGlyphs.ANIMATION_FRAMES;
        boolean animChanged = animFrame != lastAnimFrame;
        if (snapshotChanged || animChanged) {
            paint(lastSnapshot, lastCompact, lastPulseThreshold, lastShowOffhand,
                    animFrame);
            lastAnimFrame = animFrame;
        }

        if (player.getScoreboard() != board) {
            player.setScoreboard(board);
        }
    }

    @Override
    public void remove(Player player) {
        if (board == null) {
            return;
        }
        if (player.getScoreboard() == board) {
            player.setScoreboard(previousBoard != null ? previousBoard : Bukkit.getScoreboardManager().getMainScoreboard());
        }
        clearLines();
        objective.unregister();
        board = null;
        previousBoard = null;
        objective = null;
        lastFingerprint = "";
        lastSnapshot = null;
        lastAnimFrame = -1;
        lastLayoutKey = "";
        ticks = 0;
    }

    private void ensure(Player player) {
        if (board != null) {
            return;
        }
        previousBoard = player.getScoreboard();
        board = Bukkit.getScoreboardManager().getNewScoreboard();
        objective = board.registerNewObjective(OBJ, Criteria.DUMMY,
                Component.text(ArmorScoreboardGlyphs.glyph(ArmorScoreboardGlyphs.TITLE_BASE)).font(FONT));
        objective.numberFormat(NumberFormat.blank());
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        player.setScoreboard(board);
    }

    private void paint(ArmorSnapshot snap, boolean compact, int pulseThreshold, boolean showOffhand,
                       int animFrame) {
        if (snap == null) {
            return;
        }
        objective.displayName(Component.text(ArmorScoreboardGlyphs.glyph(ArmorScoreboardGlyphs.TITLE_BASE)).font(FONT));
        List<Row> rows = buildRows(snap, compact, showOffhand);
        String layoutKey = rows.size() + ":" + compact + ":" + showOffhand;
        if (!layoutKey.equals(lastLayoutKey)) {
            clearLines();
            for (String entry : List.copyOf(board.getEntries())) {
                board.resetScores(entry);
            }
            for (int i = 0; i < rows.size(); i++) {
                lines.add(registerLine(rows.size() - 1 - i, ENTRY_CODES[i]));
            }
            lastLayoutKey = layoutKey;
        }
        for (int i = 0; i < rows.size(); i++) {
            lines.get(i).prefix(lineComponent(rows.get(i), animFrame, pulseThreshold));
        }
    }

    private List<Row> buildRows(ArmorSnapshot snap, boolean compact, boolean showOffhand) {
        List<Row> rows = new ArrayList<>(6);
        rows.add(Row.frameTop());
        if (!compact) {
            rows.add(Row.spacer());
        }
        for (ArmorSlot slot : BODY) {
            ArmorPiece piece = snap.get(slot);
            if (compact && !visibleBody(piece)) {
                continue;
            }
            rows.add(Row.body(piece));
            rows.add(Row.spacer());
        }
        if (showOffhand) {
            ArmorPiece off = snap.get(ArmorSlot.OFF_HAND);
            if (off != null && !off.empty()) {
                rows.add(Row.offhand(off));
                rows.add(Row.spacer());
            }
        }
        if (!compact) {
            rows.add(Row.spacer());
            rows.add(Row.spacer());
        }
        rows.add(Row.frameBottom());
        return rows;
    }

    private static boolean visibleBody(ArmorPiece piece) {
        if (piece == null || piece.empty()) {
            return false;
        }
        if (piece.broken()) {
            return true;
        }
        if ("elytra".equals(piece.materialKey())) {
            return true;
        }
        return !piece.empty();
    }

    private Component lineComponent(Row row, int animFrame, int pulseThreshold) {
        return switch (row.kind) {
            case FRAME_TOP -> Component.text(ArmorScoreboardGlyphs.frameTop(animFrame), NamedTextColor.WHITE).font(FONT);
            case FRAME_BOTTOM -> Component.text(ArmorScoreboardGlyphs.frameBottom(animFrame), NamedTextColor.WHITE).font(FONT);
            case SPACER -> Component.text(ArmorScoreboardGlyphs.frameRail(animFrame), NamedTextColor.WHITE).font(FONT);
            case BODY -> bodyLine(row.piece, animFrame, pulseThreshold);
            case OFFHAND -> offhandLine(row.piece, animFrame, pulseThreshold);
        };
    }

    private Component bodyLine(ArmorPiece piece, int animFrame, int pulseThreshold) {
        TextColor color = piece.broken() ? NamedTextColor.DARK_RED : NamedTextColor.WHITE;
        Component line = Component.text(ArmorScoreboardGlyphs.frameRail(animFrame), NamedTextColor.WHITE).font(FONT)
                .append(Component.text(ArmorScoreboardGlyphs.iconBacktrack()).font(FONT))
                .append(Component.text(ArmorScoreboardGlyphs.iconWithBar(piece), color).font(FONT));
        if (piece.low(pulseThreshold) && !piece.broken()) {
            line = line.append(Component.text(ArmorScoreboardGlyphs.crackBacktrack()).font(FONT))
                    .append(Component.text(ArmorScoreboardGlyphs.crack(animFrame), NamedTextColor.WHITE).font(FONT));
        }
        if (!piece.empty()) {
            line = line.append(Component.text(ArmorScoreboardGlyphs.percentBackground(piece.slot(), piece.percent(), animFrame)).font(FONT))
                    .append(Component.text(ArmorScoreboardGlyphs.percentTextBacktrack()).font(FONT))
                    .append(Component.text(ArmorScoreboardGlyphs.percentText(piece.percent())).font(FONT));
        }
        return line;
    }

    private Component offhandLine(ArmorPiece piece, int animFrame, int pulseThreshold) {
        TextColor color = piece.broken() ? NamedTextColor.DARK_RED : NamedTextColor.WHITE;
        String icon = piece.empty()
                ? ArmorScoreboardGlyphs.offhandIcon()
                : ArmorScoreboardGlyphs.iconWithBar(piece);
        if (!piece.empty() && ArmorScoreboardGlyphs.materials().indexOf(piece.materialKey()) < 0) {
            icon = ArmorScoreboardGlyphs.offhandIcon();
        }
        Component line = Component.text(ArmorScoreboardGlyphs.frameRail(animFrame), NamedTextColor.WHITE).font(FONT)
                .append(Component.text(ArmorScoreboardGlyphs.iconBacktrack()).font(FONT))
                .append(Component.text(icon, color).font(FONT));
        if (piece.low(pulseThreshold) && !piece.broken()) {
            line = line.append(Component.text(ArmorScoreboardGlyphs.crackBacktrack()).font(FONT))
                    .append(Component.text(ArmorScoreboardGlyphs.crack(animFrame), NamedTextColor.WHITE).font(FONT));
        }
        return line
                .append(Component.text(ArmorScoreboardGlyphs.percentBackground(piece.slot(), piece.percent(), animFrame)).font(FONT))
                .append(Component.text(ArmorScoreboardGlyphs.percentTextBacktrack()).font(FONT))
                .append(Component.text(ArmorScoreboardGlyphs.percentText(piece.percent())).font(FONT));
    }

    private Team registerLine(int score, String entry) {
        Team team = board.registerNewTeam("su_a_" + score + "_" + entry.hashCode());
        team.addEntry(entry);
        Score s = objective.getScore(entry);
        s.setScore(score);
        s.numberFormat(NumberFormat.blank());
        return team;
    }

    private void clearLines() {
        for (Team line : lines) {
            try {
                line.unregister();
            } catch (IllegalStateException ignored) {
            }
        }
        lines.clear();
    }

    private enum Kind { FRAME_TOP, SPACER, FRAME_BOTTOM, BODY, OFFHAND }

    private record Row(Kind kind, ArmorPiece piece) {
        static Row frameTop() {
            return new Row(Kind.FRAME_TOP, null);
        }

        static Row frameBottom() {
            return new Row(Kind.FRAME_BOTTOM, null);
        }

        static Row spacer() {
            return new Row(Kind.SPACER, null);
        }

        static Row body(ArmorPiece piece) {
            return new Row(Kind.BODY, piece);
        }

        static Row offhand(ArmorPiece piece) {
            return new Row(Kind.OFFHAND, piece);
        }
    }
}
