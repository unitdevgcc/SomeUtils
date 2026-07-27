package dev.c0redev.someutils.jade;

import dev.c0redev.someutils.SomeUtilsPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

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
        List<HudLine> lines = new ArrayList<>(MAX_ROWS);
        String gap = packLoaded ? ICON_TEXT_GAP : "";
        int iconWidth = packLoaded ? ICON_ADVANCE + HudFontMetrics.estimateWidth(ICON_TEXT_GAP) : 0;

        Component icon = packLoaded
                ? Component.text(target.getType() == TargetInfo.Type.BLOCK ? blockGlyph(target) : ENTITY_ICON, NamedTextColor.WHITE).font(HUD_FONT)
                : Component.empty();
        String title = plugin.getLanguageService().localizeTargetTitle(player, target);
        lines.add(new HudLine(
                Component.empty().append(icon).append(Component.text(gap + title, NamedTextColor.WHITE).font(DEFAULT_FONT)),
                iconWidth + HudFontMetrics.estimateWidth(title)));

        if (!target.getSubtitle().isEmpty()) {
            String text = plugin.getLanguageService().localizeHud(player, target.getSubtitle());
            Component marker = packLoaded ? statusIcon(target.getSubtitle()) : Component.empty();
            lines.add(new HudLine(
                    Component.empty().append(marker).append(Component.text(gap + text, NamedTextColor.GRAY).font(DEFAULT_FONT)),
                    iconWidth + HudFontMetrics.estimateWidth(text)));
        }

        if (target.getTool() != TargetInfo.Tool.NONE) {
            String text = plugin.getLanguageService().tr(player, "tool") + ": " + target.getTool().getLabel();
            Component marker = packLoaded ? Component.text(toolGlyph(target.getTool()), NamedTextColor.YELLOW).font(HUD_FONT) : Component.empty();
            lines.add(new HudLine(
                    Component.empty().append(marker).append(Component.text(gap + text, NamedTextColor.YELLOW).font(DEFAULT_FONT)),
                    iconWidth + HudFontMetrics.estimateWidth(text)));
        }

        if (breakProgress != null && lines.size() < MAX_ROWS) {
            if (packLoaded) {
                Component bar = Component.text(breakProgressGlyph(breakProgress), NamedTextColor.WHITE).font(HUD_FONT);
                lines.add(new HudLine(bar, BREAK_PROGRESS_WIDTH));
            } else {
                String label = plugin.getLanguageService().tr(player, "break.progress");
                int percent = Math.round(breakProgress * 100.0f);
                String text = label + " " + percent + "%";
                lines.add(new HudLine(Component.text(text, NamedTextColor.YELLOW).font(DEFAULT_FONT),
                        HudFontMetrics.estimateWidth(text)));
            }
        }

        if (!target.getDetail().isEmpty() && lines.size() < MAX_ROWS) {
            String text = plugin.getLanguageService().localizeHud(player, target.getDetail());
            lines.add(new HudLine(Component.text(text, NamedTextColor.DARK_GRAY).font(DEFAULT_FONT), HudFontMetrics.estimateWidth(text)));
        }
        return lines;
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

    private static Component statusIcon(String rawText) {
        if (rawText.contains("Unpowered")) {
            return Component.text(UNPOWERED_ICON, NamedTextColor.RED).font(HUD_FONT);
        }
        if (rawText.contains("Powered")) {
            return Component.text(POWERED_ICON, NamedTextColor.GREEN).font(HUD_FONT);
        }
        return Component.text(HEALTH_ICON, NamedTextColor.RED).font(HUD_FONT);
    }
}
