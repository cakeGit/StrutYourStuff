package com.cake.struts.content.shape;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side OBB (oriented bounding box) representing one strut connection for
 * ray-casting and outline rendering.  Stored in world-space coordinates.
 */
public class StrutConnectionShape {

    private static final double INFLATE_PIXELS = 1.0 / 16.0;
    private static final double TANGENT_EXTENSION_PIXELS = 0.5 / 16.0;
    private static final double INTERSECTION_EPSILON = 1e-9;
    private static final double MIN_LENGTH_SQR = 1e-12;

    private final Vec3 fromAttachment;
    private final Vec3 toAttachment;
    private final double halfWidth;
    private final double halfHeight;

    // Pre-computed OBB frame (world-space)
    private final Vec3 tangent;
    private final Vec3 uAxis;
    private final Vec3 vAxis;
    private final Vec3 center;
    private final double halfLength;

    public StrutConnectionShape(final Vec3 fromAttachment, final Vec3 toAttachment,
                                final double halfWidth, final double halfHeight) {
        this.fromAttachment = fromAttachment;
        this.toAttachment = toAttachment;
        this.halfWidth = halfWidth + INFLATE_PIXELS;
        this.halfHeight = halfHeight + INFLATE_PIXELS;

        final Vec3 span = toAttachment.subtract(fromAttachment);
        final double spanLen = span.length();
        this.halfLength = spanLen * 0.5 + TANGENT_EXTENSION_PIXELS;
        this.center = fromAttachment.add(span.scale(0.5));

        if (spanLen > Math.sqrt(MIN_LENGTH_SQR)) {
            this.tangent = span.normalize();
        } else {
            this.tangent = new Vec3(1, 0, 0);
        }

        Vec3 u = new Vec3(0, 1, 0).cross(tangent);
        if (u.lengthSqr() < MIN_LENGTH_SQR) {
            u = new Vec3(1, 0, 0).cross(tangent);
        }
        this.uAxis = u.normalize();
        this.vAxis = tangent.cross(uAxis).normalize();
    }

    /**
     * Returns the world-space intersection point of the given ray with this OBB,
     * or {@code null} if there is no intersection.
     */
    @Nullable
    public Vec3 intersect(final Vec3 rayFrom, final Vec3 rayTo) {
        final Vec3 rayDir = rayTo.subtract(rayFrom);
        if (rayDir.lengthSqr() < MIN_LENGTH_SQR) {
            return null;
        }

        final double t = intersectRayWithObb(rayFrom, rayDir, center, tangent, uAxis, vAxis,
                halfLength, halfWidth, halfHeight);
        if (Double.isNaN(t)) {
            return null;
        }

        return rayFrom.add(rayDir.scale(t));
    }

    /**
     * Draws a box outline around this strut connection, translated by {@code -camera}.
     */
    public void drawOutline(final PoseStack ms, final VertexConsumer vb, final Vec3 camera) {
        final float hw = (float) halfWidth;
        final float hh = (float) halfHeight;

        final List<Vec3> startCorners = getCorners(fromAttachment, uAxis, vAxis, hw, hh);
        final List<Vec3> endCorners = getCorners(toAttachment, uAxis, vAxis, hw, hh);

        final int color = 0x66000000;
        for (int i = 0; i < 4; i++) {
            line(vb, ms, startCorners.get(i).subtract(camera), endCorners.get(i).subtract(camera), color);
        }

        drawRing(vb, ms,
                startCorners.get(0).subtract(camera),
                startCorners.get(1).subtract(camera),
                startCorners.get(2).subtract(camera),
                startCorners.get(3).subtract(camera),
                color);

        drawRing(vb, ms,
                endCorners.get(0).subtract(camera),
                endCorners.get(1).subtract(camera),
                endCorners.get(2).subtract(camera),
                endCorners.get(3).subtract(camera),
                color);
    }

    // ----- private helpers -----

    private static double intersectRayWithObb(final Vec3 rayOrigin, final Vec3 rayDir,
                                              final Vec3 boxCenter,
                                              final Vec3 axisX, final Vec3 axisY, final Vec3 axisZ,
                                              final double halfX, final double halfY, final double halfZ) {
        final Vec3 p = rayOrigin.subtract(boxCenter);
        final double[] range = {0.0, 1.0};

        if (!clipAxis(p.dot(axisX), rayDir.dot(axisX), halfX, range)) return Double.NaN;
        if (!clipAxis(p.dot(axisY), rayDir.dot(axisY), halfY, range)) return Double.NaN;
        if (!clipAxis(p.dot(axisZ), rayDir.dot(axisZ), halfZ, range)) return Double.NaN;

        if (range[1] < 0.0 || range[0] > 1.0) return Double.NaN;

        final boolean originInside = Math.abs(p.dot(axisX)) <= halfX
                && Math.abs(p.dot(axisY)) <= halfY
                && Math.abs(p.dot(axisZ)) <= halfZ;

        final double entry = Mth.clamp(range[0], 0.0, 1.0);
        final double exit = Mth.clamp(range[1], 0.0, 1.0);
        return originInside ? exit : entry;
    }

    private static boolean clipAxis(final double originProj, final double dirProj,
                                    final double halfExtent, final double[] range) {
        if (Math.abs(dirProj) < INTERSECTION_EPSILON) {
            return Math.abs(originProj) <= halfExtent;
        }
        double tMin = (-halfExtent - originProj) / dirProj;
        double tMax = (halfExtent - originProj) / dirProj;
        if (tMin > tMax) {
            final double tmp = tMin;
            tMin = tMax;
            tMax = tmp;
        }
        range[0] = Math.max(range[0], tMin);
        range[1] = Math.min(range[1], tMax);
        return range[0] <= range[1];
    }

    private static List<Vec3> getCorners(final Vec3 center, final Vec3 u, final Vec3 v,
                                         final float hw, final float hh) {
        final Vec3 us = u.scale(hw);
        final Vec3 vs = v.scale(hh);
        final List<Vec3> corners = new ArrayList<>(4);
        corners.add(center.add(us).add(vs));
        corners.add(center.add(us).subtract(vs));
        corners.add(center.subtract(us).subtract(vs));
        corners.add(center.subtract(us).add(vs));
        return corners;
    }

    private static void drawRing(final VertexConsumer vb, final PoseStack ms,
                                 final Vec3 a, final Vec3 b, final Vec3 c, final Vec3 d,
                                 final int color) {
        line(vb, ms, a, b, color);
        line(vb, ms, b, c, color);
        line(vb, ms, c, d, color);
        line(vb, ms, d, a, color);
    }

    private static void line(final VertexConsumer vb, final PoseStack ms,
                              final Vec3 a, final Vec3 b, final int color) {
        final PoseStack.Pose pose = ms.last();
        final Matrix4f poseMatrix = pose.pose();

        final float dx = (float) (b.x - a.x);
        final float dy = (float) (b.y - a.y);
        final float dz = (float) (b.z - a.z);
        final float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        final float nx = len > 0 ? dx / len : 0f;
        final float ny = len > 0 ? dy / len : 1f;
        final float nz = len > 0 ? dz / len : 0f;

        final float r = ((color >> 16) & 0xFF) / 255f;
        final float g = ((color >> 8) & 0xFF) / 255f;
        final float bl = (color & 0xFF) / 255f;
        final float alpha = ((color >> 24) & 0xFF) / 255f;

        vb.addVertex(poseMatrix, (float) a.x, (float) a.y, (float) a.z)
                .setColor(r, g, bl, alpha)
                .setNormal(pose.copy(), nx, ny, nz);
        vb.addVertex(poseMatrix, (float) b.x, (float) b.y, (float) b.z)
                .setColor(r, g, bl, alpha)
                .setNormal(pose.copy(), nx, ny, nz);
    }
}
