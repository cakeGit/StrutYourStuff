package com.cake.struts.internal.microliner;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;

/**
 * "This code is modified and used under MIT licence, full credit goes to the Create / Ponder team."
 */
public record MicrolinerLineOutline(Vec3 from, Vec3 to) implements MicrolinerOutline {

    @Override
    public void render(final PoseStack poseStack, final MultiBufferSource buffer, final Vec3 camera, final MicrolinerParams params) {
        final VertexConsumer consumer = buffer.getBuffer(RenderType.lines());
        MicrolinerOutline.renderLine(
                poseStack,
                consumer,
                from.subtract(camera),
                to.subtract(camera),
                params.r(),
                params.g(),
                params.b(),
                params.a()
        );
    }
}
