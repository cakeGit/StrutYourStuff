package com.cake.struts.internal.microliner;

/**
 * "This code is modified and used under MIT licence, full credit goes to the Create / Ponder team."
 */
public class MicrolinerEntry {

    private final MicrolinerOutline outline;
    private final MicrolinerCoordinateTransform transform;
    private final MicrolinerParams params;
    private int ticksRemaining;

    public MicrolinerEntry(final MicrolinerOutline outline, final MicrolinerCoordinateTransform transform, final MicrolinerParams params) {
        this.outline = outline;
        this.transform = transform;
        this.params = params;
        this.ticksRemaining = params.ticks();
    }

    public MicrolinerOutline outline() {
        return this.outline;
    }

    public MicrolinerCoordinateTransform transform() {
        return this.transform;
    }

    public MicrolinerParams params() {
        return this.params;
    }

    public boolean tick() {
        this.ticksRemaining--;
        return this.ticksRemaining > 0;
    }
}
