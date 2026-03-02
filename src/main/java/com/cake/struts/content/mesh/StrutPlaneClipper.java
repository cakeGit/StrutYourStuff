package com.cake.struts.content.mesh;

import com.cake.struts.content.geometry.StrutGeometry;
import com.cake.struts.content.geometry.StrutVertex;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Sutherland–Hodgman plane-clipping for strut mesh polygons.
 * Clips a list of vertices against a half-space defined by a plane point and normal,
 * collecting intersection edge segments for cap generation.
 */
public final class StrutPlaneClipper {

    /**
     * Result of clipping a polygon against a plane. Contains the surviving vertices
     * on the inside of the plane, and the intersection edge segments on the plane boundary.
     */
    public record ClipResult(List<StrutVertex> polygon, List<StrutMeshEdge> segments, boolean clipped) {
    }

    public static ClipResult clip(final List<StrutVertex> input,
                                  final Vector3f planePoint,
                                  final Vector3f planeNormal) {
        if (planeNormal.lengthSquared() <= StrutGeometry.EPSILON) {
            return new ClipResult(input, List.of(), false);
        }

        final List<StrutVertex> result = new ArrayList<>();
        final List<StrutMeshEdge> segments = new ArrayList<>();
        boolean clipped = false;

        final int size = input.size();
        StrutVertex previousVertex = input.get(size - 1);
        float previousDistance = StrutGeometry.signedDistance(previousVertex.position(), planeNormal, planePoint);
        boolean previousInside = previousDistance >= -StrutGeometry.EPSILON;

        StrutVertex pendingSegmentStart = null;

        for (final StrutVertex currentVertex : input) {
            final float currentDistance = StrutGeometry.signedDistance(currentVertex.position(), planeNormal, planePoint);
            final boolean currentInside = currentDistance >= -StrutGeometry.EPSILON;

            final List<StrutVertex> edgePoints = new ArrayList<>();
            if (Math.abs(previousDistance) <= StrutGeometry.EPSILON) {
                edgePoints.add(previousVertex);
            }

            if (currentInside != previousInside) {
                final float t = previousDistance / (previousDistance - currentDistance);
                final StrutVertex intersection = StrutGeometry.interpolate(previousVertex, currentVertex, t);
                result.add(intersection);
                edgePoints.add(intersection);
                clipped = true;
            }

            if (currentInside) {
                result.add(currentVertex);
            }

            if (Math.abs(currentDistance) <= StrutGeometry.EPSILON) {
                edgePoints.add(currentVertex);
            }

            if (!currentInside) {
                clipped = true;
            }

            for (final StrutVertex edgePoint : edgePoints) {
                if (pendingSegmentStart == null) {
                    pendingSegmentStart = edgePoint;
                } else if (!StrutGeometry.positionsEqual(pendingSegmentStart.position(), edgePoint.position())) {
                    segments.add(new StrutMeshEdge(pendingSegmentStart, edgePoint));
                    pendingSegmentStart = null;
                }
            }

            previousVertex = currentVertex;
            previousDistance = currentDistance;
            previousInside = currentInside;
        }

        return new ClipResult(result, segments, clipped);
    }
}
