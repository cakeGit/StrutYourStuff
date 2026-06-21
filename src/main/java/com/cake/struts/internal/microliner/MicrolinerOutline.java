package com.cake.struts.internal.microliner;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Bastardized code under MIT liscence, full credit goes to the Create team
 */
public interface MicrolinerOutline {
    void render(PoseStack poseStack,
                @Nullable Pose3dc subLevelRenderPose,
                MultiBufferSource buffer,
                Vec3 camera,
                MicrolinerParams params);

    /**
     * Equivalent to {@link net.minecraft.client.renderer.LevelRenderer#renderLineBox(VertexConsumer, double, double, double, double, double, double, float, float, float, float)} for just a line.
     */
    static void renderLine(final PoseStack poseStack,
                           final Pose3dc subLevelRenderPose, final VertexConsumer consumer,
                           final Vec3 from,
                           final Vec3 to,
                           final Vec3 camera,
                           final float r,
                           final float g,
                           final float b,
                           final float a) {
        final PoseStack.Pose last = poseStack.last();
        final Vec3 fromVertex = (subLevelRenderPose != null ? subLevelRenderPose.transformPosition(from) : from).subtract(
                camera);
        final Vec3 toVertex = (subLevelRenderPose != null ? subLevelRenderPose.transformPosition(to) : to).subtract(
                camera);

        Vec3 delta = to.subtract(from).normalize();
        delta = subLevelRenderPose != null ? subLevelRenderPose.transformNormal(delta) : delta;

        consumer.addVertex(last, (float) fromVertex.x, (float) fromVertex.y, (float) fromVertex.z).setColor(
                r,
                g,
                b,
                a
        ).setNormal(
                last,
                (float) delta.x,
                (float) delta.y,
                (float) delta.z
        );
        consumer.addVertex(last, (float) toVertex.x, (float) toVertex.y, (float) toVertex.z).setColor(
                r,
                g,
                b,
                a
        ).setNormal(
                last,
                (float) delta.x,
                (float) delta.y,
                (float) delta.z
        );
    }
}
