package com.cake.struts.content.shape;

import com.cake.struts.internal.microliner.MicrolinerCoordinateTransform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public interface StrutConnectionShape {

    @Nullable
    Vec3 intersect(final Vec3 rayFrom, final Vec3 rayTo);

    default void drawOutline(final PoseStack ms, final VertexConsumer vb, final Vec3 camera) {
        this.drawOutline(ms, vb, camera, 0x66000000);
    }

    default void drawOutline(final PoseStack ms, final VertexConsumer vb, final Vec3 camera, final int color) {
        this.drawOutline(ms, vb, camera, color, MicrolinerCoordinateTransform.identity());
    }

    void drawOutline(final PoseStack ms, final VertexConsumer vb, final Vec3 camera, final int color, final MicrolinerCoordinateTransform transform);
}
