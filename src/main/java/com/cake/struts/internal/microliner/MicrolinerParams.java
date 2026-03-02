package com.cake.struts.internal.microliner;

/**
 * "This code is modified and used under MIT licence, full credit goes to the Create / Ponder team."
 */
public record MicrolinerParams(float lineWidth, float r, float g, float b, float a, int ticks) {

    public static MicrolinerParams defaultParams() {
        return new MicrolinerParams(1f / 16f, 0.35f, 0.85f, 0.55f, 1f, 2);
    }
}
