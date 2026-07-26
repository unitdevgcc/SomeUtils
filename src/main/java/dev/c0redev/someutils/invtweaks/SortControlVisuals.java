package dev.c0redev.someutils.invtweaks;

import dev.c0redev.someutils.SomeUtilsPlugin;
import dev.c0redev.someutils.lang.Language;
import dev.c0redev.someutils.lang.LanguageService;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemLore;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemModel;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;


final class SortControlVisuals {

    private static final int CONTROL_ROW = 9;

    private final Map<UUID, SortSession> sessions;
    private final LanguageService languageService;
    private PacketListenerAbstract listener;

    SortControlVisuals(SomeUtilsPlugin plugin, Map<UUID, SortSession> sessions) {
        this.sessions = sessions;
        this.languageService = plugin.getLanguageService();
    }

    void register() {
        if (listener != null) {
            return;
        }
        listener = new PacketListenerAbstract(PacketListenerPriority.NORMAL) {
            @Override
            public void onPacketSend(PacketSendEvent event) {
                UUID playerId = resolvePlayerId(event);
                if (playerId == null) {
                    return;
                }
                SortSession session = sessions.get(playerId);
                if (session == null) {
                    return;
                }
                if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
                    handleWindowItems(event, session);
                    event.markForReEncode(true);
                } else if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
                    handleSetSlot(event, session);
                    event.markForReEncode(true);
                }
            }
        };
        PacketEvents.getAPI().getEventManager().registerListener(listener);
    }

    void unregister() {
        if (listener == null) {
            return;
        }
        PacketEvents.getAPI().getEventManager().unregisterListener(listener);
        listener = null;
    }

    private static UUID resolvePlayerId(PacketSendEvent event) {
        Object packetPlayer = event.getPlayer();
        if (packetPlayer instanceof Player player) {
            return player.getUniqueId();
        }
        return event.getUser() != null ? event.getUser().getUUID() : null;
    }

    private void handleWindowItems(PacketSendEvent event, SortSession session) {
        WrapperPlayServerWindowItems packet = new WrapperPlayServerWindowItems(event);
        List<ItemStack> items = packet.getItems();
        if (!isControlLayout(items)) {
            return;
        }
        session.windowId = packet.getWindowId();
        Player player = resolvePlayer(event);
        for (int slot = 0; slot < CONTROL_ROW; slot++) {
            replaceVisual(items, slot, player);
        }
        packet.setItems(items);
    }

    private void handleSetSlot(PacketSendEvent event, SortSession session) {
        WrapperPlayServerSetSlot packet = new WrapperPlayServerSetSlot(event);
        if (packet.getWindowId() != session.windowId || packet.getSlot() < 0 || packet.getSlot() >= CONTROL_ROW) {
            return;
        }
        ItemStack visual = packet.getItem().copy();
        applyModel(visual, packet.getSlot(), resolvePlayer(event));
        packet.setItem(visual);
    }

    private static Player resolvePlayer(PacketSendEvent event) {
        Object packetPlayer = event.getPlayer();
        if (packetPlayer instanceof Player player) {
            return player;
        }
        return event.getUser() == null ? null : Bukkit.getPlayer(event.getUser().getUUID());
    }

    private static boolean isControlLayout(List<ItemStack> items) {
        if (items.size() < CONTROL_ROW) return false;
        return typeName(items.get(1)).equals("hopper")
                && typeName(items.get(2)).equals("compass")
                && typeName(items.get(3)).equals("iron_nugget")
                && typeName(items.get(8)).equals("barrier");
    }

    private static String typeName(ItemStack item) {
        return item.getType().getName().getKey();
    }

    private void replaceVisual(List<ItemStack> items, int slot, Player player) {
        ItemStack visual = items.get(slot).copy();
        applyModel(visual, slot, player);
        items.set(slot, visual);
    }

    private void applyModel(ItemStack item, int slot, Player player) {
        String model = switch (slot) {
        case 1 -> "sort";
        case 2 -> "columns";
        case 3 -> "stack";
        case 6 -> "previous";
        case 7 -> "next";
        case 8 -> "close";
        default -> "fill";
        };
        item.setComponent(ComponentTypes.ITEM_MODEL, new ItemModel(new ResourceLocation("someutils", "gui/" + model)));
        item.setComponent(ComponentTypes.ITEM_NAME, Component.text(buttonName(player, slot), NamedTextColor.WHITE));
        item.setComponent(ComponentTypes.LORE, new ItemLore(List.of(Component.text(buttonLore(player, slot), NamedTextColor.GRAY))));
    }

    private String buttonName(Player player, int slot) {
        String key = switch (slot) {
        case 1 -> "invtweaks.button.sort";
        case 2 -> "invtweaks.button.columns";
        case 3 -> "invtweaks.button.stack";
        case 6 -> "invtweaks.button.previous";
        case 7 -> "invtweaks.button.next";
        case 8 -> "invtweaks.button.close";
        default -> "invtweaks.button.default";
        };
        return player == null ? languageService.tr(Language.EN, key) : languageService.tr(player, key);
    }

    private String buttonLore(Player player, int slot) {
        String key = switch (slot) {
        case 1 -> "invtweaks.lore.sort";
        case 2 -> "invtweaks.lore.columns";
        case 3 -> "invtweaks.lore.stack";
        case 6, 7 -> "invtweaks.lore.page";
        case 8 -> "invtweaks.lore.close";
        default -> "invtweaks.lore.default";
        };
        return player == null ? languageService.tr(Language.EN, key) : languageService.tr(player, key);
    }

}
