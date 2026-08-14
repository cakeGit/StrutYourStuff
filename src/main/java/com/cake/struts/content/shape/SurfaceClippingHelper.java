package com.cake.struts.content.shape;

import com.cake.struts.content.StrutModelBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SurfaceClippingHelper {

    public static final double INTERSECTION_EPSILON = 1e-9;
    public static final double MIN_LENGTH_SQR = 1e-12;

    public record SurfacePlane(Vec3 point, Vec3 normal) {
    }

    public record ClippedSegment(Vec3 start, Vec3 end, boolean clipped) {
    }

    public static SurfacePlane surfacePlane(final BlockPos pos, final Direction facing) {
        final Vec3 normal = new Vec3(facing.getStepX(), facing.getStepY(), facing.getStepZ());
        final Vec3 point = Vec3.atCenterOf(pos).add(normal.scale(-StrutModelBuilder.SURFACE_CLIPPING_OFFSET));
        return new SurfacePlane(point, normal);
    }

    public static boolean isInside(final Vec3 point, final SurfacePlane plane, final double epsilon) {
        return signedDistance(point, plane) >= -epsilon;
    }

    public static double signedDistance(final Vec3 point, final SurfacePlane plane) {
        return point.subtract(plane.point()).dot(plane.normal());
    }

    @Nullable
    public static ClippedSegment clipSegmentToPlanes(final Vec3 start,
                                                     final Vec3 end,
                                                     final SurfacePlane firstPlane,
                                                     final SurfacePlane secondPlane,
                                                     final double epsilon) {
        final ClippedSegment firstClip = clipSegmentToPlane(start, end, firstPlane, epsilon);
        if (firstClip == null) {
            return null;
        }

        final ClippedSegment secondClip = clipSegmentToPlane(firstClip.start(), firstClip.end(), secondPlane, epsilon);
        if (secondClip == null) {
            return null;
        }

        return new ClippedSegment(secondClip.start(), secondClip.end(), firstClip.clipped() || secondClip.clipped());
    }

    @Nullable
    public static ClippedSegment clipSegmentToPlane(final Vec3 start,
                                                    final Vec3 end,
                                                    final SurfacePlane plane,
                                                    final double epsilon) {
        final double startDistance = signedDistance(start, plane);
        final double endDistance = signedDistance(end, plane);

        final boolean startInside = startDistance >= -epsilon;
        final boolean endInside = endDistance >= -epsilon;

        if (startInside && endInside) {
            return new ClippedSegment(start, end, false);
        }

        if (!startInside && !endInside) {
            return null;
        }

        final double denominator = startDistance - endDistance;
        if (Math.abs(denominator) <= 1e-12) {
            return null;
        }

        final double t = startDistance / denominator;
        final Vec3 intersection = start.add(end.subtract(start).scale(t));

        if (startInside) {
            return new ClippedSegment(start, intersection, true);
        }

        return new ClippedSegment(intersection, end, true);
    }

    public static List<Vec3> clipPolygonToPlane(final List<Vec3> polygon,
                                                final SurfacePlane plane,
                                                final double epsilon) {
        if (polygon.isEmpty()) {
            return List.of();
        }

        final List<Vec3> result = new ArrayList<>();
        Vec3 previous = polygon.get(polygon.size() - 1);
        double previousDistance = signedDistance(previous, plane);
        boolean previousInside = previousDistance >= -epsilon;

        for (final Vec3 current : polygon) {
            final double currentDistance = signedDistance(current, plane);
            final boolean currentInside = currentDistance >= -epsilon;

            if (currentInside != previousInside) {
                final double denominator = previousDistance - currentDistance;
                if (Math.abs(denominator) > 1e-12) {
                    final double t = previousDistance / denominator;
                    final Vec3 intersection = previous.add(current.subtract(previous).scale(t));
                    result.add(intersection);
                }
            }

            if (currentInside) {
                result.add(current);
            }

            previous = current;
            previousDistance = currentDistance;
            previousInside = currentInside;
        }

        return result;
    }

    public static double intersectRayWithObb(final Vec3 rayOrigin, final Vec3 rayDir,
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

    public static List<Vec3> getCorners(final Vec3 center, final Vec3 u, final Vec3 v,
                                        final float hw, final float hh) {
        final Vec3 us = u.scale(hw);
        final Vec3 vs = v.scale(hh);
        return List.of(
                center.add(us).add(vs),
                center.add(us).subtract(vs),
                center.subtract(us).subtract(vs),
                center.subtract(us).add(vs)
        );
    }

    public static Vec3 safeDirection(final Vec3 vec) {
        if (vec.lengthSqr() < MIN_LENGTH_SQR) {
            return new Vec3(1.0, 0.0, 0.0);
        }
        return vec.normalize();
    }

    public static Vec3 perpendicularUnit(final Vec3 tangent) {
        Vec3 candidate = new Vec3(0.0, 1.0, 0.0).cross(tangent);
        if (candidate.lengthSqr() < MIN_LENGTH_SQR) {
            candidate = new Vec3(1.0, 0.0, 0.0).cross(tangent);
        }
        return safeDirection(candidate);
    }
}