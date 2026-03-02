package com.cake.struts.internal.microliner;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;

/**
 * "This code is modified and used under MIT licence, full credit goes to the Create / Ponder team."
 */
public interface MicrolinerOutline {
    void render(PoseStack poseStack, MultiBufferSource buffer, Vec3 camera, MicrolinerParams params);
}
