package com.cake.struts.content;

/**
 * Rendering parameters for cable struts.
 *
 * @param sag                 Sag amount as a length multiplier (scaled by total span length).
 * @param tangentDotThreshold Dot product threshold for merging segments (closer to 1 = fewer segments).
 * @param minStep             Minimum sampling step size in blocks.
 * @param maxSegments         Maximum number of segments for long spans.
 */
public record CableStrutInfo(float sag, float tangentDotThreshold, float minStep, int maxSegments) {

    public CableStrutInfo(final float sag) {
        this(sag, 0.995f, 0.5f, 64);
    }

    public CableStrutInfo withZeroSag() {
        return new CableStrutInfo(0, this.tangentDotThreshold(), this.minStep(), this.maxSegments());
    }
}
