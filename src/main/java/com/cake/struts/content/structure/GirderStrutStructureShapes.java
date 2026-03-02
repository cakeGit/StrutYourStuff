package com.cake.struts.content.structure;

import com.cake.struts.content.StrutModelType;
import com.cake.struts.internal.util.ChunkedMap;
import com.cake.struts.internal.util.LevelSafeStorage;
import com.cake.struts.registry.StrutBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Handler and storage for all the connections between girder struts in a level.
 */
public class GirderStrutStructureShapes {

    private static final LevelSafeStorage<ShapeRegistry> STORAGE = new LevelSafeStorage<>(ShapeRegistry::new);

    public static void registerConnection(final Level level, final BlockPos from, final Direction fromFacing, final BlockPos to, final Direction toFacing, final StrutModelType modelType) {
        STORAGE.getForLevel(level).registerConnection(level, from, fromFacing, to, toFacing, modelType);
    }

    public static void unregisterConnection(final Level level, final BlockPos a, final BlockPos b) {
        STORAGE.getForLevel(level).unregisterConnection(level, new ConnectionKey(a, b));
    }

    public static VoxelShape getShape(final Level level, final BlockPos pos) {
        return STORAGE.getForLevel(level).getShape(pos);
    }

    public static VoxelShape getOutlineShape(final Level level, final BlockPos pos, final CollisionContext context) {
        return STORAGE.getForLevel(level).getOutlineShape(pos, context);
    }

    public static @Nullable ConnectionKey getTargetedConnection(final Level level, final BlockPos pos, final Vec3 origin, final Vec3 direction) {
        return STORAGE.getForLevel(level).getTargetedConnection(pos, origin, direction);
    }

    public static Set<ConnectionKey> getConnectionsAt(final Level level, final BlockPos pos) {
        return STORAGE.getForLevel(level).getConnectionsAt(pos);
    }

    public static void removePositionData(final Level level, final BlockPos pos) {
        STORAGE.getForLevel(level).removePositionData(pos);
    }

    public static boolean hasPositionData(final Level level, final BlockPos pos) {
        return STORAGE.getForLevel(level).hasPositionData(pos);
    }

    public static boolean placeStructureBlockIfPossible(final Level level, final BlockPos pos) {
        final BlockState currentState = level.getBlockState(pos);
        if (currentState.getBlock() == StrutBlocks.GIRDER_STRUT_STRUCTURE.get()) {
            return true;
        }
        if (!currentState.canBeReplaced()) {
            return false;
        }

        final FluidState fluidState = level.getFluidState(pos);
        final boolean waterlogged = fluidState.getType() == Fluids.WATER;
        BlockState structureState = StrutBlocks.GIRDER_STRUT_STRUCTURE.get().defaultBlockState();
        if (structureState.hasProperty(BlockStateProperties.WATERLOGGED)) {
            structureState = structureState.setValue(BlockStateProperties.WATERLOGGED, waterlogged);
        }

        return level.setBlock(pos, structureState, Block.UPDATE_ALL);
    }

    private static class ShapeRegistry {
        private final ChunkedMap<GirderStrutConnectionShape> connectionsByChunk;
        private final Map<ConnectionKey, GirderStrutConnectionShape> connectionsByKey = new HashMap<>();
        private final Map<BlockPos, PositionData> shapesByPosition = new HashMap<>();

        private ShapeRegistry(final Level level) {
            this.connectionsByChunk = new ChunkedMap<>(level) {
                @Override
                protected void onChunkEvicted(final ChunkPos chunk, final List<GirderStrutConnectionShape> evictedObjects) {
                    for (final GirderStrutConnectionShape connectionShape : evictedObjects) {
                        for (final BlockPos position : connectionShape.geometry().getPositions()) {
                            if (new ChunkPos(position).equals(chunk)) {
                                final PositionData positionData = shapesByPosition.get(position);
                                if (positionData != null) {
                                    positionData.remove(connectionShape.key());
                                    if (positionData.perConnectionShapes.isEmpty()) {
                                        shapesByPosition.remove(position);
                                    }
                                }
                            }
                        }
                    }
                }
            };
        }

        void registerConnection(final Level level, final BlockPos from, final Direction fromFacing, final BlockPos to, final Direction toFacing, final StrutModelType modelType) {
            final ConnectionKey key = new ConnectionKey(from, to);
            if (connectionsByKey.containsKey(key)) {
                return;
            }

            final BlockyStrutLineGeometry geometry = new BlockyStrutLineGeometry(from, fromFacing, to, toFacing,
                    modelType.shapeSizeXPixels(), modelType.shapeSizeYPixels(), modelType.voxelShapeResolutionPixels());
            final Set<ChunkPos> chunks = new HashSet<>();
            for (final BlockPos pos : geometry.getPositions()) {
                chunks.add(new ChunkPos(pos));
            }
            final GirderStrutConnectionShape shapeObj = new GirderStrutConnectionShape(key, geometry, chunks.toArray(new ChunkPos[0]));
            connectionsByKey.put(key, shapeObj);
            connectionsByChunk.add(shapeObj);

            for (final BlockPos pos : geometry.getPositions()) {
                final VoxelShape shape = geometry.getShapeForPosition(pos);
                if (shape.isEmpty()) continue;

                final PositionData pd = shapesByPosition.computeIfAbsent(pos, $ -> new PositionData());
                final boolean wasEmpty = pd.perConnectionShapes.isEmpty();
                pd.add(key, shape);

                if (!pos.equals(from) && !pos.equals(to) && wasEmpty) {
                    placeStructureBlockIfPossible(level, pos);
                }
            }
        }

        void unregisterConnection(final Level level, final ConnectionKey key) {
            final GirderStrutConnectionShape found = connectionsByKey.remove(key);
            if (found == null) return;

            connectionsByChunk.remove(found);

            for (final BlockPos pos : found.geometry().getPositions()) {
                final PositionData pd = shapesByPosition.get(pos);
                if (pd != null) {
                    pd.remove(key);
                    if (pd.perConnectionShapes.isEmpty()) {
                        shapesByPosition.remove(pos);
                        if (level.getBlockState(pos).getBlock() == StrutBlocks.GIRDER_STRUT_STRUCTURE.get()) {
                            level.removeBlock(pos, false);
                        }
                    }
                }
            }
        }

        VoxelShape getShape(final BlockPos pos) {
            final PositionData pd = shapesByPosition.get(pos);
            return pd == null ? Shapes.empty() : pd.mergedShape;
        }

        VoxelShape getOutlineShape(final BlockPos pos, final CollisionContext context) {
            final PositionData pd = shapesByPosition.get(pos);
            return pd == null ? Shapes.empty() : pd.getOutlineShape(pos, context);
        }

        @Nullable ConnectionKey getTargetedConnection(final BlockPos pos, final Vec3 origin, final Vec3 direction) {
            final PositionData pd = shapesByPosition.get(pos);
            return pd == null ? null : pd.getTargetedConnection(pos, origin, direction);
        }

        Set<ConnectionKey> getConnectionsAt(final BlockPos pos) {
            final PositionData pd = shapesByPosition.get(pos);
            return pd == null ? Set.of() : Set.copyOf(pd.perConnectionShapes.keySet());
        }

        void removePositionData(final BlockPos pos) {
            shapesByPosition.remove(pos);
        }

        boolean hasPositionData(final BlockPos pos) {
            return shapesByPosition.containsKey(pos);
        }
    }
}
