package com.cake.struts.internal.microliner;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Bastardized code under MIT liscence, full credit goes to the Create team
 */
public record MicrolinerAABBOutline(AABB box) implements MicrolinerOutline {

    @Override
    public void render(final PoseStack poseStack,
                       final Pose3dc subLevelRenderPose,
                       final MultiBufferSource buffer,
                       final Vec3 camera,
                       final MicrolinerParams params) {
        final VertexConsumer consumer = buffer.getBuffer(RenderType.lines());
        final Vec3 minMinMin = new Vec3(
                this.box().minX,
                this.box().minY,
                this.box().minZ
        );
        final Vec3 minMinMax = new Vec3(
                this.box().minX,
                this.box().minY,
                this.box().maxZ
        );
        final Vec3 minMaxMin = new Vec3(
                this.box().minX,
                this.box().maxY,
                this.box().minZ
        );
        final Vec3 minMaxMax = new Vec3(
                this.box().minX,
                this.box().maxY,
                this.box().maxZ
        );
        final Vec3 maxMinMin = new Vec3(
                this.box().maxX,
                this.box().minY,
                this.box().minZ
        );
        final Vec3 maxMinMax = new Vec3(
                this.box().maxX,
                this.box().minY,
                this.box().maxZ
        );
        final Vec3 maxMaxMin = new Vec3(
                this.box().maxX,
                this.box().maxY,
                this.box().minZ
        );
        final Vec3 maxMaxMax = new Vec3(
                this.box().maxX,
                this.box().maxY,
                this.box().maxZ
        );

        renderEdge(poseStack, subLevelRenderPose, consumer, minMinMin, minMinMax, params, camera);
        renderEdge(poseStack, subLevelRenderPose, consumer, minMinMax, minMaxMax, params, camera);
        renderEdge(poseStack, subLevelRenderPose, consumer, minMaxMax, minMaxMin, params, camera);
        renderEdge(poseStack, subLevelRenderPose, consumer, minMaxMin, minMinMin, params, camera);
        renderEdge(poseStack, subLevelRenderPose, consumer, maxMinMin, maxMinMax, params, camera);
        renderEdge(poseStack, subLevelRenderPose, consumer, maxMinMax, maxMaxMax, params, camera);
        renderEdge(poseStack, subLevelRenderPose, consumer, maxMaxMax, maxMaxMin, params, camera);
        renderEdge(poseStack, subLevelRenderPose, consumer, maxMaxMin, maxMinMin, params, camera);
        renderEdge(poseStack, subLevelRenderPose, consumer, minMinMin, maxMinMin, params, camera);
        renderEdge(poseStack, subLevelRenderPose, consumer, minMinMax, maxMinMax, params, camera);
        renderEdge(poseStack, subLevelRenderPose, consumer, minMaxMin, maxMaxMin, params, camera);
        renderEdge(poseStack, subLevelRenderPose, consumer, minMaxMax, maxMaxMax, params, camera);
    }

    private static void renderEdge(final PoseStack poseStack,
                                   final Pose3dc subLevelRenderPose, final VertexConsumer consumer,
                                   final Vec3 from,
                                   final Vec3 to,
                                   final MicrolinerParams params,
                                   final Vec3 camera) {
        MicrolinerOutline.renderLine(
                poseStack,
                subLevelRenderPose,
                consumer,
                from,
                to,
                camera,
                params.r(),
                params.g(),
                params.b(),
                params.a()
        );
    }
}
