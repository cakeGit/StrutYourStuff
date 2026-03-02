package com.cake.struts.internal.wrench;

import com.cake.struts.content.block.StrutBlock;
import com.cake.struts.content.block.StrutBlockEntity;
import com.cake.struts.content.connection.GirderConnectionNode;
import com.cake.struts.content.structure.ConnectionKey;
import com.cake.struts.content.structure.GirderStrutStructureShapes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.common.EventBusSubscriber;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@EventBusSubscriber(modid = "struts")
public class StrutWrenchEvents {
    public static void breakStrutServer(final @NotNull Player player, final @NotNull ConnectionKey target, final boolean isWrench) {
        if (!player.mayBuild()) return;
        final ServerLevel level = (ServerLevel) player.level();
        final java.util.Set<BlockPos> anchorsToRemove = new java.util.HashSet<>();
        if (shouldRemoveAnchor(level, target.a(), 1)) {
            anchorsToRemove.add(target.a());
        }
        if (shouldRemoveAnchor(level, target.b(), 1)) {
            anchorsToRemove.add(target.b());
        }
        if (!player.hasInfiniteMaterials()) {
            final java.util.List<ItemStack> drops = collectAnchorDrops(level, anchorsToRemove);
            if (!canFitAll(player, drops)) {
                return;
            }
            removeConnection(level, target);
            destroyAnchors(level, anchorsToRemove);
            addToInventory(player.getInventory(), drops);
        } else {
            removeConnection(level, target);
            destroyAnchors(level, anchorsToRemove);
        }
    }

    private static @Nullable ConnectionKey resolveTargetedConnection(final @NotNull ServerLevel level, final @NotNull BlockPos pos, final @NotNull Player player) {
        final Vec3 look = player.getLookAngle();
        if (look.lengthSqr() <= 1.0E-7) {
            return selectBestConnectionFromAnchor(level, pos, player);
        }
        final Vec3 eye = player.getEyePosition(1.0F);
        final Vec3 lookDir = look.normalize();
        // Scan the clicked position and its immediate neighbours for any registered structure data.
        // This allows the wrench to target the abstract BigOutline shape without requiring the
        // player to click exactly on a StrutBlock or GirderStrutStructureBlock.
        for (final BlockPos candidate : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            if (!GirderStrutStructureShapes.hasPositionData(level, candidate)) {
                continue;
            }
            final ConnectionKey targeted = GirderStrutStructureShapes.getTargetedConnection(level, candidate, eye, lookDir);
            if (targeted != null) {
                return targeted;
            }
        }
        return selectBestConnectionFromAnchor(level, pos, player);
    }

    private static @Nullable ConnectionKey selectBestConnectionFromAnchor(final @NotNull ServerLevel level, final @NotNull BlockPos pos, final @NotNull Player player) {
        if (!(level.getBlockEntity(pos) instanceof final StrutBlockEntity blockEntity)) {
            return null;
        }
        final Vec3 look = player.getLookAngle();
        if (look.lengthSqr() <= 1.0E-7) {
            return null;
        }
        final Vec3 lookDir = look.normalize();
        final Vec3 anchorCenter = Vec3.atCenterOf(pos);
        double bestDot = -Double.MAX_VALUE;
        ConnectionKey best = null;
        for (final GirderConnectionNode data : blockEntity.getConnectionsCopy()) {
            final BlockPos otherPos = data.absoluteFrom(pos);
            final Vec3 toOther = Vec3.atCenterOf(otherPos).subtract(anchorCenter);
            if (toOther.lengthSqr() <= 1.0E-7) {
                continue;
            }
            final double dot = toOther.normalize().dot(lookDir);
            if (dot > bestDot) {
                bestDot = dot;
                best = new ConnectionKey(pos, otherPos);
            }
        }
        return best;
    }

    private static boolean shouldRemoveAnchor(final @NotNull ServerLevel level, final @NotNull BlockPos pos, final int removedConnections) {
        if (!(level.getBlockEntity(pos) instanceof final StrutBlockEntity other)) {
            return false;
        }
        return other.connectionCount() <= removedConnections;
    }

    private static @NotNull List<ItemStack> collectAnchorDrops(final @NotNull ServerLevel level, final @NotNull Set<BlockPos> anchorsToRemove) {
        final List<ItemStack> drops = new ArrayList<>();
        for (final BlockPos anchorPos : anchorsToRemove) {
            final BlockState anchorState = level.getBlockState(anchorPos);
            if (!(anchorState.getBlock() instanceof StrutBlock)) {
                continue;
            }
            final Item item = anchorState.getBlock().asItem();
            if (item != null) {
                final ItemStack stack = new ItemStack(item);
                if (!stack.isEmpty()) {
                    drops.add(stack);
                }
            }
        }
        return drops;
    }

    private static boolean canFitAll(final @NotNull Player player, final @NotNull List<ItemStack> drops) {
        if (drops.isEmpty()) {
            return true;
        }
        final Inventory inventory = player.getInventory();
        final Inventory simulated = new Inventory(player);
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            simulated.setItem(i, inventory.getItem(i).copy());
        }
        for (final ItemStack stack : drops) {
            if (!simulated.add(stack.copy())) {
                return false;
            }
        }
        return true;
    }

    private static void addToInventory(final @NotNull Inventory inventory, final @NotNull List<ItemStack> drops) {
        for (final ItemStack stack : drops) {
            inventory.add(stack.copy());
        }
    }

    private static void removeConnection(final @NotNull ServerLevel level, final @NotNull ConnectionKey key) {
        GirderStrutStructureShapes.unregisterConnection(level, key.a(), key.b());
        if (level.getBlockEntity(key.a()) instanceof final StrutBlockEntity strutA) {
            strutA.removeConnection(key.b());
        }
        if (level.getBlockEntity(key.b()) instanceof final StrutBlockEntity strutB) {
            strutB.removeConnection(key.a());
        }
    }

    private static void destroyAnchors(final @NotNull ServerLevel level, final @NotNull Set<BlockPos> anchorsToRemove) {
        for (final BlockPos anchorPos : anchorsToRemove) {
            final BlockState anchorState = level.getBlockState(anchorPos);
            if (anchorState.getBlock() instanceof StrutBlock) {
                level.destroyBlock(anchorPos, false);
            }
        }
    }
}
