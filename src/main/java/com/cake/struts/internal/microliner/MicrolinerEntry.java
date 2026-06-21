package com.cake.struts.internal.microliner;

/**
 * Bastardized code under MIT liscence, full credit goes to the Create team
 */
public class MicrolinerEntry {

    private final MicrolinerOutline outline;
    private final MicrolinerParams params;
    private int ticksRemaining;

    public MicrolinerEntry(final MicrolinerOutline outline,
                           final MicrolinerParams params) {
        this.outline = outline;
        this.params = params;
        this.ticksRemaining = params.ticks();
    }

    public MicrolinerOutline outline() {
        return this.outline;
    }

    public MicrolinerParams params() {
        return this.params;
    }

    public boolean tick() {
        this.ticksRemaining--;
        return this.ticksRemaining > 0;
    }
}
