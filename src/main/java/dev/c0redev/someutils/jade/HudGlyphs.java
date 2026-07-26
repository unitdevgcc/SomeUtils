package dev.c0redev.someutils.jade;

import net.kyori.adventure.key.Key;


final class HudGlyphs {

    static final Key HUD_FONT = Key.key("someutils", "jade");
    static final Key WAILA_FONT = Key.key("someutils", "waila");
    static final Key DEFAULT_FONT = Key.key("minecraft", "default");

    static final String PANEL_START = "\uF000";
    static final String PANEL_PART = "\uF001";
    static final String PANEL_END = "\uF002";
    static final String MOVE_MINUS_ONE = "\uF0FF";
    static final int PANEL_MOVE_START = 0xF100;
    static final int PANEL_TAIL_START = 0xF200;

    static final String ENTITY_ICON = "\uE008";
    static final String HEALTH_ICON = "\uE009";
    static final String PICKAXE_ICON = "\uE00A";
    static final String AXE_ICON = "\uE00B";
    static final String SHOVEL_ICON = "\uE00C";
    static final String SHEARS_ICON = "\uE00D";
    static final String POWERED_ICON = "\uE00E";
    static final String UNPOWERED_ICON = "\uE00F";
    static final String CONTAINER_ICON = "\uE010";

    private HudGlyphs() {
    }
}
