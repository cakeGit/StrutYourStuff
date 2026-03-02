package com.cake.struts.internal.microliner;

/**
 * "This code is modified and used under MIT licence, full credit goes to the Create / Ponder team."
 */
public class MicrolinerEntry {

    private final MicrolinerOutline outline;
    private final MicrolinerParams params;
    private int ticksRemaining;

    public MicrolinerEntry(final MicrolinerOutline outline, final MicrolinerParams params) {
        this.outline = outline;
        this.params = params;
        this.ticksRemaining = params.ticks();
    }

    public MicrolinerOutline outline() {
        return outline;
    }

    public MicrolinerParams params() {
        return params;
    }

    public boolean tick() {
        ticksRemaining--;
        return ticksRemaining > 0;
    }
}
