package dev.c0redev.someutils.jade;

import dev.c0redev.someutils.config.PluginConfig;
import org.bukkit.FluidCollisionMode;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;


final class JadeTargetLocator {

    private JadeTargetLocator() {
    }

    static TargetInfo locate(Player player, PluginConfig cfg) {
        double range = cfg.getJadeRange();
        if (cfg.isShowEntities()) {
            RayTraceResult hit = player.getWorld().rayTraceEntities(
                    player.getEyeLocation(),
                    player.getEyeLocation().getDirection(),
                    range,
                    0.4,
                    entity -> entity != player && entity instanceof LivingEntity && player.hasLineOfSight(entity)
            );
            if (hit != null && hit.getHitEntity() != null) {
                return EntityDataProvider.getEntityInfo(hit.getHitEntity(), cfg);
            }
        }
        if (cfg.isShowBlocks()) {
            Block block = player.getTargetBlockExact((int) Math.ceil(range), FluidCollisionMode.NEVER);
            if (block != null && !block.getType().isAir()) {
                return BlockDataProvider.getBlockInfo(block, cfg);
            }
        }
        return TargetInfo.empty();
    }
}
