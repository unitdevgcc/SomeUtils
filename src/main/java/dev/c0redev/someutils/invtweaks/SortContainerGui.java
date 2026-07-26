package dev.c0redev.someutils.invtweaks;

import dev.c0redev.someutils.SomeUtilsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.block.Barrel;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public final class SortContainerGui implements Listener {

    private final SomeUtilsPlugin plugin;
    private final Map<UUID, SortSession> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> opening = new ConcurrentHashMap<>();
    private final SortControlVisuals visuals = new SortControlVisuals(sessions);

    public SortContainerGui(SomeUtilsPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerPacketVisuals() {
        visuals.register();
    }

    public void unregisterPacketVisuals() {
        visuals.unregister();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        if (!plugin.getPluginConfig().isInvTweaksEnabled() || !plugin.getPluginConfig().isGuiControls()) {
            return;
        }
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (opening.putIfAbsent(player.getUniqueId(), true) != null) {
            return;
        }
        Inventory source = event.getInventory();

        if (source.getHolder() instanceof SortGuiHolder) {
            opening.remove(player.getUniqueId());
            return;
        }
        if (!isSupported(source)) {
            opening.remove(player.getUniqueId());
            return;
        }
        event.setCancelled(true);
        Bukkit.getScheduler().runTask(plugin, () -> {
            opening.remove(player.getUniqueId());
            open(player, new SortSession(source, plugin.getPluginConfig().isEvenPageMode()));
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof SortGuiHolder holder)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        SortSession session = holder.session;
        int raw = event.getRawSlot();
        if (raw < SortGuiRenderer.CONTROL_ROW) {
            event.setCancelled(true);
            handleControl(player, session, raw);
        } else if (raw < event.getInventory().getSize()) {

            session.markPagePending();
            Bukkit.getScheduler().runTask(plugin, () -> syncGuiToSource(player, session));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof SortGuiHolder holder)) {
            return;
        }
        boolean hitsControl = false;
        boolean hitsContent = false;
        for (int raw : event.getRawSlots()) {
            if (raw < SortGuiRenderer.CONTROL_ROW) hitsControl = true;
            else if (raw < event.getInventory().getSize()) hitsContent = true;
        }
        if (hitsControl) {
            event.setCancelled(true);
            return;
        }
        if (hitsContent) {
            Player player = (Player) event.getWhoClicked();
            holder.session.markPagePending();
            Bukkit.getScheduler().runTask(plugin, () -> syncGuiToSource(player, holder.session));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof SortGuiHolder holder)) {
            return;
        }

        if (!holder.session.switching) {
            SortGuiRenderer.savePage(event.getInventory(), holder.session);
            sessions.remove(event.getPlayer().getUniqueId());
            opening.remove(event.getPlayer().getUniqueId());
            syncSourceToAllViewers(holder.session.source);
        }
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHopperMove(InventoryMoveItemEvent event) {
        if (sessions.isEmpty()) {
            return;
        }
        Inventory source = event.getSource();
        Inventory destination = event.getDestination();

        Bukkit.getScheduler().runTask(plugin, () -> {
            syncSourceToAllViewers(source);
            syncSourceToAllViewers(destination);
        });
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(InventoryPickupItemEvent event) {
        if (sessions.isEmpty()) {
            return;
        }
        Inventory destination = event.getInventory();
        Bukkit.getScheduler().runTask(plugin, () -> syncSourceToAllViewers(destination));
    }

    private void handleControl(Player player, SortSession session, int slot) {

        SortGuiRenderer.savePage(player.getOpenInventory().getTopInventory(), session);
        switch (slot) {
        case 1 -> {
            InventorySorter.sortInventory(session.source, InventorySorter.SortMode.DEFAULT);
            reopen(player, session);
        }
        case 2 -> {
            InventorySorter.sortInventory(session.source, InventorySorter.SortMode.COLUMNS);
            reopen(player, session);
        }
        case 3 -> {
            InventorySorter.sortInventory(session.source, InventorySorter.SortMode.STACK_ONLY);
            reopen(player, session);
        }
        case 6 -> {
            if (session.page > 0) {
                session.page--;
                reopen(player, session);
            }
        }
        case 7 -> {
            if (session.page + 1 < session.pages()) {
                session.page++;
                reopen(player, session);
            }
        }
        case 8 -> player.closeInventory();
        default -> {
        }
        }
    }

    private void reopen(Player player, SortSession session) {
        session.switching = true;
        player.closeInventory();
        Bukkit.getScheduler().runTask(plugin, () -> {
            session.switching = false;
            open(player, session);
        });
    }

    private void open(Player player, SortSession session) {
        if (!player.isOnline()) {
            return;
        }
        sessions.put(player.getUniqueId(), session);
        SortGuiRenderer.open(player, session);
    }

    private void syncGuiToSource(Player player, SortSession session) {
        try {
            Inventory gui = player.getOpenInventory().getTopInventory();
            if (!(gui.getHolder() instanceof SortGuiHolder h) || h.session != session) return;
            SortGuiRenderer.savePage(gui, session);
        } finally {

            session.clearPagePending();
        }
        syncSourceToAllViewers(session.source);
    }

    private void syncSourceToAllViewers(Inventory source) {
        for (Map.Entry<UUID, SortSession> entry : sessions.entrySet()) {
            SortSession session = entry.getValue();
            if (!session.matches(source)) {
                continue;
            }
            Player viewer = Bukkit.getPlayer(entry.getKey());
            if (viewer == null || !viewer.isOnline()) {
                continue;
            }
            Inventory open = viewer.getOpenInventory().getTopInventory();
            if (!(open.getHolder() instanceof SortGuiHolder h) || h.session != session) {
                continue;
            }
            SortGuiRenderer.syncSourceToGui(session, open);
        }
    }

    private static boolean isSupported(Inventory inventory) {
        InventoryHolder holder = inventory.getHolder();
        return holder instanceof Chest
                || holder instanceof DoubleChest
                || holder instanceof Barrel
                || holder instanceof ShulkerBox
                || holder instanceof StorageMinecart;
    }
}
