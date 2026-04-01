package com.cake.struts.content;

import com.cake.struts.mixin.StrutRenderSystemAccessor;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class StrutDiffuseHelper {

    private static final Vector3fc WORLD_LIGHT_0 = new Vector3f(0.2f, 1.0f, -0.7f).normalize();
    private static final Vector3fc WORLD_LIGHT_1 = new Vector3f(-0.2f, 1.0f, 0.7f).normalize();
    private static final Vector3fc NETHER_WORLD_LIGHT_0 = new Vector3f(0.2f, 1.0f, -0.7f).normalize();
    private static final Vector3fc NETHER_WORLD_LIGHT_1 = new Vector3f(-0.2f, -1.0f, 0.7f).normalize();

    private StrutDiffuseHelper() {
    }

    public static float calculateDiffuse(final Vector3f normal) {
        final Vector3f[] directions = StrutRenderSystemAccessor.struts$getShaderLightDirections();
        return calculateDiffuse(normal, directions[0], directions[1]);
    }

    public static float calculateWorldSpaceDiffuse(final Vector3f normal, final boolean constantAmbientLight) {
        if (constantAmbientLight) {
            return calculateDiffuse(normal, NETHER_WORLD_LIGHT_0, NETHER_WORLD_LIGHT_1);
        }
        return calculateDiffuse(normal, WORLD_LIGHT_0, WORLD_LIGHT_1);
    }

    public static float calculateDiffuse(final Vector3fc normal, final Vector3fc lightDir0, final Vector3fc lightDir1) {
        final float light0 = Math.max(0.0f, lightDir0.dot(normal));
        final float light1 = Math.max(0.0f, lightDir1.dot(normal));
        return Math.min(1.0f, (light0 + light1) * 0.6f + 0.4f);
    }
}
