package com.cake.struts.content.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for blocks that participate in the girder strut structure shape system.
 * Provides common shape lookup, neighbor reconstruction scheduling, and tick-based
 * rebuilding of missing structure blocks.
 */
public interface GirderStrutShapedBlock {

    /**
     * Get the strut connection shape at this position from the shape registry.
     * Returns empty if the level is not a full Level (e.g. during chunk gen).
     */
    default VoxelShape getStrutShape(final @NotNull BlockGetter level, final @NotNull BlockPos pos) {
        if (level instanceof final Level worldLevel) {
            return GirderStrutStructureShapes.getShape(worldLevel, pos);
        }
        return Shapes.empty();
    }

    /**
     * Get the per-strut outline/interaction shape at this position.
     */
    default VoxelShape getStrutOutlineShape(final @NotNull BlockGetter level, final @NotNull BlockPos pos,
                                            final @NotNull CollisionContext context) {
        if (level instanceof final Level worldLevel) {
            return GirderStrutStructureShapes.getOutlineShape(worldLevel, pos, context);
        }
        return Shapes.empty();
    }

    /**
     * Call from {@link Block#updateShape}. Schedules restoration of structure blocks if a neighboring block was removed can now be replaced.
     */
    default void scheduleStructureRebuildIfNeeded(final @NotNull BlockState state, final @NotNull Direction direction,
                                                  final @NotNull BlockState neighborState, final @NotNull LevelAccessor level,
                                                  final @NotNull BlockPos pos, final @NotNull Block self) {
        if (!level.isClientSide() && neighborState.canBeReplaced()) {
            level.scheduleTick(pos, self, 1);
        }
    }

    /**
     * Call from {@link Block#tick} to restore missing structure blocks at neighboring positions
     * that still have shape data in the registry.
     */
    default void rebuildMissingStructureNeighbors(final @NotNull ServerLevel level, final @NotNull BlockPos pos) {
        for (final Direction dir : Direction.values()) {
            final BlockPos neighbor = pos.relative(dir);
            if (GirderStrutStructureShapes.hasPositionData(level, neighbor)) {
                GirderStrutStructureShapes.placeStructureBlockIfPossible(level, neighbor);
            }
        }
    }
}
