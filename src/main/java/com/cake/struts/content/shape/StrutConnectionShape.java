package com.cake.struts.content.shape;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ryanhcode.sable.companion.ClientSubLevelAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public interface StrutConnectionShape {

    @Nullable
    Vec3 intersect(final Vec3 rayFrom, final Vec3 rayTo);

    void drawOutline(final PoseStack ms,
                     final VertexConsumer vb,
                     final Vec3 camera,
                     final int color,
                     final ClientSubLevelAccess clientSubLevelAccess);

    BlockPos getSubLevelReferencePosition();

}
