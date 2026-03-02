package com.cake.struts.content.block;

import com.cake.struts.compat.flywheel.FlywheelCompatLoader;
import com.cake.struts.content.IAntiClippedShadowLighter;
import com.cake.struts.content.StrutModelType;
import com.cake.struts.content.connection.GirderConnectionNode;
import com.cake.struts.content.structure.GirderStrutStructureShapes;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import com.cake.struts.registry.StrutBlockEntities;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StrutBlockEntity extends BlockEntity implements IAntiClippedShadowLighter {

    private static final StrutModelType DEFAULT_MODEL_TYPE = new StrutModelType(
            ResourceLocation.fromNamespaceAndPath("struts", "block/girder_strut/girder_strut_segment"),
            ResourceLocation.fromNamespaceAndPath("struts", "block/industrial_iron_block"));

    /**
     * Set on the client during startup to receive connection-change notifications.
     * Never referenced from dedicated-server code.
     */
    public static @Nullable BiConsumer<Level, StrutBlockEntity> CLIENT_UPDATE_LISTENER;
    public static @Nullable BiConsumer<Level, BlockPos> CLIENT_REMOVE_LISTENER;

    private final Set<GirderConnectionNode> connections = new HashSet<>();
    private final Set<GirderConnectionNode> registeredConnections = new HashSet<>();
    private final Set<BlockPos> unresolvedLegacyConnections = new HashSet<>();
    public List<BakedQuad> connectionQuadCache;

    public StrutBlockEntity(final BlockPos pos, final BlockState state) {
        this(StrutBlockEntities.GIRDER_STRUT.get(), pos, state);
    }

    public StrutBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            final StrutModelType modelType = getBlockState().getBlock() instanceof final StrutBlock block ? block.getModelType() : DEFAULT_MODEL_TYPE;
            final boolean shouldRegisterShapes = shouldRegisterStructureShapes();
            for (final GirderConnectionNode data : connections) {
                if (shouldRegisterShapes && registeredConnections.add(data)) {
                    GirderStrutStructureShapes.registerConnection(level, getBlockPos(), getAttachmentDirection(), data.absoluteFrom(getBlockPos()), data.peerFacing(), modelType);
                }
            }
            tryResolveLegacyConnections();
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null) {
            if (!level.isClientSide) {
                for (final GirderConnectionNode data : registeredConnections) {
                    GirderStrutStructureShapes.unregisterConnection(level, getBlockPos(), data.absoluteFrom(getBlockPos()));
                }
                registeredConnections.clear();
            } else if (CLIENT_REMOVE_LISTENER != null) {
                CLIENT_REMOVE_LISTENER.accept(level, getBlockPos());
            }
        }
        connectionQuadCache = null;
    }

    public void addConnection(final BlockPos other, final Direction otherFacing) {
        final GirderConnectionNode data = GirderConnectionNode.fromAbsolute(getBlockPos(), other, otherFacing);
        if (!other.equals(getBlockPos()) && connections.add(data)) {
            if (level != null && !level.isClientSide && shouldRegisterStructureShapes() && registeredConnections.add(data)) {
                final StrutModelType modelType = getBlockState().getBlock() instanceof final StrutBlock block ? block.getModelType() : DEFAULT_MODEL_TYPE;
                GirderStrutStructureShapes.registerConnection(level, getBlockPos(), getAttachmentDirection(), other, otherFacing, modelType);
            }
            notifyModelChange();
        }
    }

    public void removeConnection(final BlockPos pos) {
        GirderConnectionNode toRemove = null;
        final BlockPos relative = pos.subtract(getBlockPos());
        for (final GirderConnectionNode data : connections) {
            if (data.relativeOffset().equals(relative)) {
                toRemove = data;
                break;
            }
        }
        if (toRemove != null && connections.remove(toRemove)) {
            if (level != null && !level.isClientSide && registeredConnections.remove(toRemove)) {
                GirderStrutStructureShapes.unregisterConnection(level, getBlockPos(), pos);
            }
            notifyModelChange();
        }
    }

    public int connectionCount() {
        return connections.size();
    }

    public Set<GirderConnectionNode> getConnectionsCopy() {
        return Set.copyOf(connections);
    }

    public int getConnectionHash() {
        return connections.hashCode();
    }

    private void notifyModelChange() {
        connectionQuadCache = null;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
            if (level.isClientSide) {
                FlywheelCompatLoader.queueUpdate(this);
            }
        }
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        final ListTag list = new ListTag();
        for (final GirderConnectionNode p : connections) {
            final CompoundTag ct = new CompoundTag();
            ct.putInt("X", p.relativeOffset().getX());
            ct.putInt("Y", p.relativeOffset().getY());
            ct.putInt("Z", p.relativeOffset().getZ());
            ct.putInt("Facing", p.peerFacing().get3DDataValue());
            list.add(ct);
        }
        for (final BlockPos unresolvedOffset : unresolvedLegacyConnections) {
            final CompoundTag ct = new CompoundTag();
            ct.putInt("X", unresolvedOffset.getX());
            ct.putInt("Y", unresolvedOffset.getY());
            ct.putInt("Z", unresolvedOffset.getZ());
            list.add(ct);
        }
        tag.put("Connections", list);
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        connections.clear();
        unresolvedLegacyConnections.clear();
        if (tag.contains("Connections", Tag.TAG_LIST)) {
            final ListTag list = tag.getList("Connections", Tag.TAG_COMPOUND);
            for (final Tag t : list) {
                if (t instanceof final CompoundTag ct) {
                    final BlockPos offset = new BlockPos(ct.getInt("X"), ct.getInt("Y"), ct.getInt("Z"));
                    if (ct.contains("Facing", Tag.TAG_INT)) {
                        final Direction facing = Direction.from3DDataValue(ct.getInt("Facing"));
                        connections.add(new GirderConnectionNode(offset, facing));
                    } else {
                        unresolvedLegacyConnections.add(offset);
                    }
                }
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        final CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(final CompoundTag tag, final HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
        connectionQuadCache = null;
        if (level != null && level.isClientSide) {
            FlywheelCompatLoader.queueUpdate(this);
            if (CLIENT_UPDATE_LISTENER != null) {
                CLIENT_UPDATE_LISTENER.accept(level, this);
            }
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(final Connection connection, final ClientboundBlockEntityDataPacket packet, final HolderLookup.Provider registries) {
        final CompoundTag tag = packet.getTag();
        if (tag != null) {
            handleUpdateTag(tag, registries);
        }
    }

    private void tryResolveLegacyConnections() {
        if (level == null || level.isClientSide || unresolvedLegacyConnections.isEmpty()) {
            return;
        }

        final Set<BlockPos> resolvedOffsets = new HashSet<>();
        for (final BlockPos unresolvedOffset : unresolvedLegacyConnections) {
            final BlockPos otherPosition = getBlockPos().offset(unresolvedOffset);
            Direction otherFacing = null;

            if (level.getBlockEntity(otherPosition) instanceof final StrutBlockEntity otherBlockEntity) {
                otherFacing = otherBlockEntity.getAttachmentDirection();
            } else if (level.getBlockState(otherPosition).getBlock() instanceof StrutBlock) {
                otherFacing = level.getBlockState(otherPosition).getValue(StrutBlock.FACING);
            }

            if (otherFacing != null) {
                addConnection(otherPosition, otherFacing);
                resolvedOffsets.add(unresolvedOffset);
            }
        }

        if (!resolvedOffsets.isEmpty()) {
            unresolvedLegacyConnections.removeAll(resolvedOffsets);
            notifyModelChange();
        }
    }

    public Vec3 getAttachment() {
        return Vec3.atCenterOf(getBlockPos()).relative(getBlockState().getValue(StrutBlock.FACING), -0.4);
    }

    public Direction getAttachmentDirection() {
        return getBlockState().getValue(StrutBlock.FACING);
    }

    public StrutModelType getModelType() {
        return getBlockState().getBlock() instanceof final StrutBlock block ? block.getModelType() : DEFAULT_MODEL_TYPE;
    }

    private boolean shouldRegisterStructureShapes() {
        return !(getBlockState().getBlock() instanceof final StrutBlock block && block.getCableRenderInfo() != null);
    }
}

