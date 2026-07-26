package dev.c0redev.someutils.jade;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.Material;

public final class TargetInfo {

    public enum Icon {
        BLOCK,
        STONE,
        DIRT,
        WOOD,
        ORE,
        PLANT,
        FLUID,
        ENTITY
    }

    public enum Tool {
        NONE(""),
        PICKAXE("Pickaxe"),
        AXE("Axe"),
        SHOVEL("Shovel"),
        SHEARS("Shears");

        private final String label;

        Tool(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public enum Type {
        BLOCK,
        ENTITY,
        NONE
    }

    private final Type type;
    private final Block block;
    private final Entity entity;
    private final String title;
    private final String subtitle;
    private final String detail;
    private final Icon icon;
    private final Tool tool;
    private final Material material;
    private final String fingerprint;

    private TargetInfo(Type type, Block block, Entity entity, String title, String subtitle, String detail, Icon icon, Tool tool, Material material) {
        this.type = type;
        this.block = block;
        this.entity = entity;
        this.title = title == null ? "" : title;
        this.subtitle = subtitle == null ? "" : subtitle;
        this.detail = detail == null ? "" : detail;
        this.icon = icon;
        this.tool = tool;
        this.material = material;
        this.fingerprint = this.title + '|' + this.subtitle + '|' + this.detail;
    }

    public static TargetInfo empty() {
        return new TargetInfo(Type.NONE, null, null, "", "", "", Icon.BLOCK, Tool.NONE, Material.AIR);
    }

    public static TargetInfo ofBlock(Block block, String title, String subtitle, String detail, Icon icon, Tool tool) {
        return new TargetInfo(Type.BLOCK, block, null, title, subtitle, detail, icon, tool, block.getType());
    }

    public static TargetInfo ofEntity(Entity entity, String title, String subtitle, String detail) {
        return new TargetInfo(Type.ENTITY, null, entity, title, subtitle, detail, Icon.ENTITY, Tool.NONE, Material.AIR);
    }

    public Type getType() {
        return type;
    }

    public Block getBlock() {
        return block;
    }

    public Entity getEntity() {
        return entity;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getDetail() {
        return detail;
    }

    public Icon getIcon() {
        return icon;
    }

    public Tool getTool() {
        return tool;
    }

    public Material getMaterial() {
        return material;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public boolean isEmpty() {
        return type == Type.NONE || title.isEmpty();
    }
}
