package com.cake.struts.content.block;

import com.cake.struts.content.CableStrutInfo;
import com.cake.struts.content.StrutModelType;
import com.cake.struts.content.connection.GirderConnectionNode;
import com.cake.struts.content.structure.GirderStrutShapedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED;

public class StrutBlock extends Block implements SimpleWaterloggedBlock, GirderStrutShapedBlock, EntityBlock {

    public static final DirectionProperty FACING = DirectionalBlock.FACING;
    public static final int MAX_SPAN = 30;

    private StrutModelType modelType;
    private final @Nullable CableStrutInfo cableRenderInfo;

    public StrutBlock(final Properties properties, final StrutModelType modelType) {
        this(properties, modelType, null);
    }

    public StrutBlock(final Properties properties, final StrutModelType modelType, final @Nullable CableStrutInfo cableRenderInfo) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.UP).setValue(WATERLOGGED, false));
        this.modelType = modelType;
        this.cableRenderInfo = cableRenderInfo;
    }

    @Override
    protected BlockState rotate(final BlockState state, final Rotation rotation) {
        return super.rotate(state, rotation)
                .setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(final BlockState state, final Mirror mirror) {
        return super.mirror(state, mirror)
                .setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    public @NotNull BlockState updateShape(final BlockState state, final @NotNull Direction direction, final @NotNull BlockState neighbourState, final @NotNull LevelAccessor world,
                                           final @NotNull BlockPos pos, final @NotNull BlockPos neighbourPos) {
        if (state.getValue(WATERLOGGED))
            world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
        scheduleStructureRebuildIfNeeded(state, direction, neighbourState, world, pos, this);
        return state;
    }

    @Override
    protected void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        rebuildMissingStructureNeighbors(level, pos);
    }

    @Override
    public @NotNull FluidState getFluidState(final BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : Fluids.EMPTY.defaultFluidState();
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        final Level level = context.getLevel();
        final BlockPos pos = context.getClickedPos();
        final FluidState ifluidstate = level.getFluidState(pos);
        final BlockState state = super.getStateForPlacement(context);
        if (state == null)
            return null;
        return state.setValue(FACING, context.getClickedFace()).setValue(WATERLOGGED, ifluidstate.getType() == Fluids.WATER);
    }

    @Override
    public @NotNull VoxelShape getShape(final BlockState state, final @NotNull BlockGetter level, final @NotNull BlockPos pos, final @NotNull CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(final @NotNull BlockState state, final @NotNull BlockGetter level,
                                                 final @NotNull BlockPos pos, final @NotNull CollisionContext context) {
        if (cableRenderInfo != null) {
            return Shapes.empty();
        }
        return getStrutShape(level, pos);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final @NotNull BlockPos pos, final @NotNull BlockState state) {
        return new StrutBlockEntity(pos, state);
    }

    @Override
    public PushReaction getPistonPushReaction(final @NotNull BlockState state) {
        return PushReaction.NORMAL;
    }

    @Override
    public @NotNull BlockState playerWillDestroy(final @NotNull Level level, final @NotNull BlockPos pos, final @NotNull BlockState state, final Player player) {
        final boolean shouldPreventDrops = player.hasInfiniteMaterials();

        if (shouldPreventDrops && !level.isClientSide) {
            destroyConnectedStrut(level, pos, false);
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    private void destroyConnectedStrut(final Level level, final BlockPos pos, final boolean dropBlock) {
        if (level.getBlockEntity(pos) instanceof final StrutBlockEntity gbe) {
            for (final GirderConnectionNode data : gbe.getConnectionsCopy()) {
                final BlockPos otherPos = data.absoluteFrom(pos);
                final BlockEntity otherBe = level.getBlockEntity(otherPos);
                if (otherBe instanceof final StrutBlockEntity other) {
                    other.removeConnection(pos);
                    if (other.connectionCount() == 0) {
                        level.destroyBlock(otherPos, dropBlock);
                    }
                }
            }
        }
    }

    @Override
    public void onRemove(final BlockState state, final @NotNull Level level, final @NotNull BlockPos pos, final BlockState newState, final boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide) {
                destroyConnectedStrut(level, pos, true);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    public StrutModelType getModelType() {
        return modelType;
    }

    public @Nullable CableStrutInfo getCableRenderInfo() {
        return cableRenderInfo;
    }

    public void setModelType(final StrutModelType modelType) {
        this.modelType = modelType;
    }

    public static VoxelShape getAttachmentBaseShape(final Direction facing) {
        return switch (facing.getOpposite()) {
            case DOWN -> Block.box(3, 0, 3, 13, 1, 13);
            case UP -> Block.box(3, 15, 3, 13, 16, 13);
            case NORTH -> Block.box(3, 3, 0, 13, 13, 1);
            case SOUTH -> Block.box(3, 3, 15, 13, 13, 16);
            case WEST -> Block.box(0, 3, 3, 1, 13, 13);
            case EAST -> Block.box(15, 3, 3, 16, 13, 13);
        };
    }
}

