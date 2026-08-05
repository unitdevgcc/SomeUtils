package dev.c0redev.someutils.armor;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Quaternion4f;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.key.Key;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

final class ArmorTextDisplayPresenter implements ArmorHudPresenter {

    // индексы metadata для Display/TextDisplay в 1.21
    private static final int META_NO_GRAVITY = 5;
    private static final int META_INTERPOLATION_DELAY = 8;
    private static final int META_INTERPOLATION_DURATION = 9;
    private static final int META_TELEPORT_DURATION = 10;
    private static final int META_TRANSLATION = 11;
    private static final int META_SCALE = 12;
    private static final int META_LEFT_ROT = 13;
    private static final int META_RIGHT_ROT = 14;
    private static final int META_BILLBOARD = 15;
    private static final int META_VIEW_RANGE = 17;
    private static final int META_WIDTH = 20;
    private static final int META_HEIGHT = 21;
    private static final int META_TEXT = 23;
    private static final int META_LINE_WIDTH = 24;
    private static final int META_BG = 25;
    private static final int META_OPACITY = 26;
    private static final int META_STYLE = 27;

    private static final byte BILLBOARD_CENTER = 3;
    private static final byte STYLE_SHADOW = 0x01;
    private static final Key ARMOR_FONT = Key.key("someutils", "armor");
    private static final Key DEFAULT_FONT = Key.key("minecraft", "default");
    private static final String PANEL_LINE = "\uE110";
    private static final String PANEL_BACKTRACK = "\uE111";

    private static final AtomicInteger NEXT_ID = new AtomicInteger(
            ThreadLocalRandom.current().nextInt(1_000_000_000, Integer.MAX_VALUE / 2));

    private final int entityId = NEXT_ID.decrementAndGet();
    private final UUID entityUuid = UUID.randomUUID();
    private boolean spawned;
    private String lastText = "";

    @Override
    public void update(Player player, ArmorHudRender render) {
        Vector3d vec = new Vector3d(render.position().x(), render.position().y(), render.position().z());
        String text = buildText(render.snapshot(), render.text());

        if (!spawned) {
            spawn(player, vec, render.position().yaw(), render.position().pitch());
            sendMeta(player, text);
            spawned = true;
            lastText = text;
            return;
        }

        PacketEvents.getAPI().getPlayerManager().sendPacket(player,
                new WrapperPlayServerEntityTeleport(entityId, vec, render.position().yaw(), render.position().pitch(), false));
        if (!text.equals(lastText)) {
            sendMeta(player, text);
            lastText = text;
        }
    }

    @Override
    public void remove(Player player) {
        if (!spawned) {
            return;
        }
        PacketEvents.getAPI().getPlayerManager().sendPacket(player,
                new WrapperPlayServerDestroyEntities(entityId));
        spawned = false;
        lastText = "";
    }

    private void spawn(Player player, Vector3d pos, float yaw, float pitch) {
        WrapperPlayServerSpawnEntity spawn = new WrapperPlayServerSpawnEntity(
                entityId,
                Optional.of(entityUuid),
                EntityTypes.TEXT_DISPLAY,
                pos,
                pitch,
                yaw,
                yaw,
                0,
                Optional.empty()
        );
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, spawn);
    }

    private void sendMeta(Player player, String text) {
        List<EntityData> data = new ArrayList<>(16);
        data.add(new EntityData(META_NO_GRAVITY, EntityDataTypes.BOOLEAN, true));
        data.add(new EntityData(META_INTERPOLATION_DELAY, EntityDataTypes.INT, 0));
        data.add(new EntityData(META_INTERPOLATION_DURATION, EntityDataTypes.INT, 1));
        data.add(new EntityData(META_TELEPORT_DURATION, EntityDataTypes.INT, 1));
        data.add(new EntityData(META_TRANSLATION, EntityDataTypes.VECTOR3F, new Vector3f(0f, 0f, 0f)));
        data.add(new EntityData(META_SCALE, EntityDataTypes.VECTOR3F, new Vector3f(0.35f, 0.35f, 0.35f)));
        data.add(new EntityData(META_LEFT_ROT, EntityDataTypes.QUATERNION, new Quaternion4f(0f, 0f, 0f, 1f)));
        data.add(new EntityData(META_RIGHT_ROT, EntityDataTypes.QUATERNION, new Quaternion4f(0f, 0f, 0f, 1f)));
        data.add(new EntityData(META_BILLBOARD, EntityDataTypes.BYTE, BILLBOARD_CENTER));
        data.add(new EntityData(META_VIEW_RANGE, EntityDataTypes.FLOAT, 1.0f));
        data.add(new EntityData(META_WIDTH, EntityDataTypes.FLOAT, 1.5f));
        data.add(new EntityData(META_HEIGHT, EntityDataTypes.FLOAT, 1.0f));
        Component rendered = renderText(text);
        data.add(new EntityData(META_TEXT, EntityDataTypes.ADV_COMPONENT, rendered));
        data.add(new EntityData(META_LINE_WIDTH, EntityDataTypes.INT, 200));
        data.add(new EntityData(META_BG, EntityDataTypes.INT, 0x00000000));
        data.add(new EntityData(META_OPACITY, EntityDataTypes.BYTE, (byte) -1));
        data.add(new EntityData(META_STYLE, EntityDataTypes.BYTE, STYLE_SHADOW));
        PacketEvents.getAPI().getPlayerManager().sendPacket(player,
                new WrapperPlayServerEntityMetadata(entityId, data));
    }

    private static String buildText(ArmorSnapshot snapshot, String emptyLabel) {
        return icon(ArmorSlot.HELMET) + " " + snapshot.get(ArmorSlot.HELMET).formatLine(emptyLabel) + "\n"
                + icon(ArmorSlot.CHEST) + " " + snapshot.get(ArmorSlot.CHEST).formatLine(emptyLabel) + "\n"
                + icon(ArmorSlot.LEGS) + " " + snapshot.get(ArmorSlot.LEGS).formatLine(emptyLabel) + "\n"
                + icon(ArmorSlot.BOOTS) + " " + snapshot.get(ArmorSlot.BOOTS).formatLine(emptyLabel) + "\n"
                + snapshot.get(ArmorSlot.MAIN_HAND).formatLine(emptyLabel) + "\n"
                + snapshot.get(ArmorSlot.OFF_HAND).formatLine(emptyLabel);
    }

    private static String icon(ArmorSlot slot) {
        return switch (slot) {
            case HELMET -> "\uE100";
            case CHEST -> "\uE101";
            case LEGS -> "\uE102";
            case BOOTS -> "\uE103";
            case MAIN_HAND -> "M";
            case OFF_HAND -> "O";
        };
    }

    private static Component renderText(String text) {
        String[] lines = text.split("\\n", -1);
        Component rendered = Component.empty();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (!line.isEmpty()) {
                int codePoint = line.codePointAt(0);
                int iconLength = Character.charCount(codePoint);
                String icon = line.substring(0, iconLength);
                String body = line.substring(iconLength);
                boolean customIcon = codePoint >= 0xE100;
                rendered = rendered.append(Component.text(PANEL_LINE).font(ARMOR_FONT))
                        .append(Component.text(PANEL_BACKTRACK).font(ARMOR_FONT))
                        .append(Component.text(icon, NamedTextColor.WHITE).font(customIcon ? ARMOR_FONT : DEFAULT_FONT))
                        .append(Component.text(body, NamedTextColor.YELLOW).font(DEFAULT_FONT));
            }
            if (i + 1 < lines.length) {
                rendered = rendered.append(Component.newline());
            }
        }
        return rendered;
    }
}
