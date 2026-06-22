package com.cake.struts.content;

import com.cake.struts.content.block.StrutBlock;
import com.cake.struts.content.block.StrutBlockEntity;
import com.cake.struts.content.structure.ConnectionKey;
import com.cake.struts.content.structure.GirderStrutStructureShapes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StrutBreakerHelper {
    public static void breakStrut(final @NotNull Player player,
                                  final @NotNull ConnectionKey target,
                                  final boolean isWrench) {
        if (player.level().isClientSide()) return;
        if (!player.mayBuild()) return;
        final ServerLevel level = (ServerLevel) player.level();
        final Set<BlockPos> anchorsToRemove = new HashSet<>();
        if (shouldRemoveAnchor(level, target.a(), 1)) {
            anchorsToRemove.add(target.a());
        }
        if (shouldRemoveAnchor(level, target.b(), 1)) {
            anchorsToRemove.add(target.b());
        }
        if (!player.hasInfiniteMaterials()) {
            final List<ItemStack> drops = collectAnchorDrops(level, anchorsToRemove);
            for (final ItemStack stack : drops) {
                if (!isWrench) {
                    Block.popResource(
                            level,
                            BlockPos.containing(target.a().getCenter().lerp(target.b().getCenter(), 0.5)),
                            stack
                    );
                } else {
                    player.addItem(stack);
                }
            }
        }
        removeConnection(level, target);
        destroyAnchors(level, anchorsToRemove);
    }

    private static boolean shouldRemoveAnchor(final @NotNull ServerLevel level,
                                              final @NotNull BlockPos pos,
                                              final int removedConnections) {
        if (!(level.getBlockEntity(pos) instanceof final StrutBlockEntity other)) {
            return false;
        }
        return other.connectionCount() <= removedConnections;
    }

    private static @NotNull List<ItemStack> collectAnchorDrops(final @NotNull ServerLevel level,
                                                               final @NotNull Set<BlockPos> anchorsToRemove) {
        final List<ItemStack> drops = new ArrayList<>();
        for (final BlockPos anchorPos : anchorsToRemove) {
            final BlockState anchorState = level.getBlockState(anchorPos);
            if (!(anchorState.getBlock() instanceof StrutBlock)) {
                continue;
            }
            final Item item = anchorState.getBlock().asItem();
            final ItemStack stack = new ItemStack(item);
            if (!stack.isEmpty()) {
                drops.add(stack);
            }
        }
        return drops;
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
