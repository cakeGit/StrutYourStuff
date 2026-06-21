package com.cake.struts.internal.microliner;

import dev.ryanhcode.sable.companion.ClientSubLevelAccess;
import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.core.BlockPos;

/**
 * Bastardized code under MIT liscence, full credit goes to the Create team
 */
public record MicrolinerParams(float lineWidth, float r, float g, float b, float a, int ticks,
                               ClientSubLevelAccess containingSubLevel) {

    public MicrolinerParams(final float lineWidth,
                            final float r,
                            final float g,
                            final float b,
                            final float a,
                            final int ticks,
                            final BlockPos sublevelReferencePos) {
        this(
                lineWidth,
                r,
                g,
                b,
                a,
                ticks,
                sublevelReferencePos == null ? null : SableCompanion.INSTANCE.getContainingClient(
                        sublevelReferencePos
                )
        );
    }

    public static MicrolinerParams defaultParams() {
        return new MicrolinerParams(1f / 16f, 0.35f, 0.85f, 0.55f, 1f, 2, (ClientSubLevelAccess) null);
    }

    public MicrolinerParams setContainingSubLevel(final ClientSubLevelAccess subLevel) {
        return new MicrolinerParams(this.lineWidth, this.r, this.g, this.b, this.a, this.ticks, subLevel);
    }

    public MicrolinerParams withSubLevelTransform(final BlockPos pos) {
        return new MicrolinerParams(
                this.lineWidth,
                this.r,
                this.g,
                this.b,
                this.a,
                this.ticks,
                SableCompanion.INSTANCE.getContainingClient(
                        pos
                )
        );
    }

}
