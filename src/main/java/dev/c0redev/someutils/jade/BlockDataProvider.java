package dev.c0redev.someutils.jade;

import dev.c0redev.someutils.config.PluginConfig;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.Furnace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.AnaloguePowerable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Powerable;
import org.bukkit.block.data.type.Beehive;
import org.bukkit.Material;

public final class BlockDataProvider {

    private BlockDataProvider() {
    }

    public static TargetInfo getBlockInfo(Block block, PluginConfig cfg) {
        if (block == null || block.getType().isAir()) {
            return TargetInfo.empty();
        }

        String name = formatMaterialName(block.getType().name());
        StringBuilder subtitle = new StringBuilder();
        StringBuilder detail = new StringBuilder();
        BlockData data = block.getBlockData();

        if (cfg.isShowRedstone()) {
            if (data instanceof AnaloguePowerable analogue) {
                append(subtitle, "Power: " + analogue.getPower());
            } else if (data instanceof Powerable powerable) {
                append(subtitle, powerable.isPowered() ? "Powered" : "Unpowered");
            }
        }

        if (cfg.isShowGrowth() && data instanceof Ageable ageable) {
            int max = Math.max(1, ageable.getMaximumAge());
            int percent = (int) ((ageable.getAge() * 100.0) / max);
            append(subtitle, "Growth: " + percent + "%");
        }

        if (cfg.isShowBeehive() && data instanceof Beehive beehive) {
            append(subtitle, "Honey: " + beehive.getHoneyLevel() + "/" + beehive.getMaximumHoneyLevel());
        }

        BlockState state = block.getState();

        if (cfg.isShowFurnace() && state instanceof Furnace furnace) {
            int cookTotal = Math.max(1, furnace.getCookTimeTotal());
            int cookPercent = furnace.getCookTime() * 100 / cookTotal;
            int burn = furnace.getBurnTime();
            append(subtitle, "Smelting: " + cookPercent + "%");
            append(detail, "Fuel: " + (burn > 0 ? (burn / 20) + "s" : "Empty"));
        }

        if (cfg.isShowContainer() && state instanceof Container container && !(state instanceof Furnace)) {
            int filled = 0;
            for (var item : container.getInventory().getContents()) {
                if (item != null && !item.getType().isAir()) {
                    filled++;
                }
            }
            append(detail, "Items: " + filled + "/" + container.getInventory().getSize());
        }

        TargetInfo.Tool tool = cfg.isShowTool() ? harvestTool(block.getType().name()) : TargetInfo.Tool.NONE;
        return TargetInfo.ofBlock(block, name, subtitle.toString(), detail.toString(), iconFor(block.getType()), tool);
    }

    private static void append(StringBuilder sb, String part) {
        if (sb.length() > 0) {
            sb.append(" §8| ");
        }
        sb.append(part);
    }

    private static TargetInfo.Tool harvestTool(String n) {
        if (n.contains("ORE") || n.contains("STONE") || n.contains("DEEPSLATE")
                || n.contains("BRICK") || n.contains("NETHERRACK") || n.contains("OBSIDIAN"))
            return TargetInfo.Tool.PICKAXE;
        if (n.contains("LOG") || n.contains("PLANKS") || n.contains("WOOD")
                || n.contains("BAMBOO") || n.contains("FENCE") || n.contains("DOOR"))
            return TargetInfo.Tool.AXE;
        if (n.contains("DIRT") || n.contains("GRASS") || n.contains("SAND")
                || n.contains("GRAVEL") || n.contains("CLAY") || n.contains("SOUL"))
            return TargetInfo.Tool.SHOVEL;
        if (n.contains("LEAVES") || n.contains("WOOL") || n.contains("COBWEB"))
            return TargetInfo.Tool.SHEARS;
        return TargetInfo.Tool.NONE;
    }

    private static TargetInfo.Icon iconFor(Material material) {
        String n = material.name();
        if (material == Material.WATER || material == Material.LAVA
                || n.contains("WATER") || n.contains("LAVA")) return TargetInfo.Icon.FLUID;
        if (n.contains("ORE") || n.contains("RAW_")
                || n.contains("ANCIENT_DEBRIS") || n.contains("AMETHYST")) return TargetInfo.Icon.ORE;
        if (n.contains("LOG") || n.contains("PLANKS") || n.contains("WOOD")
                || n.contains("BAMBOO") || n.contains("STEM")) return TargetInfo.Icon.WOOD;
        if (n.contains("DIRT") || n.contains("GRASS_BLOCK") || n.contains("SAND")
                || n.contains("GRAVEL") || n.contains("CLAY") || n.contains("SOUL")) return TargetInfo.Icon.DIRT;
        if (n.contains("LEAVES") || n.contains("FLOWER") || n.contains("SAPLING")
                || n.contains("CROP") || n.contains("WHEAT") || n.contains("CARROT")
                || n.contains("POTATO") || n.contains("NETHER_WART")) return TargetInfo.Icon.PLANT;
        if (n.contains("STONE") || n.contains("DEEPSLATE") || n.contains("COBBLE")
                || n.contains("BRICK") || n.contains("OBSIDIAN") || n.contains("NETHERRACK")) return TargetInfo.Icon.STONE;
        return TargetInfo.Icon.BLOCK;
    }

    private static String formatMaterialName(String name) {
        String[] words = name.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }
}
