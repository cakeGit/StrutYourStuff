package com.cake.struts.internal.microliner;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * "This code is modified and used under MIT licence, full credit goes to the Create / Ponder team."
 */
public record MicrolinerAABBOutline(AABB box) implements MicrolinerOutline {

    @Override
    public void render(final PoseStack poseStack, final MultiBufferSource buffer, final Vec3 camera, final MicrolinerParams params) {
        final VertexConsumer consumer = buffer.getBuffer(RenderType.lines());
        final AABB shifted = box.move(-camera.x, -camera.y, -camera.z);
        net.minecraft.client.renderer.LevelRenderer.renderLineBox(
                poseStack,
                consumer,
                shifted,
                params.r(),
                params.g(),
                params.b(),
                params.a());
    }
}
