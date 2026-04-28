package com.cake.struts.content;

import com.cake.struts.compat.sable.SableCompat;
import com.cake.struts.internal.microliner.MicrolinerCoordinateTransform;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;

public record StrutPreviewRenderTransforms(MicrolinerCoordinateTransform from,
                                           MicrolinerCoordinateTransform to,
                                           MicrolinerCoordinateTransform connection) {

    public static StrutPreviewRenderTransforms resolve(final ClientLevel level, final BlockPos fromPos, final BlockPos toPos) {
        return SableCompat.resolvePreviewRenderTransforms(level, fromPos, toPos);
    }
}
