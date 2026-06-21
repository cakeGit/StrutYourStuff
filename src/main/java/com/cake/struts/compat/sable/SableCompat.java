package com.cake.struts.compat.sable;

import com.cake.struts.content.StrutPreviewRenderTransforms;
import com.cake.struts.internal.microliner.MicrolinerCoordinateTransform;
import dev.ryanhcode.sable.companion.ClientSubLevelAccess;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.UUID;

//TODO: fix since this is clearly broken
public class SableCompat {

    public static boolean isInSubLevel(final Level level, final BlockPos pos) {
        return SableCompanion.INSTANCE.getContaining(level, pos) != null;
    }

    public static StrutPreviewRenderTransforms resolvePreviewRenderTransforms(final ClientLevel level,
                                                                              final BlockPos fromPos,
                                                                              final BlockPos toPos) {
        final ResolvedSubLevel fromSubLevel = resolveSubLevel(level, fromPos);
        final ResolvedSubLevel toSubLevel = resolveSubLevel(level, toPos);
        final MicrolinerCoordinateTransform fromTransform = transformFor(fromSubLevel);
        final MicrolinerCoordinateTransform toTransform = transformFor(toSubLevel);
        final MicrolinerCoordinateTransform connectionTransform = connectionTransform(
                fromSubLevel,
                toSubLevel,
                fromTransform
        );
        return new StrutPreviewRenderTransforms(fromTransform, toTransform, connectionTransform);
    }

    private static ResolvedSubLevel resolveSubLevel(final ClientLevel level, final BlockPos pos) {
        final SubLevelAccess subLevel = SableCompanion.INSTANCE.getContaining(level, pos);
        if (!(subLevel instanceof final ClientSubLevelAccess clientSubLevel)) {
            return null;
        }
        return new ResolvedSubLevel(clientSubLevel.getUniqueId(), clientSubLevel.renderPose()::transformPosition);
    }

    private static MicrolinerCoordinateTransform transformFor(final ResolvedSubLevel subLevel) {
        if (subLevel == null) {
            return MicrolinerCoordinateTransform.identity();
        }
        return subLevel.transform();
    }

    private static MicrolinerCoordinateTransform connectionTransform(final ResolvedSubLevel fromSubLevel,
                                                                     final ResolvedSubLevel toSubLevel,
                                                                     final MicrolinerCoordinateTransform fromTransform) {
        if (fromSubLevel != null && toSubLevel != null && fromSubLevel.uniqueId().equals(toSubLevel.uniqueId())) {
            return fromTransform;
        }
        return MicrolinerCoordinateTransform.identity();
    }

    private record ResolvedSubLevel(UUID uniqueId, MicrolinerCoordinateTransform transform) {
    }
}
