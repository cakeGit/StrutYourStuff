package com.cake.struts.content;

import com.cake.struts.content.block.StrutBlock;
import com.cake.struts.content.block.StrutBlockItem;
import com.cake.struts.content.structure.BlockyStrutLineGeometry;
import com.cake.struts.internal.microliner.Microliner;
import com.cake.struts.internal.microliner.MicrolinerParams;
import com.cake.struts.registry.StrutDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

public class StrutPlacementEffects {

    public static void tick(final LocalPlayer player) {
        if (Minecraft.getInstance().isPaused() || Minecraft.getInstance().hitResult == null) return;

        //Get held item
        final ItemStack heldItem = player.getMainHandItem().getItem() instanceof StrutBlockItem ? player.getMainHandItem() :
                player.getOffhandItem().getItem() instanceof StrutBlockItem ? player.getOffhandItem() : null;
        if (heldItem != null) {
            display(player, heldItem);
        }
    }

    private static void display(final LocalPlayer player, final ItemStack heldItem) {
        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        final BlockPos fromPos = heldItem.get(StrutDataComponents.GIRDER_STRUT_FROM);
        final Direction fromFace = heldItem.get(StrutDataComponents.GIRDER_STRUT_FROM_FACE);

        if (fromPos == null) {
            return;
        }

        final HitResult genericHit = Minecraft.getInstance().hitResult;
        if (!(genericHit instanceof final BlockHitResult hit) || genericHit.getType() != HitResult.Type.BLOCK) {
            return;
        }

        final BlockPos targetPos = resolvePlacementPos(level, hit.getBlockPos(), hit.getDirection());
        if (targetPos == null || targetPos.distSqr(fromPos) > StrutBlock.MAX_SPAN * StrutBlock.MAX_SPAN * 1.5) {
            return;
        }

        Direction targetFace = hit.getDirection();

        final BlockState targetState = level.getBlockState(targetPos);
        if (targetState.getBlock() instanceof StrutBlock) {
            targetFace = targetState.getValue(StrutBlock.FACING);
        }

        final Vec3 renderFrom = Vec3.atCenterOf(fromPos);
        final Vec3 renderTo = Vec3.atCenterOf(targetPos);

        final Vec3 delta = renderTo.subtract(renderFrom);
        final double length = delta.length();
        if (length < 1.0E-3 || length > StrutBlock.MAX_SPAN * 3) {
            return;
        }

        final boolean valid = StrutBlockItem.isValidConnection(level, fromPos, fromFace, targetPos, targetFace);

        // 95CD41 valid and EA5C2B invalid
        final Vector3f outlinerColor = valid ? new Vector3f(.35f, .85f, .55f) : new Vector3f(.85f, .35f, .55f);
        final StrutModelType modelType = resolveModelType(heldItem);
        final BlockyStrutLineGeometry lineGeometry = new BlockyStrutLineGeometry(fromPos, fromFace, targetPos, targetFace, modelType.shapeSizeXPixels(),
                modelType.shapeSizeYPixels(), modelType.voxelShapeResolutionPixels());

        showAnchorOutline(fromPos, fromFace, "from", outlinerColor.x, outlinerColor.y, outlinerColor.z);
        showAnchorOutline(targetPos, targetFace, "to", outlinerColor.x, outlinerColor.y, outlinerColor.z);
        showConnectionShape(lineGeometry, outlinerColor.x, outlinerColor.y, outlinerColor.z);

    }

    private static void showAnchorOutline(final BlockPos targetPos, final Direction targetFace, final String id, final float r, final float g, final float b) {
        final AABB localBounds = StrutBlock.getAttachmentBaseShape(targetFace).bounds();
        final AABB worldBounds = localBounds.move(targetPos.getX(), targetPos.getY(), targetPos.getZ());
        Microliner.get().showAABB("strut_preview_anchor_" + id, worldBounds, new MicrolinerParams(1 / 16f, r, g, b, 1f, 2));
    }

    private static void showConnectionShape(final BlockyStrutLineGeometry lineGeometry, final float r, final float g, final float b) {
        final BlockPos[] positions = lineGeometry.getPositions();
        for (int i = 0; i < positions.length; i++) {
            final BlockPos pos = positions[i];
            final VoxelShape shape = lineGeometry.getShapeForPosition(pos);
            if (shape.isEmpty()) {
                continue;
            }
            final int boxCount = shape.toAabbs().size();
            for (int j = 0; j < boxCount; j++) {
                final AABB localBox = shape.toAabbs().get(j);
                final AABB worldBox = localBox.move(pos.getX(), pos.getY(), pos.getZ());
                Microliner.get().showAABB("strut_preview_shape_" + i + '_' + j, worldBox, new MicrolinerParams(1 / 16f, r, g, b, 1f, 2));
            }
        }
    }

    private static StrutModelType resolveModelType(final ItemStack heldItem) {
        if (heldItem.getItem() instanceof final BlockItem blockItem && blockItem.getBlock() instanceof final StrutBlock strutBlock) {
            return strutBlock.getModelType();
        }
        return new StrutModelType(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("struts", "block/girder_strut/girder_strut_segment"),
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("struts", "block/industrial_iron_block")
        );
    }

    private static BlockPos resolvePlacementPos(final ClientLevel level, final BlockPos clickedPos, final Direction face) {
        BlockPos pos = clickedPos;
        if (!(level.getBlockState(pos).getBlock() instanceof StrutBlock)) {
            pos = pos.relative(face);
            if (!(level.getBlockState(pos).canBeReplaced() || level.getBlockState(pos).getBlock() instanceof StrutBlock)) {
                return null;
            }
        }
        return pos;
    }

}

