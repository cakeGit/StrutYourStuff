package com.cake.struts.content.mesh;

import com.cake.struts.content.geometry.StrutGeometry;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public final class StrutSegmentMesh {

    private final List<StrutMeshQuad> baseQuads;

    public StrutSegmentMesh(final List<BakedQuad> quads) {
        this.baseQuads = quads.stream()
                .map(StrutMeshQuad::from)
                .toList();
    }

    public List<StrutMeshQuad> forLength(final float length) {
        final int fullSegments = Mth.floor(length + StrutGeometry.EPSILON);
        final float partial = length - fullSegments;

        final List<StrutMeshQuad> result = new ArrayList<>(baseQuads.size() * (fullSegments + 1));

        for (int i = 0; i < fullSegments; i++) {
            for (final StrutMeshQuad quad : baseQuads) {
                result.add(quad.translate(0f, 0f, (float) i));
            }
        }

        if (partial > StrutGeometry.EPSILON) {
            for (final StrutMeshQuad quad : baseQuads) {
                final StrutMeshQuad clipped = quad.clipZ(partial);
                if (clipped != null) {
                    result.add(clipped.translate(0f, 0f, fullSegments));
                }
            }
        }

        if (result.isEmpty()) {
            for (final StrutMeshQuad quad : baseQuads) {
                final StrutMeshQuad fallback = quad.clipZ(Math.max(partial, StrutGeometry.EPSILON));
                if (fallback != null) {
                    result.add(fallback);
                }
            }
        }

        return result;
    }

    public List<StrutMeshQuad> forScaledLength(final float length) {
        final float safeLength = Math.max(length, StrutGeometry.EPSILON);
        final List<StrutMeshQuad> result = new ArrayList<>(baseQuads.size());
        for (final StrutMeshQuad quad : baseQuads) {
            result.add(quad.scaleZWithUv(safeLength));
        }
        return result;
    }
}

