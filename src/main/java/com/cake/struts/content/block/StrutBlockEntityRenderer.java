package com.cake.struts.content.block;

import com.cake.struts.compat.flywheel.FlywheelCompatLoader;
import com.cake.struts.content.StrutModelBuilder;
import com.cake.struts.mixin.StrutRenderSystemAccessor;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Vec3i;
import net.minecraft.util.FastColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;
import java.util.function.Function;

public class StrutBlockEntityRenderer implements BlockEntityRenderer<StrutBlockEntity> {

    public StrutBlockEntityRenderer(final BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(final StrutBlockEntity blockEntity, final float partialTick, final PoseStack poseStack, final MultiBufferSource buffer, final int packedLight, final int packedOverlay) {
        if (!(blockEntity.getBlockState().getBlock() instanceof final StrutBlock strutBlock) || blockEntity.getLevel() == null) {
            return;
        }

        if (FlywheelCompatLoader.supportsVisualization(blockEntity.getLevel())) {
            return;
        }

        if (blockEntity.connectionQuadCache == null) {
            blockEntity.connectionQuadCache = StrutModelBuilder.buildConnectionQuads(
                    blockEntity.getLevel(),
                    blockEntity.getBlockPos(),
                    blockEntity.getBlockState(),
                    blockEntity,
                    strutBlock.getModelType());
        }

        final List<BakedQuad> quads = blockEntity.connectionQuadCache;
        if (quads.isEmpty()) {
            return;
        }

        final VertexConsumer consumer = buffer.getBuffer(RenderType.solid());
        final Function<Vector3f, Integer> lighter = blockEntity.createLighter();

        for (final BakedQuad quad : quads) {
            putBulkLitData(consumer, poseStack.last(), quad, new float[]{1.0F, 1.0F, 1.0F, 1.0F}, 1f, 1f, 1f, 1f, lighter, packedOverlay, true);
        }
    }

    @Override
    public @NotNull AABB getRenderBoundingBox(@NotNull final StrutBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(32);
    }

    @Override
    public boolean shouldRender(final StrutBlockEntity blockEntity, final Vec3 cameraPos) {
        return true;
    }


    private static void putBulkLitData(final VertexConsumer consumer,
                                       final PoseStack.Pose p_85988_,
                                       final BakedQuad quads,
                                       final float[] p_331397_,
                                       final float p_85990_,
                                       final float p_85991_,
                                       final float p_85992_,
                                       final float p_331416_,
                                       final Function<Vector3f, Integer> lighter,
                                       final int p_85993_,
                                       final boolean p_331268_) {
        final int[] vertices = quads.getVertices();
        final Vec3i vec3i = quads.getDirection().getNormal();
        final Matrix4f matrix4f = p_85988_.pose();
        final Vector3f vector3f = p_85988_.transformNormal((float) vec3i.getX(), (float) vec3i.getY(), (float) vec3i.getZ(), new Vector3f());
        final int i = 8;
        final int j = vertices.length / 8;
        final int k = (int) (p_331416_ * 255.0F);
        try (final MemoryStack memorystack = MemoryStack.stackPush()) {
            final ByteBuffer bytebuffer = memorystack.malloc(DefaultVertexFormat.BLOCK.getVertexSize());
            final IntBuffer intbuffer = bytebuffer.asIntBuffer();

            for (int l = 0; l < j; ++l) {
                intbuffer.clear();
                intbuffer.put(vertices, l * 8, 8);
                final float f = bytebuffer.getFloat(0);
                final float f1 = bytebuffer.getFloat(4);
                final float f2 = bytebuffer.getFloat(8);
                final float f3;
                final float f4;
                final float f5;
                if (p_331268_) {
                    final float diffuse = calculateDiffuse(vector3f);
                    final float f6 = (float) (bytebuffer.get(12) & 255);
                    final float f7 = (float) (bytebuffer.get(13) & 255);
                    final float f8 = (float) (bytebuffer.get(14) & 255);
                    f3 = f6 * p_331397_[l] * p_85990_ * diffuse;
                    f4 = f7 * p_331397_[l] * p_85991_ * diffuse;
                    f5 = f8 * p_331397_[l] * p_85992_ * diffuse;
                } else {
                    f3 = p_331397_[l] * p_85990_ * 255.0F;
                    f4 = p_331397_[l] * p_85991_ * 255.0F;
                    f5 = p_331397_[l] * p_85992_ * 255.0F;
                }

                final int vertexAlpha = p_331268_ ? (int) (p_331416_ * (float) (bytebuffer.get(15) & 255) / 255.0F * 255.0F) : k;
                final int i1 = FastColor.ARGB32.color(vertexAlpha, (int) f3, (int) f4, (int) f5);
                final float f10 = bytebuffer.getFloat(16);
                final float f9 = bytebuffer.getFloat(20);
                final Vector3f worldPos = new Vector3f(f, f1, f2);
                final int j1 = consumer.applyBakedLighting(lighter.apply(worldPos), bytebuffer);
                final Vector3f vector3f1 = matrix4f.transformPosition(worldPos);
                consumer.applyBakedNormals(vector3f, bytebuffer, p_85988_.normal());
                consumer.addVertex(vector3f1.x(), vector3f1.y(), vector3f1.z(), i1, f10, f9, p_85993_, j1, vector3f.x(), vector3f.y(), vector3f.z());
            }
        }
    }

    /**
     * If your friends jumped off a cliff, would you do it too?
     */
    private static float calculateDiffuse(final Vector3f normal) {
        return calculateDiffuse(normal, StrutRenderSystemAccessor.struts$getShaderLightDirections()[0], StrutRenderSystemAccessor.struts$getShaderLightDirections()[1]);
    }

    // Adapted from minecraft:shaders/include/light.glsl (and subsequently stolen from ShadeSeparatingSuperByteBuffer because these girders refuse to shade)
    private static float calculateDiffuse(final Vector3fc normal, final Vector3fc lightDir0, final Vector3fc lightDir1) {
        final float light0 = Math.max(0.0f, lightDir0.dot(normal));
        final float light1 = Math.max(0.0f, lightDir1.dot(normal));
        return Math.min(1.0f, (light0 + light1) * 0.6f + 0.4f);
    }

}

