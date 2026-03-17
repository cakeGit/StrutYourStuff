package com.cake.struts.content.structure;

import com.cake.struts.StrutYourStuff;
import com.cake.struts.content.block.StrutBlock;
import com.cake.struts.registry.StrutBlocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = StrutYourStuff.MOD_ID)
public class GirderStrutStructureRestoreHandler {

    @SubscribeEvent
    public static void onBlockBreak(final BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof final ServerLevel level)) {
            return;
        }

        final BlockState brokenState = event.getState();
        if (brokenState.getBlock() instanceof StrutBlock || brokenState.is(StrutBlocks.GIRDER_STRUT_STRUCTURE.get())) {
            return;
        }

        if (GirderStrutStructureShapes.hasPositionData(level, event.getPos())) {
            GirderStrutStructureShapes.queueRestoreFromBreak(level, event.getPos());
        }
    }

    @SubscribeEvent
    public static void onLevelTick(final LevelTickEvent.Post event) {
        if (event.getLevel() instanceof final ServerLevel level) {
            GirderStrutStructureShapes.flushQueuedRestores(level);
        }
    }
}
