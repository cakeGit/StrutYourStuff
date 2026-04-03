package com.cake.struts.content.block;

import com.cake.struts.content.CableStrutInfo;
import com.cake.struts.content.StrutModelType;
import com.cake.struts.content.connection.GirderConnectionNode;
import com.cake.struts.content.structure.GirderStrutShapedBlock;
import com.cake.struts.registry.StrutItemTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED;

public abstract class StrutBlock extends Block implements SimpleWaterloggedBlock, GirderStrutShapedBlock, EntityBlock {

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

    protected abstract BlockEntityType<? extends StrutBlockEntity> getStrutBlockEntityType();

    @Override
    protected @NotNull BlockState rotate(final @NotNull BlockState state, final @NotNull Rotation rotation) {
        return super.rotate(state, rotation)
                .setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected @NotNull BlockState mirror(final @NotNull BlockState state, final @NotNull Mirror mirror) {
        return super.mirror(state, mirror)
                .setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    public @NotNull BlockState updateShape(final BlockState state, final @NotNull Direction direction, final @NotNull BlockState neighbourState, final @NotNull LevelAccessor world,
                                           final @NotNull BlockPos pos, final @NotNull BlockPos neighbourPos) {
        if (state.getValue(WATERLOGGED))
            world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
        return state;
    }

    @Override
    public @NotNull FluidState getFluidState(final BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : Fluids.EMPTY.defaultFluidState();
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED);
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(final @NotNull ItemStack stack, final @NotNull BlockState state,
                                                       final @NotNull Level level, final @NotNull BlockPos pos,
                                                       final @NotNull Player player, final @NotNull InteractionHand hand,
                                                       final @NotNull BlockHitResult hitResult) {
        if (this.cableRenderInfo != null && stack.is(StrutItemTags.WRENCHES) && !player.isShiftKeyDown()) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof final StrutBlockEntity blockEntity) {
                final boolean tensioned = blockEntity.toggleAllCableTension();
                final SoundType soundType = state.getSoundType();
                level.playSound(null, pos, soundType.getPlaceSound(), SoundSource.BLOCKS, 0.5f, tensioned ? 1.5f : 0.8f);
            }
            return ItemInteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
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
        return getAttachmentBaseShape(state.getValue(FACING), true);
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(final @NotNull BlockState state, final @NotNull BlockGetter level,
                                                 final @NotNull BlockPos pos, final @NotNull CollisionContext context) {
        final VoxelShape attachmentShape = getAttachmentBaseShape(state.getValue(FACING), false);
        if (cableRenderInfo != null) {
            return attachmentShape;
        }
        final VoxelShape strutShape = getStrutShape(level, pos);
        return strutShape.isEmpty() ? attachmentShape : Shapes.or(attachmentShape, strutShape);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final @NotNull BlockPos pos, final @NotNull BlockState state) {
        return new StrutBlockEntity(getStrutBlockEntityType(), pos, state);
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

    public static VoxelShape getAttachmentBaseShape(final Direction facing, final boolean interaction) {
        return switch (facing.getOpposite()) {
            case DOWN -> Block.box(3, interaction ? -5 : 0, 3, 13, 1, 13);
            case UP -> Block.box(3, 15, 3, 13, interaction ? 21 : 16, 13);
            case NORTH -> Block.box(3, 3, interaction ? -5 : 0, 13, 13, 1);
            case SOUTH -> Block.box(3, 3, 15, 13, 13, interaction ? 21 : 16);
            case WEST -> Block.box(interaction ? -5 : 0, 3, 3, 1, 13, 13);
            case EAST -> Block.box(15, 3, 3, interaction ? 21 : 16, 13, 13);
        };
    }
}

