package com.cake.struts.content.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for blocks that participate in the girder strut structure shape system.
 * Provides common shape lookup for strut collision and outline geometry.
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
}
