package dev.c0redev.someutils.pack;

import com.sun.net.httpserver.HttpServer;
import dev.c0redev.someutils.SomeUtilsPlugin;
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
import java.awt.Graphics2D;
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
        if (index == null) {
            return "";
        }
        return new String(Character.toChars(BLOCK_GLYPH_START + Integer.parseInt(index)));
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
                        "pack_format": 75,
                        "min_format": 70,
                        "max_format": 75,
                        "description": "SomeUtils Jade HUD"
                      }
                    }
                    """.getBytes(StandardCharsets.UTF_8));

            write(zip, "assets/someutils/font/jade.json", vanillaIconFont().getBytes(StandardCharsets.UTF_8));
            write(zip, "assets/someutils/font/waila.json", wailaFont().getBytes(StandardCharsets.UTF_8));
            write(zip, "assets/someutils/textures/font/waila_start.png", wailaPartPng(2, true, false));
            write(zip, "assets/someutils/textures/font/waila_part.png", wailaPartPng(10, false, false));
            write(zip, "assets/someutils/textures/font/waila_end.png", wailaPartPng(2, false, true));
            byte[] renderedAtlas = readPluginResource("waila-rendered-atlas.png");
            write(zip, "assets/someutils/textures/font/block_atlas.png", renderedAtlas != null ? renderedAtlas : blockAtlasPng());
            write(zip, "assets/someutils/textures/font/item_atlas.png", itemAtlasPng());
            write(zip, "assets/someutils/textures/font/status_atlas.png", statusAtlasPng());
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
        }

        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        try (InputStream input = new FileInputStream(packFile)) {
            packHash = sha1.digest(input.readAllBytes());
        }
    }

    private String vanillaIconFont() {
        return """
                {
                  "providers": [
                    {"type":"bitmap","file":"someutils:font/status_atlas.png","ascent":10,"height":16,"chars":["\\uE008\\uE009\\uE00E\\uE00F\\uE010"]},
                    {"type":"bitmap","file":"someutils:font/item_atlas.png","ascent":10,"height":16,"chars":["\\uE00A\\uE00B\\uE00C\\uE00D"]},
                    {"type":"bitmap","file":"someutils:font/block_atlas.png","ascent":10,"height":16,"chars":%s}
                  ]
                }
                """.formatted(blockGlyphRows());
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
                    {"type":"bitmap","file":"someutils:font/waila_start.png","ascent":14,"height":26,"chars":["\\uF000"]},
                    {"type":"bitmap","file":"someutils:font/waila_part.png","ascent":14,"height":26,"chars":["\\uF001"]},
                    {"type":"bitmap","file":"someutils:font/waila_end.png","ascent":14,"height":26,"chars":["\\uF002"]},
                    {"type":"space","advances":{"%s":-1,%s}}
                  ]
                }
                """.formatted(cursor, moves);
    }

    private byte[] wailaPartPng(int width, boolean left, boolean right) throws Exception {
        BufferedImage image = new BufferedImage(width, 26, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        Color shadow = new Color(3, 5, 6, 185);
        Color border = new Color(62, 82, 75, 255);
        Color fill = new Color(16, 23, 24, 245);
        Color accent = new Color(132, 187, 99, 255);
        Color accentDim = new Color(66, 109, 59, 255);

        graphics.setColor(shadow);
        graphics.fillRect(0, 5, width, 21);
        graphics.setColor(border);
        graphics.fillRect(0, 0, width, 22);
        graphics.setColor(fill);
        graphics.fillRect(0, 1, width, 20);
        graphics.setColor(new Color(98, 140, 78, 130));
        graphics.fillRect(0, 1, width, 1);
        graphics.setColor(new Color(45, 68, 60, 180));
        graphics.fillRect(0, 20, width, 1);
        if (left) {
            graphics.setColor(accentDim);
            graphics.fillRect(0, 1, 2, 20);
            graphics.setColor(accent);
            graphics.fillRect(0, 2, 1, 18);
        }
        if (right) {
            graphics.setColor(border);
            graphics.fillRect(width - 1, 1, 1, 20);
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
            "lang_ru", "lang_en", "lang_auto", "jade_on", "jade_off", "logo", "divider",
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
