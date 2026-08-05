package dev.c0redev.someutils.jade;

import dev.c0redev.someutils.SomeUtilsPlugin;
import dev.c0redev.someutils.config.PluginConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static dev.c0redev.someutils.jade.HudGlyphs.*;


final class HudLineBuilder {

    private static final int MAX_ROWS = 4;
    private static final int ICON_ADVANCE = 17;
    private static final String ICON_TEXT_GAP = "  ";
    private static final int PANEL_RIGHT_PADDING = 4;
    private static final int PANEL_PART_WIDTH = 10;

    private final SomeUtilsPlugin plugin;

    HudLineBuilder(SomeUtilsPlugin plugin) {
        this.plugin = plugin;
    }

    List<HudLine> buildLines(Player player, TargetInfo target, boolean packLoaded, Float breakProgress) {
        PluginConfig cfg = plugin.getPluginConfig();
        int maxRows = cfg.isJadeCompact() ? 2 : MAX_ROWS;
        List<HudLine> lines = new ArrayList<>(maxRows);
        boolean showIcons = packLoaded && cfg.isJadeShowIcons();
        String gap = showIcons ? ICON_TEXT_GAP : "";
        int iconWidth = showIcons ? ICON_ADVANCE + HudFontMetrics.estimateWidth(ICON_TEXT_GAP) : 0;
        TextColor primary = color(cfg.getArmorHudPrimary(), NamedTextColor.WHITE);
        TextColor accent = color(cfg.getArmorHudAccent(), NamedTextColor.YELLOW);

        Component icon = showIcons
                ? Component.text(target.getType() == TargetInfo.Type.BLOCK ? blockGlyph(target) : ENTITY_ICON, NamedTextColor.WHITE).font(HUD_FONT)
                : Component.empty();
        String title = plugin.getLanguageService().localizeTargetTitle(player, target);
        lines.add(new HudLine(
                Component.empty().append(icon).append(Component.text(gap + title, primary).font(DEFAULT_FONT)),
                iconWidth + HudFontMetrics.estimateWidth(title)));

        if (cfg.isJadeCompact() && breakProgress != null) {
            addBreakProgress(lines, player, packLoaded, breakProgress, maxRows, accent);
        }

        if (!target.getSubtitle().isEmpty() && lines.size() < maxRows) {
            String text = plugin.getLanguageService().localizeHud(player, target.getSubtitle());
            Component marker = showIcons ? statusIcon(target.getSubtitle(), player) : Component.empty();
            lines.add(new HudLine(
                    Component.empty().append(marker).append(Component.text(gap + text, accent).font(DEFAULT_FONT)),
                    iconWidth + HudFontMetrics.estimateWidth(text)));
        }

        if (target.getTool() != TargetInfo.Tool.NONE && lines.size() < maxRows) {
            String text = plugin.getLanguageService().tr(player, "tool") + ": " + target.getTool().getLabel();
            Component marker = showIcons ? Component.text(toolGlyph(target.getTool()), accent).font(HUD_FONT) : Component.empty();
            lines.add(new HudLine(
                    Component.empty().append(marker).append(Component.text(gap + text, accent).font(DEFAULT_FONT)),
                    iconWidth + HudFontMetrics.estimateWidth(text)));
        }

        if (!cfg.isJadeCompact() && breakProgress != null) {
            addBreakProgress(lines, player, packLoaded, breakProgress, maxRows, accent);
        }

        if (cfg.isJadeShowDetails() && !target.getDetail().isEmpty() && lines.size() < maxRows) {
            String text = plugin.getLanguageService().localizeHud(player, target.getDetail());
            lines.add(new HudLine(Component.text(text, accent).font(DEFAULT_FONT), HudFontMetrics.estimateWidth(text)));
        }
        return lines;
    }

    private void addBreakProgress(List<HudLine> lines, Player player, boolean packLoaded, float progress,
                                  int maxRows, TextColor accent) {
        if (lines.size() >= maxRows) return;
        if (packLoaded) {
            lines.add(new HudLine(Component.text(breakProgressGlyph(progress), NamedTextColor.WHITE).font(HUD_FONT),
                    BREAK_PROGRESS_WIDTH));
            return;
        }
        String text = plugin.getLanguageService().tr(player, "break.progress") + " " + Math.round(progress * 100.0f) + "%";
        lines.add(new HudLine(Component.text(text, accent).font(DEFAULT_FONT), HudFontMetrics.estimateWidth(text)));
    }

    private static TextColor color(String value, TextColor fallback) {
        TextColor color = TextColor.fromHexString(value);
        return color == null ? fallback : color;
    }

    Component decorate(HudLine line, boolean packLoaded) {
        if (!packLoaded) {
            return line.component();
        }
        int requiredInnerWidth = line.width() + PANEL_RIGHT_PADDING;
        int parts = Math.max(4, Math.min(40, (requiredInnerWidth + PANEL_PART_WIDTH - 1) / PANEL_PART_WIDTH));
        String background = PANEL_START + MOVE_MINUS_ONE + (PANEL_PART + MOVE_MINUS_ONE).repeat(parts) + PANEL_END;
        String backtrack = new String(Character.toChars(PANEL_MOVE_START + parts));
        int tail = Math.max(0, Math.min(16, parts * PANEL_PART_WIDTH - line.width()));
        String tailMove = new String(Character.toChars(PANEL_TAIL_START + tail));
        return Component.text(background).font(WAILA_FONT)
                .append(Component.text(backtrack).font(WAILA_FONT))
                .append(line.component())
                .append(Component.text(tailMove).font(WAILA_FONT));
    }

    private String blockGlyph(TargetInfo target) {
        Material mat = target.getMaterial();
        if (mat == Material.CHEST || mat == Material.TRAPPED_CHEST || mat == Material.ENDER_CHEST
                || mat == Material.BARREL || mat.name().contains("SHULKER_BOX")) {
            return CONTAINER_ICON;
        }
        return plugin.getPackServer().getBlockGlyph(mat);
    }

    private static String toolGlyph(TargetInfo.Tool tool) {
        return switch (tool) {
        case PICKAXE -> PICKAXE_ICON;
        case AXE -> AXE_ICON;
        case SHOVEL -> SHOVEL_ICON;
        case SHEARS -> SHEARS_ICON;
        default -> "";
        };
    }

    private Component statusIcon(String rawText, Player player) {
        if (rawText.contains(plugin.getLanguageService().tr(player, "hud.unpowered"))) {
            return Component.text(UNPOWERED_ICON, NamedTextColor.RED).font(HUD_FONT);
        }
        if (rawText.contains(plugin.getLanguageService().tr(player, "hud.powered"))) {
            return Component.text(POWERED_ICON, NamedTextColor.GREEN).font(HUD_FONT);
        }
        return Component.text(HEALTH_ICON, NamedTextColor.RED).font(HUD_FONT);
    }
}
