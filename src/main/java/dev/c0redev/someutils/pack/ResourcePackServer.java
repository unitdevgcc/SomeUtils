package dev.c0redev.someutils.pack;

import com.sun.net.httpserver.HttpServer;
import dev.c0redev.someutils.SomeUtilsPlugin;
import dev.c0redev.someutils.armor.ArmorScoreboardGlyphs;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.Material;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ResourcePackServer implements Listener {

    private static final String PACK_PATH = "/someutils.zip";
    private static final int BLOCK_GLYPH_START = 0xE100;
    private static final int TILE_SIZE = 16;
    private static final int ICON_DRAW_SIZE = 12;
    private static final int BREAK_PROGRESS_START = 0xE011;
    private static final int BREAK_PROGRESS_WIDTH = 64;
    private static final int BREAK_PROGRESS_HEIGHT = 16;
    private static final int BREAK_PROGRESS_PADDING_X = 1;
    private static final int BREAK_PROGRESS_BAR_TOP = 2;
    private static final int BREAK_PROGRESS_BAR_HEIGHT = 12;
    private static final int BREAK_PROGRESS_FRAMES = 21;
    private static final Font BREAK_PROGRESS_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 7);
    private static final int ATLAS_COLUMNS = 32;
    private static final int MAX_PANEL_PARTS = 40;

    private static final int WAILA_MOVE_BASE = 0xF100;
    private static final int WAILA_TAIL_BASE = 0xF200;
    private static final int WAILA_CURSOR = 0xF0FF;
    private final SomeUtilsPlugin plugin;
    private HttpServer server;
    private File packFile;
    private byte[] packHash;
    private String packUrl;
    private boolean ready;
    private final Set<UUID> loadedPlayers = ConcurrentHashMap.newKeySet();
    private final Properties wailaTextureMap = new Properties();
    private final Properties wailaGlyphMap = new Properties();

    public ResourcePackServer(SomeUtilsPlugin plugin) {
        this.plugin = plugin;
        loadProps("waila-texture-map.properties", wailaTextureMap);
        loadProps("waila-glyph-map.properties", wailaGlyphMap);
    }

    public void start() {
        stop();

        try {
            buildPack();
            String configuredUrl = plugin.getPluginConfig().getResourcePackUrl().trim();
            if (configuredUrl.isEmpty()) {
                ready = false;
                plugin.getLogger().warning("Resource pack was built, but resource-pack.public-url is empty. "
                        + "Set a public HTTP URL before automatic delivery is enabled.");
                return;
            }

            int port = plugin.getPluginConfig().getResourcePackPort();
            server = HttpServer.create(new InetSocketAddress(plugin.getPluginConfig().getResourcePackBindAddress(), port), 0);
            server.createContext(PACK_PATH, exchange -> {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(405, -1);
                    exchange.close();
                    return;
                }

                exchange.getResponseHeaders().set("Content-Type", "application/zip");
                exchange.getResponseHeaders().set("Cache-Control", "no-store");
                exchange.sendResponseHeaders(200, packFile.length());
                try (InputStream input = new FileInputStream(packFile);
                     OutputStream output = exchange.getResponseBody()) {
                    input.transferTo(output);
                }
            });
            server.start();

            packUrl = configuredUrl;
            ready = true;
            Bukkit.getPluginManager().registerEvents(this, plugin);
            plugin.getLogger().info("Resource pack ready: " + packUrl);
            for (Player player : Bukkit.getOnlinePlayers()) {
                sendPack(player);
            }
        } catch (Exception e) {
            ready = false;
            plugin.getLogger().log(Level.WARNING, "Resource pack failed", e);
        }
    }

    public void stop() {
        HandlerList.unregisterAll(this);
        if (server != null) {
            server.stop(0);
            server = null;
        }
        ready = false;
        loadedPlayers.clear();
    }

    public boolean isReady() {
        return ready && packFile != null && packFile.exists() && packHash != null;
    }

    public boolean isLoaded(Player player) {
        return loadedPlayers.contains(player.getUniqueId());
    }

    public String getBlockGlyph(Material material) {
        String index = wailaGlyphMap.getProperty(material.getKey().getKey());
        if (index == null || index.isBlank()) {
            return "";
        }
        try {
            int ordinal = Integer.parseInt(index.trim());
            if (ordinal < 0) {
                return "";
            }
            return new String(Character.toChars(BLOCK_GLYPH_START + ordinal));
        } catch (NumberFormatException ignored) {
            return "";
        } catch (IllegalArgumentException ignored) {
            return "";
        }
    }

    public void sendPack(Player player) {
        if (!isReady()) {
            return;
        }

        UUID id = UUID.nameUUIDFromBytes(packHash);
        Component prompt = Component.text(plugin.getPluginConfig().getResourcePackPrompt());
        player.setResourcePack(id, packUrl, packHash, prompt, plugin.getPluginConfig().isResourcePackForce());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!isReady()) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> sendPack(event.getPlayer()), 20L);
    }

    @EventHandler
    public void onPackStatus(PlayerResourcePackStatusEvent event) {
        String name = event.getPlayer().getName();
        switch (event.getStatus()) {
        case ACCEPTED, DOWNLOADED ->
                plugin.getLogger().info("Resource pack " + event.getStatus() + " by " + name);
        case SUCCESSFULLY_LOADED -> {
            loadedPlayers.add(event.getPlayer().getUniqueId());
            plugin.getLogger().info("Resource pack loaded by " + name);
        }
        case DECLINED, FAILED_DOWNLOAD -> {
            loadedPlayers.remove(event.getPlayer().getUniqueId());
            plugin.getLogger().warning("Resource pack not available for " + name + ": " + event.getStatus());
        }
        default -> {}
        }
    }

    private void buildPack() throws Exception {
        File data = plugin.getDataFolder();
        if (!data.exists() && !data.mkdirs()) {
            throw new IllegalStateException("Cannot create plugin data directory");
        }

        packFile = new File(data, "SomeUtilsRP.zip");
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(packFile))) {
            write(zip, "pack.mcmeta", """
                    {
                      "pack": {
                      "pack_format": %d,
                      "min_format": %d,
                      "max_format": %d,
                        "description": "SomeUtils Jade HUD"
                      }
                    }
                    """.formatted(plugin.getPluginConfig().getResourcePackPackFormat(),
                    plugin.getPluginConfig().getResourcePackMinFormat(),
                    plugin.getPluginConfig().getResourcePackMaxFormat())
                    .getBytes(StandardCharsets.UTF_8));

            write(zip, "assets/someutils/font/jade.json", vanillaIconFont().getBytes(StandardCharsets.UTF_8));
            write(zip, "assets/someutils/font/waila.json", wailaFont().getBytes(StandardCharsets.UTF_8));
            write(zip, "assets/someutils/font/armor.json", armorFont().getBytes(StandardCharsets.UTF_8));
            write(zip, "assets/someutils/font/armor_scoreboard.json", armorScoreboardFont().getBytes(StandardCharsets.UTF_8));
            write(zip, "assets/someutils/textures/font/waila_start.png", wailaPartPng(2, true, false));
            write(zip, "assets/someutils/textures/font/waila_part.png", wailaPartPng(10, false, false));
            write(zip, "assets/someutils/textures/font/waila_end.png", wailaPartPng(2, false, true));
            byte[] renderedAtlas = readPluginResource("waila-rendered-atlas.png");
            write(zip, "assets/someutils/textures/font/block_atlas.png", renderedAtlas != null ? renderedAtlas : blockAtlasPng());
            write(zip, "assets/someutils/textures/font/item_atlas.png", itemAtlasPng());
            write(zip, "assets/someutils/textures/font/status_atlas.png", statusAtlasPng());
            write(zip, "assets/someutils/textures/font/crit_icon.png", critIconPng());
            write(zip, "assets/someutils/textures/font/break_progress.png", breakProgressAtlasPng());
            writeGuiModels(zip);
            byte[] transparentBossBar = transparentPng(182, 5);
            for (String sprite : new String[]{
                    "notched_6_background", "notched_6_progress", "white_background", "white_progress"
            }) {
                write(zip, "assets/minecraft/textures/gui/sprites/boss_bar/" + sprite + ".png", transparentBossBar);
            }

            if (plugin.getPluginConfig().isVanillaTextShader()) {
                write(zip, "assets/minecraft/shaders/core/rendertype_text.fsh", textShader(false));
                write(zip, "assets/minecraft/shaders/core/rendertype_text_see_through.fsh", textShader(true));
            }

            byte[] buttons = readPluginResource("assets/someutils/textures/gui/invtweaks_buttons.png");
            if (buttons != null) {
                write(zip, "assets/someutils/textures/gui/invtweaks_buttons.png", buttons);
            }
            writeArmorHudAssets(zip);
            writeArmorItemAssets(zip);
            writeArmorPercentAssets(zip);
        }

        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        try (InputStream input = new FileInputStream(packFile)) {
            packHash = sha1.digest(input.readAllBytes());
        }
    }

    private void writeArmorHudAssets(ZipOutputStream zip) throws Exception {
        String base = "assets/someutils/textures/armor_hud/";
        String[] names = {
                "panel.png", "side_rail.png", "bar_background.png",
                "slot_helmet.png", "slot_chestplate.png", "slot_leggings.png", "slot_boots.png",
                "empty_helmet.png", "empty_chestplate.png", "empty_leggings.png", "empty_boots.png",
                "armor_title.png"
        };
        for (String name : names) {
            byte[] asset = readPluginResource(base + name);
            if (asset != null) {
                write(zip, base + name, asset);
            }
        }
        for (int frame = 0; frame <= ArmorScoreboardGlyphs.BAR_FRAMES; frame++) {
            writePluginAsset(zip, base, "bar_fill_" + frame + ".png");
        }
        for (int frame = 0; frame < ArmorScoreboardGlyphs.ANIMATION_FRAMES; frame++) {
            write(zip, base + "frame_top_" + frame + ".png", png(borderFrame("top", frame)));
            write(zip, base + "frame_rail_" + frame + ".png", png(borderFrame("rail", frame)));
            write(zip, base + "frame_bottom_" + frame + ".png", png(borderFrame("bottom", frame)));
        }
    }

    private BufferedImage borderFrame(String kind, int phase) {
        BufferedImage image = new BufferedImage(96, 18, BufferedImage.TYPE_INT_ARGB);
        Color accent = parseColor(plugin.getPluginConfig().getArmorHudAccent(), new Color(132, 187, 99));
        Color primary = parseColor(plugin.getPluginConfig().getArmorHudPrimary(), new Color(175, 192, 122));
        Color secondary = parseColor(plugin.getPluginConfig().getArmorHudSecondary(), new Color(50, 67, 50));
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                boolean edge = (kind.equals("top") && y == 17 && x >= 2 && x < 94)
                        || (kind.equals("bottom") && y == 0 && x >= 2 && x < 94)
                        || (kind.equals("rail") && (x == 0 || x == 95));
                boolean rounded = (kind.equals("top") && y == 16 && (x == 1 || x == 94))
                        || (kind.equals("bottom") && y == 1 && (x == 1 || x == 94));
                if (!edge && !rounded) continue;
                int position = kind.equals("rail") ? y : x;
                double wave = 0.5 + 0.5 * Math.sin((position - phase * 3) * Math.PI / 24.0);
                Color base = mix(primary, secondary, wave);
                double pulse = Math.max(0.0, 1.0 - Math.abs(((position - phase * 3) % 32 + 32) % 32 - 16) / 16.0);
                image.setRGB(x, y, mix(base, accent, pulse * 0.72).getRGB());
            }
        }
        return image;
    }

    private static Color mix(Color a, Color b, double amount) {
        amount = Math.max(0.0, Math.min(1.0, amount));
        return new Color(
                (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * amount),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * amount),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * amount),
                255);
    }

    private static Color parseColor(String value, Color fallback) {
        try {
            String hex = value == null ? "" : value.trim();
            if (hex.startsWith("#")) hex = hex.substring(1);
            if (hex.length() != 6) return fallback;
            return new Color(Integer.parseInt(hex, 16));
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private void writePluginAsset(ZipOutputStream zip, String base, String name) throws Exception {
        byte[] asset = readPluginResource(base + name);
        if (asset != null) {
            write(zip, base + name, asset);
        }
    }

    private void writeArmorItemAssets(ZipOutputStream zip) throws Exception {
        BufferedImage barTrack = barTrackImage();
        for (String material : ArmorScoreboardGlyphs.materials()) {
            BufferedImage item = readImage("vanilla-textures/item/" + material + ".png");
            if (item != null) {
                for (int frame = 0; frame <= ArmorScoreboardGlyphs.BAR_FRAMES; frame++) {
                    write(zip, "assets/someutils/textures/armor_hud/durability_" + material + "_" + frame + ".png",
                            png(compositeDurability(item, barTrack, frame)));
                }
            }
        }
        for (String slot : new String[]{"helmet", "chestplate", "leggings", "boots"}) {
            BufferedImage empty = missingSlotImage(slot);
            write(zip, "assets/someutils/textures/armor_hud/missing_" + slot + ".png",
                    png(empty));
            write(zip, "assets/someutils/textures/armor_hud/broken_" + slot + ".png",
                    png(tintBroken(empty)));
        }
        BufferedImage off = new BufferedImage(16, 19, BufferedImage.TYPE_INT_ARGB);
        Graphics2D og = off.createGraphics();
        og.setColor(new Color(180, 180, 190, 255));
        og.fillRect(4, 3, 8, 10);
        og.setColor(new Color(90, 90, 100, 255));
        og.drawRect(4, 3, 7, 9);
        og.drawImage(barTrack, 0, 16, null);
        og.dispose();
        write(zip, "assets/someutils/textures/armor_hud/offhand.png", png(off));
        for (int frame = 0; frame < ArmorScoreboardGlyphs.CRACK_FRAMES; frame++) {
            write(zip, "assets/someutils/textures/armor_hud/crack_" + frame + ".png", png(crackOverlay(frame)));
        }
    }

    private BufferedImage crackOverlay(int frame) {
        BufferedImage image = new BufferedImage(16, 19, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        int alpha = 120 + Math.abs(3 - frame) * 18;
        g.setColor(new Color(255, 224, 160, Math.min(255, alpha)));
        int[][] segments = {
                {8, 2, 7, 5}, {7, 5, 9, 7}, {9, 7, 7, 10},
                {7, 10, 5, 12}, {9, 7, 12, 9}, {5, 12, 4, 14},
                {12, 9, 13, 12}
        };
        int visible = Math.min(segments.length, 2 + frame);
        for (int i = 0; i < visible; i++) {
            int[] line = segments[i];
            g.drawLine(line[0], line[1], line[2], line[3]);
        }
        if (frame == 3 || frame == 7) {
            g.setColor(new Color(255, 255, 225, 230));
            g.fillRect(frame == 3 ? 11 : 4, frame == 3 ? 4 : 7, 1, 1);
        }
        g.dispose();
        image.setRGB(15, 18, new Color(0, 0, 0, 1).getRGB());
        return image;
    }

    private void writeArmorPercentAssets(ZipOutputStream zip) throws Exception {
        String base = "assets/someutils/textures/armor_hud/";
        String[] percentSlots = {"helmet", "chestplate", "leggings", "boots", "offhand"};
        String[] states = {"healthy", "warn", "critical"};
        for (int slot = 0; slot < percentSlots.length; slot++) {
            for (int state = 0; state < states.length; state++) {
                for (int frame = 0; frame < ArmorScoreboardGlyphs.ANIMATION_FRAMES; frame++) {
                    write(zip, base + "percent_" + percentSlots[slot] + "_" + states[state] + "_" + frame + ".png",
                            png(percentBackground(percentSlots[slot], state, frame)));
                }
            }
        }
        for (int percent = 0; percent <= 100; percent++) {
            write(zip, base + "percent_text_" + percent + ".png", png(percentTextGlyph(percent)));
        }
    }

    private BufferedImage percentBackground(String slot, int state, int phase) {
        BufferedImage image = new BufferedImage(32, 18, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        Color base = switch (slot) {
            case "helmet" -> new Color(75, 155, 205, 255);
            case "chestplate" -> new Color(83, 173, 111, 255);
            case "leggings" -> new Color(111, 102, 194, 255);
            case "boots" -> new Color(190, 126, 65, 255);
            case "offhand" -> new Color(154, 103, 190, 255);
            default -> parseColor(plugin.getPluginConfig().getArmorHudAccent(), new Color(132, 187, 99));
        };
        Color stateColor = switch (state) {
            case 2 -> new Color(220, 68, 62, 255);
            case 1 -> new Color(219, 164, 62, 255);
            default -> base;
        };
        double wave = state == 0 ? 0.12 : 0.28 + 0.25 * (0.5 + 0.5 * Math.sin((phase - 4) * Math.PI / 16.0));
        Color fill = mix(stateColor, new Color(10, 16, 20, 255), 0.48);
        Color glow = mix(stateColor, Color.WHITE, wave * 0.35);
        g.setColor(new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 220));
        g.fillRoundRect(1, 2, 30, 14, 4, 4);
        g.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 220));
        g.drawRoundRect(1, 2, 30, 14, 4, 4);
        g.setColor(new Color(255, 255, 255, state == 2 ? (int) (35 + wave * 80) : 28));
        g.fillRect(4 + Math.floorMod(phase, 24), 4, 2, 1);
        g.dispose();
        return image;
    }

    private void drawPixelText(Graphics2D g, String text, int x, int y, Color color) {
        int xOffset = 0;
        for (char c : text.toCharArray()) {
            drawPixelChar(g, c, x + xOffset, y, color);
            xOffset += c == '%' ? 5 : 4;
        }
    }

    private void drawPixelChar(Graphics2D g, char c, int x, int y, Color color) {
        g.setColor(color);
        int[][] pixels = switch (c) {
            case '0' -> new int[][]{{1,1,1},{1,0,1},{1,0,1},{1,0,1},{1,1,1}};
            case '1' -> new int[][]{{0,1,0},{1,1,0},{0,1,0},{0,1,0},{1,1,1}};
            case '2' -> new int[][]{{1,1,1},{0,0,1},{1,1,1},{1,0,0},{1,1,1}};
            case '3' -> new int[][]{{1,1,1},{0,0,1},{0,1,1},{0,0,1},{1,1,1}};
            case '4' -> new int[][]{{1,0,1},{1,0,1},{1,1,1},{0,0,1},{0,0,1}};
            case '5' -> new int[][]{{1,1,1},{1,0,0},{1,1,1},{0,0,1},{1,1,1}};
            case '6' -> new int[][]{{1,1,1},{1,0,0},{1,1,1},{1,0,1},{1,1,1}};
            case '7' -> new int[][]{{1,1,1},{0,0,1},{0,0,1},{0,1,0},{0,1,0}};
            case '8' -> new int[][]{{1,1,1},{1,0,1},{1,1,1},{1,0,1},{1,1,1}};
            case '9' -> new int[][]{{1,1,1},{1,0,1},{1,1,1},{0,0,1},{1,1,1}};
            case '%' -> new int[][]{{1,0,1},{0,0,1},{0,1,0},{1,0,0},{1,0,1}};
            default -> new int[][]{{0,0,0},{0,0,0},{0,0,0},{0,0,0},{0,0,0}};
        };
        for (int row = 0; row < pixels.length; row++) {
            for (int col = 0; col < pixels[row].length; col++) {
                if (pixels[row][col] == 1) {
                    g.fillRect(x + col, y + row, 1, 1);
                }
            }
        }
    }

    private BufferedImage percentTextGlyph(int percent) {
        BufferedImage image = new BufferedImage(32, 14, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        String text = Math.clamp(percent, 0, 100) + "%";
        int width = 0;
        for (char c : text.toCharArray()) width += c == '%' ? 5 : 4;
        width--;
        drawPixelText(g, text, (32 - width) / 2, 7, new Color(235, 255, 235, 255));
        g.dispose();
        return image;
    }

    private BufferedImage missingSlotImage(String slot) throws Exception {
        BufferedImage source = readImage("vanilla-textures/item/iron_" + slot + ".png");
        BufferedImage image = new BufferedImage(16, 19, BufferedImage.TYPE_INT_ARGB);
        if (source == null) return image;
        for (int y = 0; y < Math.min(16, source.getHeight()); y++) {
            for (int x = 0; x < Math.min(16, source.getWidth()); x++) {
                Color pixel = new Color(source.getRGB(x, y), true);
                if (pixel.getAlpha() == 0) continue;
                int shade = (pixel.getRed() * 3 + pixel.getGreen() * 5 + pixel.getBlue() * 2) / 10;
                int value = 42 + shade * 42 / 255;
                image.setRGB(x, y, new Color(value, value + 7, value + 9,
                        Math.max(55, pixel.getAlpha() * 3 / 5)).getRGB());
            }
        }
        return image;
    }

    private static BufferedImage tintBroken(BufferedImage src) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int argb = src.getRGB(x, y);
                int a = (argb >>> 24) & 0xFF;
                if (a == 0) {
                    continue;
                }
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                int nr = Math.min(255, (int) (r * 0.35 + 180));
                int ng = Math.min(255, (int) (g * 0.25));
                int nb = Math.min(255, (int) (b * 0.25));
                if (Math.abs(x - y) <= 1 && y < 16) {
                    nr = 40; ng = 10; nb = 10; a = 220;
                }
                out.setRGB(x, y, (a << 24) | (nr << 16) | (ng << 8) | nb);
            }
        }
        return out;
    }

    // один glyph сохраняет позицию иконки и полосы в scoreboard
    private static BufferedImage compositeIconWithBar(BufferedImage icon, BufferedImage barTrack) {
        BufferedImage out = new BufferedImage(16, 19, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(icon, 0, 0, 16, 16, 0, 0,
                Math.min(16, icon.getWidth()), Math.min(16, icon.getHeight()), null);
        g.drawImage(barTrack, 0, 16, null);
        g.dispose();
        return out;
    }

    private static BufferedImage compositeDurability(BufferedImage icon, BufferedImage barTrack, int frame) {
        BufferedImage out = compositeIconWithBar(icon, barTrack);
        Graphics2D g = out.createGraphics();
        int filled = Math.round(14.0f * frame / ArmorScoreboardGlyphs.BAR_FRAMES);
        g.setColor(Color.WHITE);
        g.fillRect(1, 17, filled, 1);
        g.dispose();
        return out;
    }

    private static BufferedImage barTrackImage() {
        BufferedImage bar = new BufferedImage(16, 3, BufferedImage.TYPE_INT_ARGB);
        int accent = 0xFF84BB63;
        int trough = 0xFF0F141C;
        for (int x = 1; x < 15; x++) {
            bar.setRGB(x, 0, accent);
            bar.setRGB(x, 1, trough);
            bar.setRGB(x, 2, accent);
        }
        return bar;
    }

    private String armorFont() {
        return """
                {
                  "providers": [
                    {"type":"bitmap","file":"someutils:armor_hud/slot_helmet.png","ascent":8,"height":16,"chars":["\\uE100"]},
                    {"type":"bitmap","file":"someutils:armor_hud/slot_chestplate.png","ascent":8,"height":16,"chars":["\\uE101"]},
                    {"type":"bitmap","file":"someutils:armor_hud/slot_leggings.png","ascent":8,"height":16,"chars":["\\uE102"]},
                    {"type":"bitmap","file":"someutils:armor_hud/slot_boots.png","ascent":8,"height":16,"chars":["\\uE103"]},
                    {"type":"space","advances":{"\\uE111":-129}}
                  ]
                }
                """;
    }

    private String armorScoreboardFont() {
        StringBuilder providers = new StringBuilder();

        for (String material : ArmorScoreboardGlyphs.materials()) {
            int idx = ArmorScoreboardGlyphs.materials().indexOf(material);
            for (int frame = 0; frame <= ArmorScoreboardGlyphs.BAR_FRAMES; frame++) {
                appendProvider(providers, "someutils:armor_hud/durability_" + material + "_" + frame + ".png", 8, 19,
                        ArmorScoreboardGlyphs.ICON_WITH_BAR_BASE
                                + idx * (ArmorScoreboardGlyphs.BAR_FRAMES + 1) + frame);
            }
        }

        String[] slots = {"helmet", "chestplate", "leggings", "boots"};
        for (int i = 0; i < slots.length; i++) {
            appendProvider(providers, "someutils:armor_hud/missing_" + slots[i] + ".png", 8, 19,
                    ArmorScoreboardGlyphs.MISSING_BASE + i);
            appendProvider(providers, "someutils:armor_hud/broken_" + slots[i] + ".png", 8, 19,
                    ArmorScoreboardGlyphs.BROKEN_BASE + i);
        }
        appendProvider(providers, "someutils:armor_hud/offhand.png", 8, 19, ArmorScoreboardGlyphs.OFFHAND_ICON);
        for (int frame = 0; frame < ArmorScoreboardGlyphs.CRACK_FRAMES; frame++) {
            appendProvider(providers, "someutils:armor_hud/crack_" + frame + ".png", 8, 19,
                    ArmorScoreboardGlyphs.CRACK_BASE + frame);
        }

        String[] percentSlots = {"helmet", "chestplate", "leggings", "boots", "offhand"};
        String[] states = {"healthy", "warn", "critical"};
        for (int slot = 0; slot < percentSlots.length; slot++) {
            for (int state = 0; state < states.length; state++) {
                for (int frame = 0; frame < ArmorScoreboardGlyphs.ANIMATION_FRAMES; frame++) {
                    appendProvider(providers, "someutils:armor_hud/percent_" + percentSlots[slot] + "_"
                                    + states[state] + "_" + frame + ".png", 8, 19,
                            ArmorScoreboardGlyphs.PERCENT_BACKGROUND_BASE
                                    + slot * 3 * ArmorScoreboardGlyphs.ANIMATION_FRAMES
                                    + state * ArmorScoreboardGlyphs.ANIMATION_FRAMES + frame);
                }
            }
        }
        for (int percent = 0; percent <= 100; percent++) {
            appendProvider(providers, "someutils:armor_hud/percent_text_" + percent + ".png", 8, 14,
                    ArmorScoreboardGlyphs.PERCENT_TEXT_BASE + percent);
        }

        for (int frame = 0; frame < ArmorScoreboardGlyphs.ANIMATION_FRAMES; frame++) {
            appendProvider(providers, "someutils:armor_hud/frame_top_" + frame + ".png", 14, 18,
                    ArmorScoreboardGlyphs.FRAME_TOP_BASE + frame);
            appendProvider(providers, "someutils:armor_hud/frame_rail_" + frame + ".png", 14, 18,
                    ArmorScoreboardGlyphs.FRAME_RAIL_BASE + frame);
            appendProvider(providers, "someutils:armor_hud/frame_bottom_" + frame + ".png", 14, 18,
                    ArmorScoreboardGlyphs.FRAME_BOTTOM_BASE + frame);
        }
        appendProvider(providers, "someutils:armor_hud/armor_title.png", 7, 7,
                ArmorScoreboardGlyphs.TITLE_BASE);
        providers.append(',').append("{\"type\":\"space\",\"advances\":{\"")
                .append(Character.toChars(ArmorScoreboardGlyphs.ICON_BACKTRACK)).append("\":-72,\"")
                .append(Character.toChars(ArmorScoreboardGlyphs.PERCENT_TEXT_BACKTRACK)).append("\":-32,\"")
                .append(Character.toChars(ArmorScoreboardGlyphs.CRACK_BACKTRACK)).append("\":-17}}");

        return "{\"providers\":[" + providers + "]}";
    }

    private void appendProvider(StringBuilder providers, String file, int ascent, int height, int codePoint) {
        if (!providers.isEmpty()) {
            providers.append(',');
        }
        providers.append("{\"type\":\"bitmap\",\"file\":\"").append(file)
                .append("\",\"ascent\":").append(ascent)
                .append(",\"height\":").append(height)
                .append(",\"chars\":[\"").append(Character.toChars(codePoint)).append("\"]}");
    }

    private String vanillaIconFont() {
        return """
                {
                  "providers": [
                    {"type":"bitmap","file":"someutils:font/status_atlas.png","ascent":10,"height":16,"chars":["\\uE008\\uE009\\uE00E\\uE00F\\uE010"]},
                    {"type":"bitmap","file":"someutils:font/crit_icon.png","ascent":8,"height":10,"chars":["\\uE030"]},
                    {"type":"bitmap","file":"someutils:font/item_atlas.png","ascent":10,"height":16,"chars":["\\uE00A\\uE00B\\uE00C\\uE00D"]},
                    {"type":"bitmap","file":"someutils:font/break_progress.png","ascent":10,"height":16,"chars":[%s]},
                    {"type":"bitmap","file":"someutils:font/block_atlas.png","ascent":10,"height":16,"chars":%s}
                  ]
                }
                """.formatted(breakProgressGlyphRow(), blockGlyphRows());
    }

    private String breakProgressGlyphRow() {
        StringBuilder row = new StringBuilder("\"");
        for (int frame = 0; frame < BREAK_PROGRESS_FRAMES; frame++) {
            row.append(Character.toChars(BREAK_PROGRESS_START + frame));
        }
        return row.append('"').toString();
    }

    private String wailaFont() {
        StringBuilder moves = new StringBuilder();
        for (int parts = 0; parts <= MAX_PANEL_PARTS; parts++) {
            if (parts > 0) moves.append(',');
            String glyph = new String(Character.toChars(WAILA_MOVE_BASE + parts));
            int advance = -(5 + parts * 10) + 4;
            moves.append('"').append(glyph).append("\":").append(advance);
        }
        for (int tail = 0; tail <= 16; tail++) {
            moves.append(',');
            String glyph = new String(Character.toChars(WAILA_TAIL_BASE + tail));
            moves.append('"').append(glyph).append("\":").append(tail);
        }
        String cursor = new String(Character.toChars(WAILA_CURSOR));
        return """
                {
                  "providers": [
                    {"type":"bitmap","file":"someutils:font/waila_start.png","ascent":12,"height":20,"chars":["\\uF000"]},
                    {"type":"bitmap","file":"someutils:font/waila_part.png","ascent":12,"height":20,"chars":["\\uF001"]},
                    {"type":"bitmap","file":"someutils:font/waila_end.png","ascent":12,"height":20,"chars":["\\uF002"]},
                    {"type":"space","advances":{"%s":-1,%s}}
                  ]
                }
                """.formatted(cursor, moves);
    }

    private byte[] wailaPartPng(int width, boolean left, boolean right) throws Exception {
        BufferedImage image = new BufferedImage(width, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        Color accent = parseColor(plugin.getPluginConfig().getArmorHudAccent(), new Color(132, 187, 99));
        Color primary = parseColor(plugin.getPluginConfig().getArmorHudPrimary(), new Color(175, 192, 122));
        Color secondary = parseColor(plugin.getPluginConfig().getArmorHudSecondary(), new Color(50, 67, 50));
        Color border = mix(secondary, primary, 0.32);
        Color fillBase = mix(secondary, Color.BLACK, 0.58);
        Color fill = new Color(fillBase.getRed(), fillBase.getGreen(), fillBase.getBlue(), 242);
        Color shadow = new Color(0, 0, 0, 155);
        Color accentDim = mix(secondary, accent, 0.42);

        graphics.setColor(shadow);
        graphics.fillRect(0, 4, width, 16);
        graphics.setColor(border);
        graphics.fillRect(0, 0, width, 17);
        graphics.setColor(fill);
        graphics.fillRect(0, 1, width, 15);
        Color highlight = mix(primary, accent, 0.35);
        graphics.setColor(new Color(highlight.getRed(), highlight.getGreen(), highlight.getBlue(), 140));
        graphics.fillRect(0, 1, width, 1);
        graphics.setColor(border.darker());
        graphics.fillRect(0, 15, width, 1);
        if (left) {
            graphics.setColor(accentDim);
            graphics.fillRect(0, 1, 2, 15);
            graphics.setColor(accent);
            graphics.fillRect(0, 2, 1, 13);
        }
        if (right) {
            graphics.setColor(border);
            graphics.fillRect(width - 1, 1, 1, 15);
        }
        graphics.dispose();
        return png(image);
    }

    private String blockGlyphRows() {
        StringBuilder rows = new StringBuilder("[");
        int count = wailaGlyphMap.size();
        int rowCount = (count + ATLAS_COLUMNS - 1) / ATLAS_COLUMNS;
        for (int row = 0; row < rowCount; row++) {
            if (row > 0) rows.append(',');
            rows.append('"');
            for (int column = 0; column < ATLAS_COLUMNS; column++) {
                int ordinal = row * ATLAS_COLUMNS + column;

                rows.appendCodePoint(ordinal < count ? BLOCK_GLYPH_START + ordinal : 0xF000 + ordinal);
            }
            rows.append('"');
        }
        return rows.append(']').toString();
    }

    private byte[] blockAtlasPng() throws Exception {
        Material[] materials = Material.values();
        int rows = (materials.length + ATLAS_COLUMNS - 1) / ATLAS_COLUMNS;
        BufferedImage atlas = new BufferedImage(ATLAS_COLUMNS * TILE_SIZE, rows * TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = atlas.createGraphics();
        for (Material material : materials) {
            if (!material.isBlock()) continue;
            BufferedImage tile = readImage("vanilla-textures/" + blockTexture(material) + ".png");
            if (tile == null) tile = readImage("vanilla-textures/block/stone.png");
            drawTile(graphics, tile, material.ordinal() % ATLAS_COLUMNS, material.ordinal() / ATLAS_COLUMNS);
        }
        graphics.dispose();
        return png(atlas);
    }

    private byte[] itemAtlasPng() throws Exception {
        String[] items = {"diamond_pickaxe", "diamond_axe", "diamond_shovel", "shears"};
        BufferedImage atlas = new BufferedImage(items.length * TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = atlas.createGraphics();
        for (int i = 0; i < items.length; i++) {
            drawTile(graphics, readImage("vanilla-textures/item/" + items[i] + ".png"), i, 0);
        }
        graphics.dispose();
        return png(atlas);
    }

    private byte[] statusAtlasPng() throws Exception {
        BufferedImage atlas = new BufferedImage(TILE_SIZE * 5, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = atlas.createGraphics();
        drawPlayerIcon(g, 0);
        drawHeartIcon(g, TILE_SIZE);
        drawPowerIcon(g, TILE_SIZE * 2, new Color(117, 210, 104, 255));
        drawPowerIcon(g, TILE_SIZE * 3, new Color(222, 83, 76, 255));
        drawChestIcon(g, TILE_SIZE * 4);
        g.dispose();
        return png(atlas);
    }

    private byte[] breakProgressAtlasPng() throws Exception {
        BufferedImage atlas = new BufferedImage(BREAK_PROGRESS_WIDTH * BREAK_PROGRESS_FRAMES, BREAK_PROGRESS_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = atlas.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int trackWidth = BREAK_PROGRESS_WIDTH - BREAK_PROGRESS_PADDING_X * 2;
        int fillWidth = trackWidth - 2;
        int bottom = BREAK_PROGRESS_BAR_TOP + BREAK_PROGRESS_BAR_HEIGHT - 1;
        for (int frame = 0; frame < BREAK_PROGRESS_FRAMES; frame++) {
            int x = frame * BREAK_PROGRESS_WIDTH + BREAK_PROGRESS_PADDING_X;
            float ratio = frame / (float) (BREAK_PROGRESS_FRAMES - 1);
            int filled = Math.round(ratio * fillWidth);

            g.setColor(new Color(0, 0, 0, 90));
            g.fillRect(x + 1, bottom + 1, trackWidth - 1, 1);
            g.setColor(new Color(12, 17, 18, 240));
            g.fillRect(x + 1, BREAK_PROGRESS_BAR_TOP + 1, trackWidth - 2, BREAK_PROGRESS_BAR_HEIGHT - 2);

            if (filled > 0) {
                Color head = gradient(ratio);
                g.setColor(head.darker());
                g.fillRect(x + 1, BREAK_PROGRESS_BAR_TOP + 1, filled, BREAK_PROGRESS_BAR_HEIGHT - 2);
                g.setColor(head);
                g.fillRect(x + 1, BREAK_PROGRESS_BAR_TOP + 1, filled, BREAK_PROGRESS_BAR_HEIGHT - 4);
                g.setColor(new Color(255, 255, 255, 60));
                g.fillRect(x + 1, BREAK_PROGRESS_BAR_TOP + 1, filled, 1);
                if (filled < fillWidth) {
                    g.setColor(new Color(255, 255, 255, 150));
                    g.fillRect(x + filled, BREAK_PROGRESS_BAR_TOP + 1, 1, BREAK_PROGRESS_BAR_HEIGHT - 2);
                }
            }

            g.setColor(new Color(78, 100, 92, 255));
            g.drawRect(x, BREAK_PROGRESS_BAR_TOP, trackWidth - 1, BREAK_PROGRESS_BAR_HEIGHT - 1);

            String percent = Math.round(ratio * 100.0f) + "%";
            g.setFont(BREAK_PROGRESS_FONT);
            FontMetrics fm = g.getFontMetrics();
            int textX = x + (trackWidth - fm.stringWidth(percent)) / 2;
            int textY = BREAK_PROGRESS_BAR_TOP + (BREAK_PROGRESS_BAR_HEIGHT + fm.getAscent() - fm.getDescent()) / 2 - 1;
            g.setColor(new Color(0, 0, 0, 200));
            g.drawString(percent, textX + 1, textY + 1);
            g.setColor(Color.WHITE);
            g.drawString(percent, textX, textY);
        }
        g.dispose();
        return png(atlas);
    }

    private static Color gradient(float ratio) {
        Color low = new Color(203, 78, 62);
        Color mid = new Color(214, 168, 65);
        Color high = new Color(132, 187, 99);
        return ratio < 0.5f
                ? lerp(low, mid, ratio / 0.5f)
                : lerp(mid, high, (ratio - 0.5f) / 0.5f);
    }

    private static Color lerp(Color from, Color to, float t) {
        return new Color(
                Math.round(from.getRed() + (to.getRed() - from.getRed()) * t),
                Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * t),
                Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * t));
    }

    private byte[] critIconPng() throws Exception {
        BufferedImage icon = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = icon.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int[] xs = {6, 3, 5, 2, 8, 5, 7};
        int[] ys = {0, 5, 5, 10, 4, 4, 0};
        g.setColor(new Color(255, 210, 60, 255));
        g.fillPolygon(xs, ys, xs.length);
        g.setColor(new Color(255, 255, 220, 255));
        g.drawPolygon(xs, ys, xs.length);
        g.dispose();
        return png(icon);
    }

    private static void drawPlayerIcon(Graphics2D g, int x) {
        Color dark = new Color(17, 24, 25, 255);
        Color pale = new Color(222, 236, 216, 255);
        g.setColor(pale);
        g.fillRect(x + 6, 2, 4, 4);
        g.fillRect(x + 4, 7, 8, 5);
        g.fillRect(x + 3, 12, 3, 2);
        g.fillRect(x + 10, 12, 3, 2);
        g.setColor(dark);
        g.fillRect(x + 7, 3, 1, 1);
    }

    private static void drawHeartIcon(Graphics2D g, int x) {
        Color red = new Color(222, 83, 76, 255);
        g.setColor(red);
        g.fillRect(x + 3, 4, 4, 4);
        g.fillRect(x + 9, 4, 4, 4);
        g.fillRect(x + 5, 6, 6, 6);
        g.fillRect(x + 7, 12, 2, 2);
    }

    private static void drawPowerIcon(Graphics2D g, int x, Color color) {
        g.setColor(color);
        g.fillRect(x + 6, 2, 4, 7);
        g.fillRect(x + 3, 5, 2, 6);
        g.fillRect(x + 11, 5, 2, 6);
        g.fillRect(x + 5, 11, 6, 2);
    }

    private static void drawChestIcon(Graphics2D g, int x) {

        g.setColor(new Color(148, 101, 45, 255));
        g.fillRect(x + 2, 5, 12, 8);
        g.setColor(new Color(192, 144, 75, 255));
        g.fillRect(x + 2, 3, 12, 3);
        g.setColor(new Color(66, 45, 25, 255));
        g.fillRect(x + 2, 6, 12, 1);
        g.setColor(new Color(240, 202, 81, 255));
        g.fillRect(x + 7, 7, 2, 3);
    }

    private void writeGuiModels(ZipOutputStream zip) throws Exception {
        String[] names = {
            "sort", "columns", "stack", "previous", "next", "close", "fill",
                "lang_ru", "lang_en", "lang_auto", "jade_on", "jade_off", "armor_on", "armor_off", "armor_border_accent", "armor_border_primary", "armor_border_secondary", "logo", "divider",
            "frame", "panel",
        };
        for (String name : names) {
            write(zip, "assets/someutils/items/gui/" + name + ".json", """
                    {"model":{"type":"minecraft:model","model":"someutils:gui/%s"}}
                    """.formatted(name).getBytes(StandardCharsets.UTF_8));
            write(zip, "assets/someutils/models/gui/" + name + ".json", """
                    {"parent":"minecraft:item/generated","textures":{"layer0":"someutils:item/gui/%s"}}
                    """.formatted(name).getBytes(StandardCharsets.UTF_8));
            byte[] icon = guiIconPng(name);
            write(zip, "assets/someutils/textures/gui/" + name + ".png", icon);
            write(zip, "assets/someutils/textures/item/gui/" + name + ".png", icon);
        }
    }

    private byte[] guiIconPng(String name) throws Exception {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        Color outline = new Color(17, 24, 25, 255);
        Color panel = new Color(42, 54, 51, 255);
        Color accent = new Color(132, 187, 99, 255);
        Color muted = new Color(174, 192, 181, 255);
        boolean framed = !name.equals("divider") && !name.equals("frame") && !name.equals("panel");
        if (framed) {
            g.setColor(outline);
            g.fillRect(1, 1, 14, 14);
            g.setColor(panel);
            g.fillRect(2, 2, 12, 12);
        }
        g.setColor(accent);

        switch (name) {
        case "sort" -> {
            g.fillRect(4, 4, 7, 1);
            g.fillRect(4, 7, 5, 1);
            g.fillRect(4, 10, 3, 1);
            g.fillRect(10, 9, 1, 3);
            g.fillRect(9, 11, 3, 1);
        }
        case "columns" -> {
            g.fillRect(4, 4, 2, 8);
            g.fillRect(8, 4, 2, 8);
            g.fillRect(11, 4, 1, 8);
        }
        case "stack" -> {
            g.fillRect(4, 4, 8, 2);
            g.fillRect(5, 7, 7, 2);
            g.fillRect(6, 10, 6, 2);
        }
        case "previous" -> {
            g.fillRect(5, 7, 7, 2);
            g.fillRect(4, 6, 2, 4);
            g.fillRect(3, 7, 2, 2);
        }
        case "next" -> {
            g.fillRect(4, 7, 7, 2);
            g.fillRect(10, 6, 2, 4);
            g.fillRect(11, 7, 2, 2);
        }
        case "close" -> {
            g.setColor(new Color(224, 91, 82, 255));
            g.fillRect(5, 5, 2, 2);
            g.fillRect(9, 5, 2, 2);
            g.fillRect(7, 7, 2, 2);
            g.fillRect(5, 9, 2, 2);
            g.fillRect(9, 9, 2, 2);
        }
        case "lang_ru" -> {

            g.setColor(new Color(244, 246, 244, 255));
            g.fillRect(3, 4, 10, 3);
            g.setColor(new Color(48, 78, 168, 255));
            g.fillRect(3, 7, 10, 3);
            g.setColor(new Color(206, 60, 51, 255));
            g.fillRect(3, 10, 10, 3);
            g.setColor(new Color(17, 24, 25, 120));
            g.drawRect(3, 4, 9, 8);
        }
        case "lang_en" -> {

            Color navy = new Color(30, 52, 122, 255);
            Color white = new Color(244, 246, 244, 255);
            Color red = new Color(206, 60, 51, 255);
            g.setColor(navy);
            g.fillRect(3, 4, 10, 9);
            g.setColor(white);
            for (int i = 0; i < 9; i++) {
                int dx = i * 10 / 9;
                g.fillRect(3 + dx, 4 + i, 2, 1);
                g.fillRect(11 - dx, 4 + i, 2, 1);
            }
            g.setColor(red);
            for (int i = 0; i < 9; i++) {
                int dx = i * 10 / 9;
                g.fillRect(3 + dx, 4 + i, 1, 1);
                g.fillRect(12 - dx, 4 + i, 1, 1);
            }
            g.setColor(white);
            g.fillRect(3, 7, 10, 3);
            g.fillRect(6, 4, 4, 9);
            g.setColor(red);
            g.fillRect(3, 8, 10, 1);
            g.fillRect(7, 4, 2, 9);
            g.setColor(new Color(17, 24, 25, 120));
            g.drawRect(3, 4, 9, 8);
        }
        case "lang_auto" -> {
            g.fillRect(7, 3, 2, 3);
            g.fillRect(7, 10, 2, 3);
            g.fillRect(3, 7, 3, 2);
            g.fillRect(10, 7, 3, 2);
            g.fillRect(5, 5, 2, 2);
            g.fillRect(9, 5, 2, 2);
            g.fillRect(5, 9, 2, 2);
            g.fillRect(9, 9, 2, 2);
            g.setColor(muted);
            g.fillRect(7, 7, 2, 2);
        }
        case "jade_on" -> drawEye(g, accent, new Color(198, 240, 170, 255), panel);
        case "jade_off" -> {
            drawEye(g, muted, new Color(120, 132, 126, 255), panel);
            g.setColor(new Color(224, 91, 82, 255));
            for (int i = 0; i < 10; i++) {
                g.fillRect(3 + i, 3 + i, 1, 1);
            }
        }
        case "armor_on" -> drawArmorIcon(g, accent, true);
        case "armor_off" -> drawArmorIcon(g, muted, false);
        case "armor_border_accent", "armor_border_primary", "armor_border_secondary" -> {
            String channel = name.substring("armor_border_".length());
            g.setColor(parseColor(plugin.getConfig().getString("armor-hud.border." + channel),
                    name.endsWith("accent") ? accent : name.endsWith("primary")
                            ? new Color(175, 192, 122, 255) : new Color(50, 67, 50, 255)));
            g.drawRoundRect(2, 3, 11, 9, 3, 3);
            g.fillRect(4, 7, 8, 1);
        }
        case "logo" -> {

            g.setColor(new Color(198, 240, 170, 255));
            g.fillRect(6, 3, 4, 1);
            g.fillRect(4, 4, 3, 2);
            g.setColor(accent);
            g.fillRect(9, 4, 3, 2);
            g.fillRect(4, 6, 8, 3);
            g.setColor(new Color(66, 109, 59, 255));
            g.fillRect(5, 9, 6, 2);
            g.fillRect(7, 11, 2, 1);
            g.setColor(new Color(232, 252, 214, 255));
            g.fillRect(6, 4, 1, 1);
            g.fillRect(5, 6, 1, 2);
        }
        case "divider" -> {
            g.setColor(new Color(66, 90, 82, 200));
            g.fillRect(0, 7, 16, 1);
            g.setColor(new Color(132, 187, 99, 160));
            g.fillRect(5, 7, 6, 1);
        }
        case "frame" -> {

            g.setColor(new Color(24, 31, 32, 235));
            g.fillRect(0, 0, 16, 16);
            g.setColor(new Color(48, 62, 58, 255));
            g.drawRect(0, 0, 15, 15);
            g.setColor(new Color(14, 19, 20, 255));
            g.drawRect(1, 1, 13, 13);
        }
        case "panel" -> {

            g.setColor(new Color(36, 48, 45, 245));
            g.fillRect(0, 0, 16, 16);
            g.setColor(new Color(132, 187, 99, 90));
            g.drawRect(0, 0, 15, 15);
        }
        default -> g.setColor(muted);
        }
        g.dispose();
        return png(image);
    }

    private static void drawEye(Graphics2D g, Color iris, Color highlight, Color pupil) {
        g.setColor(iris);
        g.fillRect(5, 4, 6, 1);
        g.fillRect(3, 5, 10, 6);
        g.fillRect(5, 11, 6, 1);
        g.setColor(pupil);
        g.fillRect(6, 6, 4, 4);
        g.setColor(highlight);
        g.fillRect(6, 6, 2, 2);
    }

    private static void drawArmorIcon(Graphics2D g, Color color, boolean enabled) {
        g.setColor(color);
        g.fillRect(5, 3, 6, 2);
        g.fillRect(3, 5, 10, 6);
        g.fillRect(5, 11, 6, 2);
        if (!enabled) {
            g.setColor(new Color(224, 91, 82, 255));
            for (int i = 0; i < 10; i++) g.fillRect(3 + i, 3 + i, 1, 1);
        }
    }

    private BufferedImage readImage(String path) throws Exception {
        try (InputStream input = plugin.getResource(path)) {
            return input == null ? null : ImageIO.read(input);
        }
    }

    private void drawTile(Graphics2D graphics, BufferedImage tile, int x, int y) {
        if (tile == null) return;
        int offset = (TILE_SIZE - ICON_DRAW_SIZE) / 2;
        graphics.drawImage(tile,
                x * TILE_SIZE + offset,
                y * TILE_SIZE + offset,
                ICON_DRAW_SIZE,
                ICON_DRAW_SIZE,
                null);
    }

    private byte[] png(BufferedImage image) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private byte[] transparentPng(int width, int height) throws Exception {
        return png(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB));
    }


    private String blockTexture(Material material) {
        String id = material.getKey().getKey();
        return wailaTextureMap.getProperty(id, "block/stone");
    }

    private void loadProps(String resource, Properties target) {
        try (InputStream input = plugin.getResource(resource)) {
            if (input != null) target.load(input);
        } catch (Exception ignored) {
            target.clear();
        }
    }

    private byte[] textShader(boolean seeThrough) {
        String fogImport = seeThrough ? "" : "#moj_import <minecraft:fog.glsl>\n";
        String fogInputs = seeThrough ? "" : """
                in float sphericalVertexDistance;
                in float cylindricalVertexDistance;
                """;
        String fogApply = seeThrough
                ? "fragColor = color;"
                : """
                fragColor = apply_fog(color,
                    sphericalVertexDistance, cylindricalVertexDistance,
                    FogEnvironmentalStart, FogEnvironmentalEnd,
                    FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);""";

        String shader = """
                #version 330

                %s#moj_import <minecraft:dynamictransforms.glsl>

                uniform sampler2D Sampler0;

                %sin vec4 vertexColor;
                in vec2 texCoord0;

                out vec4 fragColor;

                void main() {
                    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
                    if (color.a < 0.1) {
                        discard;
                    }


                    bool jadeAccent = color.g > color.r * 1.16 && color.g > color.b * 1.05 && color.g > 0.16;
                    bool powerOff   = color.r > color.g * 1.28 && color.r > color.b * 1.20 && color.r > 0.22;
                    bool playerIcon = color.b > color.r * 1.30 && color.g > color.r * 1.25 && color.b > 0.35;
                    bool chestIcon  = color.r > 0.30 && color.g > 0.20
                                      && color.g < color.r * 0.90 && color.b < color.g * 0.72;

                    if (jadeAccent || powerOff || playerIcon || chestIcon) {
                        float shimmer = 0.88 + 0.12 * sin(
                            gl_FragCoord.x * 0.19 + gl_FragCoord.y * 0.11 + gl_FragCoord.y * 0.03);
                        vec3 target = powerOff    ? vec3(1.0, 0.42, 0.36)
                                    : playerIcon  ? vec3(0.35, 0.92, 1.0)
                                    : chestIcon   ? vec3(1.0, 0.72, 0.30)
                                    :               vec3(0.64, 1.0, 0.48);
                        color.rgb = mix(color.rgb, target, 0.22) * shimmer;
                    }
                    %s
                }
                """.formatted(fogImport, fogInputs, fogApply);
        return shader.getBytes(StandardCharsets.UTF_8);
    }

    private byte[] readPluginResource(String path) throws Exception {
        try (InputStream input = plugin.getResource(path)) {
            if (input == null) {
                return null;
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            input.transferTo(output);
            return output.toByteArray();
        }
    }

    private void write(ZipOutputStream zip, String path, byte[] data) throws Exception {
        zip.putNextEntry(new ZipEntry(path));
        zip.write(data);
        zip.closeEntry();
    }
}
