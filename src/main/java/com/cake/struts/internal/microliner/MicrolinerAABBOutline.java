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
    public void render(final PoseStack poseStack, final MultiBufferSource buffer, final Vec3 camera, final MicrolinerCoordinateTransform transform, final MicrolinerParams params) {
        final VertexConsumer consumer = buffer.getBuffer(RenderType.lines());
        final Vec3 minMinMin = transform.transform(new Vec3(this.box().minX, this.box().minY, this.box().minZ)).subtract(camera);
        final Vec3 minMinMax = transform.transform(new Vec3(this.box().minX, this.box().minY, this.box().maxZ)).subtract(camera);
        final Vec3 minMaxMin = transform.transform(new Vec3(this.box().minX, this.box().maxY, this.box().minZ)).subtract(camera);
        final Vec3 minMaxMax = transform.transform(new Vec3(this.box().minX, this.box().maxY, this.box().maxZ)).subtract(camera);
        final Vec3 maxMinMin = transform.transform(new Vec3(this.box().maxX, this.box().minY, this.box().minZ)).subtract(camera);
        final Vec3 maxMinMax = transform.transform(new Vec3(this.box().maxX, this.box().minY, this.box().maxZ)).subtract(camera);
        final Vec3 maxMaxMin = transform.transform(new Vec3(this.box().maxX, this.box().maxY, this.box().minZ)).subtract(camera);
        final Vec3 maxMaxMax = transform.transform(new Vec3(this.box().maxX, this.box().maxY, this.box().maxZ)).subtract(camera);

        renderEdge(poseStack, consumer, minMinMin, minMinMax, params);
        renderEdge(poseStack, consumer, minMinMax, minMaxMax, params);
        renderEdge(poseStack, consumer, minMaxMax, minMaxMin, params);
        renderEdge(poseStack, consumer, minMaxMin, minMinMin, params);
        renderEdge(poseStack, consumer, maxMinMin, maxMinMax, params);
        renderEdge(poseStack, consumer, maxMinMax, maxMaxMax, params);
        renderEdge(poseStack, consumer, maxMaxMax, maxMaxMin, params);
        renderEdge(poseStack, consumer, maxMaxMin, maxMinMin, params);
        renderEdge(poseStack, consumer, minMinMin, maxMinMin, params);
        renderEdge(poseStack, consumer, minMinMax, maxMinMax, params);
        renderEdge(poseStack, consumer, minMaxMin, maxMaxMin, params);
        renderEdge(poseStack, consumer, minMaxMax, maxMaxMax, params);
    }

    private static void renderEdge(final PoseStack poseStack, final VertexConsumer consumer, final Vec3 from, final Vec3 to, final MicrolinerParams params) {
        MicrolinerOutline.renderLine(poseStack, consumer, from, to, params.r(), params.g(), params.b(), params.a());
    }
}
