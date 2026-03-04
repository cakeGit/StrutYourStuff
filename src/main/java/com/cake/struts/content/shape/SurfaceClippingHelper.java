package com.cake.struts.content.shape;

import com.cake.struts.content.StrutModelBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SurfaceClippingHelper {

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
}