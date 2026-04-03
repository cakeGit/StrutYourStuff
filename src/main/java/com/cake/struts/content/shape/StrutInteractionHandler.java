package com.cake.struts.content.shape;

import com.cake.struts.content.CableStrutInfo;
import com.cake.struts.content.StrutModelType;
import com.cake.struts.content.block.StrutBlock;
import com.cake.struts.content.block.StrutBlockEntity;
import com.cake.struts.content.connection.GirderConnectionNode;
import com.cake.struts.content.structure.BlockyStrutLineGeometry;
import com.cake.struts.content.structure.ConnectionKey;
import com.cake.struts.internal.util.LevelSafeStorage;
import com.cake.struts.network.BreakStrutPacket;
import com.cake.struts.registry.StrutItemTags;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

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
    private static @Nullable Vec3 selectedHit;
    private static float breakProgress;
    private static int breakTicks;
    private static @Nullable ConnectionKey breakingKey;
    private static @Nullable BlockPos breakPos;

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
            final CableStrutInfo cableRenderInfo = block.getCableRenderInfo();
            for (final GirderConnectionNode conn : be.getConnectionsCopy()) {
                final BlockPos other = conn.absoluteFrom(pos);
                final BlockyStrutLineGeometry geom = new BlockyStrutLineGeometry(
                        pos, be.getAttachmentDirection(),
                        other, conn.peerFacing(),
                        modelType.shapeSizeXPixels(), modelType.shapeSizeYPixels(),
                        modelType.voxelShapeResolutionPixels()
                );
                final ConnectionKey key = new ConnectionKey(pos, other);
                final CableStrutInfo effectiveRenderInfo = cableRenderInfo != null && be.isTensioned(conn.relativeOffset())
                        ? cableRenderInfo.withZeroSag()
                        : cableRenderInfo;
                store.put(key, cableRenderInfo != null
                        ? new CableStrutConnectionShape(
                        geom.getFromAttachment(), geom.getToAttachment(),
                        geom.getHalfX(), geom.getHalfY(), effectiveRenderInfo
                )
                        : new DefaultStrutConnectionShape(
                        geom.getFromAttachment(), geom.getToAttachment(),
                        geom.getHalfX(), geom.getHalfY(),
                        geom.getFromPos(), geom.getFromFacing(),
                        geom.getToPos(), geom.getToFacing()
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
    public static void onClickInput(final InputEvent.InteractionKeyMappingTriggered event) {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.player == null) return;

        if (selectedKey == null) {
            return;
        }

        final boolean isAttack = event.getKeyMapping() == mc.options.keyAttack;
        if (isAttack) {
            event.setCanceled(true);
            event.setSwingHand(true);
            return;
        }

        final boolean isUse = event.getKeyMapping() == mc.options.keyUse;
        if (isUse && mc.player.isShiftKeyDown() && isActive(mc.player.getMainHandItem())) {
            PacketDistributor.sendToServer(new BreakStrutPacket(selectedKey, true));
            resetBreakProgress(mc.level, mc.player);
            event.setCanceled(true);
            event.setSwingHand(true);
        }
    }

    @SubscribeEvent
    public static void onClientTick(final ClientTickEvent.Post event) {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            clearSelection();
            return;
        }

        final ClientLevel level = mc.level;
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
        Vec3 bestHit = null;

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
                bestHit = hit;
            }
        }

        selectedKey = bestKey;
        selectedShape = bestShape;
        selectedHit = bestHit;
        tickBreakProgress(mc, level, player);
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
        if (selectedKey != null) {
            drawAttachmentOutline(mc.level, ms, vb, camera, selectedKey.a());
            drawAttachmentOutline(mc.level, ms, vb, camera, selectedKey.b());
        }
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
        final Minecraft mc = Minecraft.getInstance();
        resetBreakProgress(mc.level, mc.player);
        selectedKey = null;
        selectedShape = null;
        selectedHit = null;
    }

    private static void tickBreakProgress(final Minecraft mc, final ClientLevel level, final LocalPlayer player) {
        if (!player.getAbilities().mayBuild || selectedKey == null || !mc.options.keyAttack.isDown()) {
            resetBreakProgress(level, player);
            return;
        }

        if (!selectedKey.equals(breakingKey)) {
            resetBreakProgress(level, player);
            breakingKey = selectedKey;
        }

        final ConnectionKey key = breakingKey;
        if (key == null) {
            return;
        }

        final BlockPos currentBreakPos = resolveBreakPos(level, key);
        if (currentBreakPos == null) {
            resetBreakProgress(level, player);
            return;
        }

        if (!currentBreakPos.equals(breakPos)) {
            resetBreakProgress(level, player);
            breakingKey = key;
            breakPos = currentBreakPos;
        }

        final BlockState blockState = level.getBlockState(currentBreakPos);
        if (!(blockState.getBlock() instanceof StrutBlock) || blockState.isAir()) {
            resetBreakProgress(level, player);
            return;
        }

        if (breakTicks % 4 == 0) {
            final SoundType soundType = blockState.getSoundType(level, currentBreakPos, player);
            mc.getSoundManager().play(new SimpleSoundInstance(
                    soundType.getHitSound(),
                    SoundSource.BLOCKS,
                    (soundType.getVolume() + 1.0F) / 8.0F,
                    soundType.getPitch() * 0.5F,
                    level.random,
                    currentBreakPos
            ));
            player.swing(InteractionHand.MAIN_HAND);
        }

        breakTicks++;
        breakProgress += player.getAbilities().instabuild ? 1.0F : blockState.getDestroyProgress(player, level, currentBreakPos);

        final int progress = Math.max(0, Math.min(9, (int) (breakProgress * 10.0F) - 1));
        level.destroyBlockProgress(player.getId(), currentBreakPos, progress);

        final Vec3 particlePos = selectedHit != null ? selectedHit : Vec3.atCenterOf(currentBreakPos);
        for (int i = 0; i < 3; i++) {
            level.addParticle(
                    new BlockParticleOption(ParticleTypes.BLOCK, blockState),
                    particlePos.x,
                    particlePos.y,
                    particlePos.z,
                    (level.random.nextDouble() - 0.5) * 0.08,
                    (level.random.nextDouble() - 0.5) * 0.08,
                    (level.random.nextDouble() - 0.5) * 0.08
            );
        }

        if (breakProgress >= 1.0F) {
            PacketDistributor.sendToServer(new BreakStrutPacket(key, false));
            level.levelEvent(player, 2001, currentBreakPos, Block.getId(blockState));
            resetBreakProgress(level, player);
        }
    }

    private static @Nullable BlockPos resolveBreakPos(final Level level, final ConnectionKey key) {
        final BlockState stateA = level.getBlockState(key.a());
        if (stateA.getBlock() instanceof StrutBlock && !stateA.isAir()) {
            return key.a();
        }
        final BlockState stateB = level.getBlockState(key.b());
        if (stateB.getBlock() instanceof StrutBlock && !stateB.isAir()) {
            return key.b();
        }
        return null;
    }

    private static void drawAttachmentOutline(final ClientLevel level, final PoseStack ms, final VertexConsumer vb,
                                              final Vec3 camera, final BlockPos pos) {
        final BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof StrutBlock)) {
            return;
        }
        final VoxelShape attachmentShape = state.getShape(level, pos);
        attachmentShape.forAllEdges((minX, minY, minZ, maxX, maxY, maxZ) -> drawAttachmentEdge(
                ms, vb, camera,
                minX + pos.getX(), minY + pos.getY(), minZ + pos.getZ(),
                maxX + pos.getX(), maxY + pos.getY(), maxZ + pos.getZ()
        ));
    }

    private static void drawAttachmentEdge(final PoseStack ms, final VertexConsumer vb, final Vec3 camera,
                                           final double startX, final double startY, final double startZ,
                                           final double endX, final double endY, final double endZ) {
        final PoseStack.Pose pose = ms.last();
        final Matrix4f poseMatrix = pose.pose();
        final float ax = (float) (startX - camera.x);
        final float ay = (float) (startY - camera.y);
        final float az = (float) (startZ - camera.z);
        final float bx = (float) (endX - camera.x);
        final float by = (float) (endY - camera.y);
        final float bz = (float) (endZ - camera.z);

        final float dx = bx - ax;
        final float dy = by - ay;
        final float dz = bz - az;
        final float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        final float nx = len > 0 ? dx / len : 0f;
        final float ny = len > 0 ? dy / len : 1f;
        final float nz = len > 0 ? dz / len : 0f;

        vb.addVertex(poseMatrix, ax, ay, az)
                .setColor(0.0F, 0.0F, 0.0F, 0.4F)
                .setNormal(pose.copy(), nx, ny, nz);
        vb.addVertex(poseMatrix, bx, by, bz)
                .setColor(0.0F, 0.0F, 0.0F, 0.4F)
                .setNormal(pose.copy(), nx, ny, nz);
    }

    private static void resetBreakProgress(final @Nullable ClientLevel level, final @Nullable LocalPlayer player) {
        if (level != null && player != null && breakPos != null) {
            level.destroyBlockProgress(player.getId(), breakPos, -1);
        }
        breakProgress = 0.0F;
        breakTicks = 0;
        breakingKey = null;
        breakPos = null;
    }

    private static boolean isActive(final ItemStack stack) {
        return !stack.isEmpty() && stack.is(StrutItemTags.WRENCHES);
    }
}
