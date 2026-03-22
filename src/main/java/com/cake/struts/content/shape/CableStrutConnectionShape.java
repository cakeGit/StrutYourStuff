package com.cake.struts.content.shape;

import com.cake.struts.content.CableStrutInfo;
import com.cake.struts.content.CableStrutModelManipulator;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class CableStrutConnectionShape implements StrutConnectionShape {

    private static final double INFLATE_PIXELS = 0.0;
    private static final double TANGENT_EXTENSION = 8.0;
    private static final double INTERSECTION_EPSILON = 1e-9;
    private static final double MIN_LENGTH_SQR = 1e-12;

    private final List<Vec3> points;
    private final double halfWidth;
    private final double halfHeight;
    private final AABB bounds;

    private final Vec3[] tangentAtVertex;
    private final Vec3[] uAtVertex;
    private final Vec3[] vAtVertex;
    private final int segmentCount;

    public CableStrutConnectionShape(final Vec3 fromAttachment, final Vec3 toAttachment,
                                     final double halfWidth, final double halfHeight,
                                     final CableStrutInfo renderInfo) {
        this(samplePoints(fromAttachment, toAttachment, renderInfo), halfWidth, halfHeight);
    }

    private CableStrutConnectionShape(final List<Vec3> points,
                                      final double halfWidth, final double halfHeight) {
        this.points = sanitizePoints(points);
        this.halfWidth = halfWidth + INFLATE_PIXELS;
        this.halfHeight = halfHeight + INFLATE_PIXELS;
        this.segmentCount = Math.max(0, this.points.size() - 1);
        this.tangentAtVertex = new Vec3[this.points.size()];
        this.uAtVertex = new Vec3[this.points.size()];
        this.vAtVertex = new Vec3[this.points.size()];
        this.bounds = computeBounds(this.points, Math.max(this.halfWidth, this.halfHeight));
        buildFrames();
    }

    private static List<Vec3> samplePoints(final Vec3 from, final Vec3 to, final CableStrutInfo info) {
        final double spanLength = to.subtract(from).length();
        if (spanLength <= MIN_LENGTH_SQR) {
            return List.of(from, to);
        }
        return CableStrutModelManipulator.sampleCurvePoints(from, to, info, spanLength);
    }

    private static List<Vec3> sanitizePoints(final List<Vec3> rawPoints) {
        final List<Vec3> cleaned = new ArrayList<>(rawPoints.size());
        for (final Vec3 point : rawPoints) {
            if (cleaned.isEmpty() || cleaned.get(cleaned.size() - 1).distanceToSqr(point) > MIN_LENGTH_SQR) {
                cleaned.add(point);
            }
        }

        if (cleaned.isEmpty()) {
            cleaned.add(new Vec3(0.0, 0.0, 0.0));
        }
        if (cleaned.size() == 1) {
            cleaned.add(cleaned.get(0));
        }
        return List.copyOf(cleaned);
    }

    protected double getTangentExtension() {
        return TANGENT_EXTENSION;
    }

    @Nullable
    @Override
    public Vec3 intersect(final Vec3 rayFrom, final Vec3 rayTo) {
        if (segmentCount <= 0) {
            return null;
        }

        if (!bounds.contains(rayFrom) && bounds.clip(rayFrom, rayTo).isEmpty()) {
            return null;
        }

        final Vec3 rayDir = rayTo.subtract(rayFrom);
        if (rayDir.lengthSqr() < MIN_LENGTH_SQR) {
            return null;
        }

        double bestDistanceSq = Double.POSITIVE_INFINITY;
        Vec3 bestHit = null;

        for (int segmentIndex = 0; segmentIndex < segmentCount; segmentIndex++) {
            final Vec3 start = points.get(segmentIndex);
            final Vec3 end = points.get(segmentIndex + 1);
            final Vec3 segment = end.subtract(start);
            final double segmentLength = segment.length();
            if (segmentLength * segmentLength < MIN_LENGTH_SQR) {
                continue;
            }

            final Vec3 segmentTangent = segment.scale(1.0 / segmentLength);
            final Frame frame = frameForSegment(segmentIndex, segmentIndex + 1, 0.5, segmentTangent);
            final Vec3 segmentCenter = start.add(segment.scale(0.5));
            final double halfLength = segmentLength * 0.5 + getTangentExtension();

            final double t = intersectRayWithObb(rayFrom, rayDir, segmentCenter, segmentTangent, frame.u, frame.v,
                    halfLength, halfWidth, halfHeight);
            if (Double.isNaN(t)) {
                continue;
            }

            final Vec3 pointOnRay = rayFrom.add(rayDir.scale(t));
            final double distanceSq = rayFrom.distanceToSqr(pointOnRay);
            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq;
                bestHit = pointOnRay;
            }
        }

        return bestHit;
    }

    @Override
    public void drawOutline(final PoseStack ms, final VertexConsumer vb, final Vec3 camera) {
        drawOutline(ms, vb, camera, 0x66000000);
    }

    public void drawOutline(final PoseStack ms, final VertexConsumer vb, final Vec3 camera, final int color) {
        if (segmentCount <= 0) {
            return;
        }

        final float hw = (float) halfWidth;
        final float hh = (float) halfHeight;

        for (int segmentIndex = 0; segmentIndex < segmentCount; segmentIndex++) {
            final int endIndex = segmentIndex + 1;

            final Vec3 start = points.get(segmentIndex);
            final Vec3 end = points.get(endIndex);

            final List<Vec3> startCorners = getCorners(start, uAtVertex[segmentIndex], vAtVertex[segmentIndex], hw, hh);
            final List<Vec3> endCorners = getCornersInClosestOrder(
                    getCorners(end, uAtVertex[endIndex], vAtVertex[endIndex], hw, hh),
                    startCorners
            );

            for (int cornerIndex = 0; cornerIndex < 4; cornerIndex++) {
                line(vb, ms, startCorners.get(cornerIndex).subtract(camera), endCorners.get(cornerIndex).subtract(camera), color);
            }

//            drawRing(vb, ms,
//                    startCorners.get(0).subtract(camera),
//                    startCorners.get(1).subtract(camera),
//                    startCorners.get(2).subtract(camera),
//                    startCorners.get(3).subtract(camera),
//                    color);
        }

//        final int lastIndex = points.size() - 1;
//        final Vec3 end = points.get(lastIndex);
//        final List<Vec3> endCorners = getCorners(end, uAtVertex[lastIndex], vAtVertex[lastIndex], hw, hh);
//        drawRing(vb, ms,
//                endCorners.get(0).subtract(camera),
//                endCorners.get(1).subtract(camera),
//                endCorners.get(2).subtract(camera),
//                endCorners.get(3).subtract(camera),
//                color);
    }

    private static AABB computeBounds(final List<Vec3> path, final double inflate) {
        AABB box = new AABB(path.get(0), path.get(0)).inflate(inflate);
        for (int i = 1; i < path.size(); i++) {
            box = box.minmax(new AABB(path.get(i), path.get(i)).inflate(inflate));
        }
        return box;
    }

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

    private static List<Vec3> getCornersInClosestOrder(final List<Vec3> destinationPoints, final List<Vec3> sourcePoints) {
        List<Vec3> best = destinationPoints;
        double bestScore = Double.POSITIVE_INFINITY;

        for (final boolean reverse : new boolean[]{false, true}) {
            for (int offset = 0; offset < 4; offset++) {
                final List<Vec3> candidate = new ArrayList<>(4);
                double score = 0.0;
                for (int i = 0; i < 4; i++) {
                    final int index = reverse ? (offset - i + 4) % 4 : (offset + i) % 4;
                    final Vec3 point = destinationPoints.get(index);
                    candidate.add(point);
                    score += point.distanceToSqr(sourcePoints.get(i));
                }
                if (score < bestScore) {
                    bestScore = score;
                    best = candidate;
                }
            }
        }

        return best;
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

        vb.vertex(poseMatrix, (float) a.x, (float) a.y, (float) a.z)
                .color(r, g, bl, alpha)
                .normal(pose.normal(), nx, ny, nz)
                .endVertex();
        vb.vertex(poseMatrix, (float) b.x, (float) b.y, (float) b.z)
                .color(r, g, bl, alpha)
                .normal(pose.normal(), nx, ny, nz)
                .endVertex();
    }

    private void buildFrames() {
        if (points.size() < 2) {
            return;
        }

        final int count = points.size();
        for (int i = 0; i < count; i++) {
            final Vec3 tangent;
            if (i == 0) {
                tangent = safeDirection(points.get(1).subtract(points.get(0)));
            } else if (i == count - 1) {
                tangent = safeDirection(points.get(count - 1).subtract(points.get(count - 2)));
            } else {
                final Vec3 in = safeDirection(points.get(i).subtract(points.get(i - 1)));
                final Vec3 out = safeDirection(points.get(i + 1).subtract(points.get(i)));
                tangent = safeDirection(in.add(out));
            }
            tangentAtVertex[i] = tangent;
        }

        for (int i = 0; i < count; i++) {
            final Vec3 tangent = tangentAtVertex[i];
            final Vec3 u;
            if (i == 0) {
                u = perpendicularUnit(tangent);
            } else {
                Vec3 projected = uAtVertex[i - 1].subtract(tangent.scale(uAtVertex[i - 1].dot(tangent)));
                if (projected.lengthSqr() < MIN_LENGTH_SQR) {
                    projected = perpendicularUnit(tangent);
                } else {
                    projected = projected.normalize();
                }
                u = projected;
            }
            final Vec3 v = safeDirection(tangent.cross(u));
            uAtVertex[i] = u;
            vAtVertex[i] = v;
        }
    }

    private static Vec3 safeDirection(final Vec3 vec) {
        if (vec.lengthSqr() < MIN_LENGTH_SQR) {
            return new Vec3(1.0, 0.0, 0.0);
        }
        return vec.normalize();
    }

    private static Vec3 perpendicularUnit(final Vec3 tangent) {
        Vec3 candidate = new Vec3(0.0, 1.0, 0.0).cross(tangent);
        if (candidate.lengthSqr() < MIN_LENGTH_SQR) {
            candidate = new Vec3(1.0, 0.0, 0.0).cross(tangent);
        }
        return safeDirection(candidate);
    }

    private Frame frameForSegment(final int startIndex, final int endIndex, final double segT, final Vec3 segmentTangent) {
        Vec3 u = uAtVertex[startIndex].lerp(uAtVertex[endIndex], segT);
        u = u.subtract(segmentTangent.scale(u.dot(segmentTangent)));
        if (u.lengthSqr() < MIN_LENGTH_SQR) {
            u = perpendicularUnit(segmentTangent);
        } else {
            u = u.normalize();
        }
        final Vec3 v = safeDirection(segmentTangent.cross(u));
        return new Frame(u, v);
    }

    private record Frame(Vec3 u, Vec3 v) {
    }
}
