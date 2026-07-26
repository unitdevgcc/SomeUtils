package dev.c0redev.someutils.tools;

import xyz.xenondevs.renderer.MinecraftModelRenderer;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipFile;

public final class WailaAtlasGenerator {

    private static final int CELL = 16;
    private static final int ICON = 12;
    private static final int COLUMNS = 32;

    private WailaAtlasGenerator() {
    }

    public static void main(String[] args) throws Exception {
        Path clientJar = Path.of(args[0]);
        Path textureMapFile = Path.of(args[1]);
        File outputDir = Path.of(args[2]).toFile();
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IllegalStateException("Cannot create rendered WAILA output directory");
        }

        Properties textureMap = new Properties();
        try (InputStream input = new FileInputStream(textureMapFile.toFile())) {
            textureMap.load(input);
        }
        List<String> ids = new ArrayList<>(textureMap.stringPropertyNames());
        ids.sort(String::compareTo);

        int rows = (ids.size() + COLUMNS - 1) / COLUMNS;
        BufferedImage atlas = new BufferedImage(COLUMNS * CELL, rows * CELL, BufferedImage.TYPE_INT_ARGB);
        Path rendererJar = Path.of(MinecraftModelRenderer.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        MinecraftModelRenderer renderer = new MinecraftModelRenderer(
                512, 512, 128, 128, List.of(asZip(clientJar), asZip(rendererJar)), true,
                40.0, 0.95, 0.1, 0.1
        );

        Properties glyphMap = new Properties();
        try (ZipFile zip = new ZipFile(clientJar.toFile())) {
            for (int index = 0; index < ids.size(); index++) {
                String id = ids.get(index);
                BufferedImage image = render(renderer, zip, id, textureMap.getProperty(id));
                if (image != null) {
                    drawIcon(atlas, image, index % COLUMNS, index / COLUMNS);
                }
                glyphMap.setProperty(id, Integer.toString(index));
            }
        }

        ImageIO.write(atlas, "png", new File(outputDir, "waila-rendered-atlas.png"));
        try (var output = Files.newOutputStream(Path.of(outputDir.getPath(), "waila-glyph-map.properties"))) {
            glyphMap.store(output, "Generated WAILA glyph indices");
        }
    }

    private static BufferedImage render(MinecraftModelRenderer renderer, ZipFile zip, String id, String fallbackTexture) {
        for (String model : List.of("item/" + id, "block/" + id)) {
            if (zip.getEntry("assets/minecraft/models/" + model + ".json") == null && !isSpecialRendererModel(model)) {
                continue;
            }
            try {
                return renderer.renderModel(model);
            } catch (RuntimeException ignored) {
            }
        }
        if (fallbackTexture == null) {
            return null;
        }
        try (InputStream input = zip.getInputStream(zip.getEntry("assets/minecraft/textures/" + fallbackTexture + ".png"))) {
            return input == null ? null : ImageIO.read(input);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean isSpecialRendererModel(String model) {
        return model.endsWith("/chest") || model.endsWith("/trapped_chest") || model.endsWith("/ender_chest")
                || model.endsWith("/barrel") || model.endsWith("/bell");
    }

    private static Path asZip(Path source) throws Exception {
        Path zip = source.resolveSibling(source.getFileName().toString() + ".zip");
        if (!Files.exists(zip) || Files.size(zip) != Files.size(source)) {
            Files.copy(source, zip, StandardCopyOption.REPLACE_EXISTING);
        }
        return zip;
    }

    private static void drawIcon(BufferedImage atlas, BufferedImage source, int cellX, int cellY) {
        int minX = source.getWidth();
        int minY = source.getHeight();
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                if ((source.getRGB(x, y) >>> 24) == 0) continue;
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }
        if (maxX < minX || maxY < minY) return;
        Graphics2D graphics = atlas.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(source, cellX * CELL + 2, cellY * CELL + 2, cellX * CELL + 2 + ICON, cellY * CELL + 2 + ICON,
                minX, minY, maxX + 1, maxY + 1, null);
        graphics.dispose();
    }
}
