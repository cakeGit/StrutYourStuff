package com.cake.struts.content.shape;

import com.cake.struts.content.StrutModelType;
import com.cake.struts.content.block.StrutBlock;
import com.cake.struts.content.block.StrutBlockEntity;
import com.cake.struts.content.connection.GirderConnectionNode;
import com.cake.struts.content.structure.BlockyStrutLineGeometry;
import com.cake.struts.content.structure.ConnectionKey;
import com.cake.struts.internal.util.LevelSafeStorage;
import com.cake.struts.registry.StrutItemTags;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Client-side handler for strut connection selection and BigOutline rendering.
 * <p>
 * Call {@link #updateOutlineShapes} whenever a {@link StrutBlockEntity} receives
 * updated connection data on the client, and {@link #removeOutlineShapes} when
 * the block entity is removed.
 */
@EventBusSubscriber(modid = "struts", value = Dist.CLIENT)
public class StrutInteractionHandler {

    private static final LevelSafeStorage<LevelStrutOutlineStore> STORES =
            new LevelSafeStorage<>($ -> new LevelStrutOutlineStore());

    public static @Nullable ConnectionKey selectedKey;
    public static @Nullable StrutConnectionShape selectedShape;

    /**
     * Rebuilds all outline shapes for the connections owned by the given
     * {@link StrutBlockEntity}.  Must be called on the client thread.
     */
    public static void updateOutlineShapes(final Level level, final StrutBlockEntity be) {
        if (be.getBlockState().getBlock() instanceof final StrutBlock block) {
            final LevelStrutOutlineStore store = STORES.getForLevel(level);
            final BlockPos pos = be.getBlockPos();

            // Remove stale shapes for this block first so we don't accumulate entries on reconnect.
            store.removeAllFor(pos);

            // Cable struts have no collision geometry, but they should still have a BigOutline
            // (no early return here so cables get their outline)

            final StrutModelType modelType = be.getModelType();
            for (final GirderConnectionNode conn : be.getConnectionsCopy()) {
                final BlockPos other = conn.absoluteFrom(pos);
                final BlockyStrutLineGeometry geom = new BlockyStrutLineGeometry(
                        pos, be.getAttachmentDirection(),
                        other, conn.peerFacing(),
                        modelType.shapeSizeXPixels(), modelType.shapeSizeYPixels(),
                        modelType.voxelShapeResolutionPixels()
                );
                final ConnectionKey key = new ConnectionKey(pos, other);
                store.put(key, new StrutConnectionShape(
                        geom.getFromAttachment(), geom.getToAttachment(),
                        geom.getHalfX(), geom.getHalfY()
                ));
            }
        }
    }

    /**
     * Removes all outline shapes associated with a block position.
     * Must be called on the client thread.
     */
    public static void removeOutlineShapes(final Level level, final BlockPos pos) {
        STORES.getForLevel(level).removeAllFor(pos);
        if (selectedKey != null && (selectedKey.a().equals(pos) || selectedKey.b().equals(pos))) {
            clearSelection();
        }
    }

    @SubscribeEvent
    public static void onClickInput(final net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered event) {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.player == null) return;

        if (selectedKey != null) {
            final boolean isAttack = event.getKeyMapping() == mc.options.keyAttack;
            final boolean isUse = event.getKeyMapping() == mc.options.keyUse;

            if (isAttack || (isUse && mc.player.isShiftKeyDown())) {
                final boolean isWrench = isUse && isActive(mc.player.getMainHandItem());
                if (isAttack || isWrench) {
                    net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.cake.struts.network.BreakStrutPacket(selectedKey, isWrench));
                    event.setCanceled(true);
                    event.setSwingHand(true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(final ClientTickEvent.Post event) {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            clearSelection();
            return;
        }

        final Level level = mc.level;
        final LocalPlayer player = mc.player;

        // We want the BigOutline to highlight with ANY item, just like a normal block's VoxelShape would.
        // Therefore, we do not require isActive() to check raycasting.
        final LevelStrutOutlineStore store = STORES.getForLevel(level);
        store.validate(level);

        final Vec3 eye = player.getEyePosition();
        final double range = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) + 1.0;
        final Vec3 look = player.getLookAngle();
        final Vec3 traceTarget = eye.add(look.scale(range));

        double bestDistanceSq = Double.MAX_VALUE;
        ConnectionKey bestKey = null;
        StrutConnectionShape bestShape = null;

        for (final Map.Entry<ConnectionKey, StrutConnectionShape> entry : store.entries()) {
            final Vec3 hit = entry.getValue().intersect(eye, traceTarget);
            if (hit == null) {
                continue;
            }
            final double distSq = eye.distanceToSqr(hit);
            if (distSq < bestDistanceSq) {
                bestDistanceSq = distSq;
                bestKey = entry.getKey();
                bestShape = entry.getValue();
            }
        }

        selectedKey = bestKey;
        selectedShape = bestShape;
    }

    @SubscribeEvent
    public static void onRenderWorld(final RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        if (selectedShape == null) {
            return;
        }

        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            clearSelection();
            return;
        }

        final Vec3 camera = event.getCamera().getPosition();
        final MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        final VertexConsumer vb = buffer.getBuffer(RenderType.lines());

        final PoseStack ms = event.getPoseStack();
        ms.pushPose();
        selectedShape.drawOutline(ms, vb, camera);
        ms.popPose();

        buffer.endBatch(RenderType.lines());
    }

    @SubscribeEvent
    public static void hideVanillaBlockSelection(final RenderHighlightEvent.Block event) {
        if (selectedShape != null) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(final LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            clearSelection();
        }
    }
    
    private static void clearSelection() {
        selectedKey = null;
        selectedShape = null;
    }

    private static boolean isActive(final ItemStack stack) {
        return !stack.isEmpty() && stack.is(StrutItemTags.WRENCHES);
    }
}
