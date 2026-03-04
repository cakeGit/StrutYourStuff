package com.cake.struts.compat.flywheel;

import com.cake.struts.content.block.StrutBlockEntity;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import dev.engine_room.flywheel.lib.visualization.VisualizationHelper;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class FlywheelCompat {

    public void register(final BlockEntityType<? extends StrutBlockEntity> blockEntityType) {
        SimpleBlockEntityVisualizer.builder(blockEntityType)
                .factory(StrutFlywheelVisual::new)
                .skipVanillaRender(blockEntity -> supportsVisualization(blockEntity.getLevel()))
                .apply();
    }

    public boolean supportsVisualization(final @Nullable LevelAccessor level) {
        return VisualizationManager.supportsVisualization(level);
    }

    public void queueUpdate(final @NotNull BlockEntity blockEntity) {
        VisualizationHelper.queueUpdate(blockEntity);
    }
}
