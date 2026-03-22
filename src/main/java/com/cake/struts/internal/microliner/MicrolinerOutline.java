package com.cake.struts.internal.microliner;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;

/**
 * "This code is modified and used under MIT licence, full credit goes to the Create / Ponder team."
 */
public interface MicrolinerOutline {
    void render(PoseStack poseStack, MultiBufferSource buffer, Vec3 camera, MicrolinerParams params);

    /**
     * Equivalent to {@link net.minecraft.client.renderer.LevelRenderer#renderLineBox(VertexConsumer, double, double, double, double, double, double, float, float, float, float)} for just a line.
     */
    static void renderLine(final PoseStack poseStack, final VertexConsumer consumer, final Vec3 frompos, final Vec3 toPos, final float r, final float g, final float b, final float a) {
        final PoseStack.Pose last = poseStack.last();
        consumer.vertex(last.pose(), (float) frompos.x, (float) frompos.y, (float) frompos.z).color(r, g, b, a).normal(last.normal(), 0, 1, 0).endVertex();
        consumer.vertex(last.pose(), (float) toPos.x, (float) toPos.y, (float) toPos.z).color(r, g, b, a).normal(last.normal(), 0, 1, 0).endVertex();
    }
}
