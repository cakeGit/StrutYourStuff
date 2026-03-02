package com.cake.struts.internal.util;

import net.minecraft.world.phys.Vec3;

public class BakedQuadHelper {

    public static final int VERTEX_STRIDE = 8;
    public static final int COLOR_OFFSET = 3;
    public static final int UV_OFFSET = 4;
    public static final int LIGHT_OFFSET = 6;
    public static final int NORMAL_OFFSET = 7;

    public static Vec3 getXYZ(final int[] data, final int vertex) {
        final int i = vertex * VERTEX_STRIDE;
        return new Vec3(Float.intBitsToFloat(data[i]), Float.intBitsToFloat(data[i + 1]), Float.intBitsToFloat(data[i + 2]));
    }

    public static Vec3 getNormalXYZ(final int[] data, final int vertex) {
        final int packed = data[vertex * VERTEX_STRIDE + NORMAL_OFFSET];
        final float x = (byte) (packed & 255) / 127.0f;
        final float y = (byte) ((packed >> 8) & 255) / 127.0f;
        final float z = (byte) ((packed >> 16) & 255) / 127.0f;
        return new Vec3(x, y, z);
    }

    public static float getU(final int[] data, final int vertex) {
        return Float.intBitsToFloat(data[vertex * VERTEX_STRIDE + UV_OFFSET]);
    }

    public static float getV(final int[] data, final int vertex) {
        return Float.intBitsToFloat(data[vertex * VERTEX_STRIDE + UV_OFFSET + 1]);
    }

    public static void setXYZ(final int[] data, final int vertex, final Vec3 pos) {
        final int i = vertex * VERTEX_STRIDE;
        data[i] = Float.floatToRawIntBits((float) pos.x);
        data[i + 1] = Float.floatToRawIntBits((float) pos.y);
        data[i + 2] = Float.floatToRawIntBits((float) pos.z);
    }

    public static void setNormalXYZ(final int[] data, final int vertex, final Vec3 normal) {
        final int nx = ((int) Math.round(normal.x * 127.0)) & 255;
        final int ny = ((int) Math.round(normal.y * 127.0)) & 255;
        final int nz = ((int) Math.round(normal.z * 127.0)) & 255;
        data[vertex * VERTEX_STRIDE + NORMAL_OFFSET] = nx | (ny << 8) | (nz << 16);
    }

    public static void setU(final int[] data, final int vertex, final float u) {
        data[vertex * VERTEX_STRIDE + UV_OFFSET] = Float.floatToRawIntBits(u);
    }

    public static void setV(final int[] data, final int vertex, final float v) {
        data[vertex * VERTEX_STRIDE + UV_OFFSET + 1] = Float.floatToRawIntBits(v);
    }

    private BakedQuadHelper() {
    }
}
