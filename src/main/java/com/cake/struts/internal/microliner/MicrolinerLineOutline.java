package com.cake.struts.internal.microliner;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;

/**
 * Bastardized code under MIT liscence, full credit goes to the Create team
 */
public record MicrolinerLineOutline(Vec3 from, Vec3 to) implements MicrolinerOutline {

    @Override
    public void render(final PoseStack poseStack,
                       final Pose3dc subLevelRenderPose, final MultiBufferSource buffer,
                       final Vec3 camera,
                       final MicrolinerParams params) {
        final VertexConsumer consumer = buffer.getBuffer(RenderType.lines());
        MicrolinerOutline.renderLine(
                poseStack,
                subLevelRenderPose,
                consumer,
                this.from(),
                this.to(),
                camera, params.r(),
                params.g(),
                params.b(),
                params.a()
        );
    }
}
