package com.cake.struts.content.shape;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class DefaultStrutConnectionShape implements StrutConnectionShape {

    private static final double INFLATE_PIXELS = 0.0;
    private static final double INTERSECTION_EPSILON = 1e-9;
    private static final double MIN_LENGTH_SQR = 1e-12;
    private static final double SURFACE_CLIP_EPSILON = 1e-4;

    private final Vec3 start, end;
    private final double halfWidth, halfHeight;
    private final AABB bounds;

    private final SurfaceClippingHelper.SurfacePlane fromSurfacePlane;
    private final SurfaceClippingHelper.SurfacePlane toSurfacePlane;

    private final Vec3 u, v, tangent;
    private final List<OutlineEdge> outlineEdges;
    private final boolean hasGeometry;

    public DefaultStrutConnectionShape(final Vec3 fromAttachment, final Vec3 toAttachment,
                                       final double halfWidth, final double halfHeight,
                                       final BlockPos fromPos, final Direction fromFacing,
                                       final BlockPos toPos, final Direction toFacing) {
        this.halfWidth = halfWidth + INFLATE_PIXELS;
        this.halfHeight = halfHeight + INFLATE_PIXELS;

        this.fromSurfacePlane = SurfaceClippingHelper.surfacePlane(fromPos, fromFacing);
        this.toSurfacePlane = SurfaceClippingHelper.surfacePlane(toPos, toFacing);

        final Vec3 dir = toAttachment.subtract(fromAttachment);
        if (dir.lengthSqr() >= MIN_LENGTH_SQR) {
            this.tangent = dir.normalize();
        } else {
            this.tangent = new Vec3(1.0, 0.0, 0.0);
        }

        // Extend endpoints by 0.5 mathematically and we clip after using planes
        this.start = fromAttachment.subtract(tangent.scale(0.5));
        this.end = toAttachment.add(tangent.scale(0.5));

        this.u = perpendicularUnit(this.tangent);
        this.v = safeDirection(this.tangent.cross(this.u));

        final List<Vec3> rawStart = getCorners(this.start, this.u, this.v, (float) this.halfWidth, (float) this.halfHeight);
        final List<Vec3> rawEnd = getCorners(this.end, this.u, this.v, (float) this.halfWidth, (float) this.halfHeight);

        final List<List<Vec3>> clippedFaces = clipFaces(rawStart, rawEnd);
        this.outlineEdges = buildOutlineEdges(clippedFaces);
        this.hasGeometry = !outlineEdges.isEmpty();

        this.bounds = computeBounds(clippedFaces);
    }

    @Nullable
    @Override
    public Vec3 intersect(final Vec3 rayFrom, final Vec3 rayTo) {
        if (!hasGeometry) {
            return null;
        }

        if (!bounds.contains(rayFrom) && bounds.clip(rayFrom, rayTo).isEmpty()) {
            return null;
        }

        final Vec3 rayDir = rayTo.subtract(rayFrom);
        if (rayDir.lengthSqr() < MIN_LENGTH_SQR) {
            return null;
        }

        final Vec3 segment = end.subtract(start);
        final double segmentLength = segment.length();
        final Vec3 segmentCenter = start.add(segment.scale(0.5));
        final double halfLength = segmentLength * 0.5;

        final double t = intersectRayWithObb(rayFrom, rayDir, segmentCenter, tangent, u, v, halfLength, halfWidth, halfHeight);
        if (Double.isNaN(t)) {
            return null;
        }

        final Vec3 hit = rayFrom.add(rayDir.scale(t));
        if (!SurfaceClippingHelper.isInside(hit, fromSurfacePlane, SURFACE_CLIP_EPSILON)) return null;
        if (!SurfaceClippingHelper.isInside(hit, toSurfacePlane, SURFACE_CLIP_EPSILON)) return null;

        return hit;
    }

    @Override
    public void drawOutline(final PoseStack ms, final VertexConsumer vb, final Vec3 camera) {
        drawOutline(ms, vb, camera, 0x66000000);
    }

    @Override
    public void drawOutline(final PoseStack ms, final VertexConsumer vb, final Vec3 camera, final int color) {
        if (!hasGeometry) {
            return;
        }

        for (final OutlineEdge edge : outlineEdges) {
            line(vb, ms, edge.from().subtract(camera), edge.to().subtract(camera), color);
        }
    }

    private List<List<Vec3>> clipFaces(final List<Vec3> rawStart, final List<Vec3> rawEnd) {
        final List<List<Vec3>> faces = new ArrayList<>();

        faces.add(List.of(rawStart.get(0), rawStart.get(1), rawStart.get(2), rawStart.get(3)));
        faces.add(List.of(rawEnd.get(0), rawEnd.get(1), rawEnd.get(2), rawEnd.get(3)));
        faces.add(List.of(rawStart.get(0), rawStart.get(1), rawEnd.get(1), rawEnd.get(0)));
        faces.add(List.of(rawStart.get(1), rawStart.get(2), rawEnd.get(2), rawEnd.get(1)));
        faces.add(List.of(rawStart.get(2), rawStart.get(3), rawEnd.get(3), rawEnd.get(2)));
        faces.add(List.of(rawStart.get(3), rawStart.get(0), rawEnd.get(0), rawEnd.get(3)));

        final List<List<Vec3>> clippedToFrom = clipFaceListToPlane(faces, fromSurfacePlane);
        return clipFaceListToPlane(clippedToFrom, toSurfacePlane);
    }

    private List<List<Vec3>> clipFaceListToPlane(final List<List<Vec3>> faces,
                                                 final SurfaceClippingHelper.SurfacePlane plane) {
        final List<List<Vec3>> result = new ArrayList<>(faces.size());
        for (final List<Vec3> face : faces) {
            final List<Vec3> clipped = SurfaceClippingHelper.clipPolygonToPlane(face, plane, SURFACE_CLIP_EPSILON);
            if (clipped.size() >= 3) {
                result.add(clipped);
            }
        }
        return result;
    }

    private List<OutlineEdge> buildOutlineEdges(final List<List<Vec3>> faces) {
        final List<OutlineEdge> edges = new ArrayList<>();
        for (final List<Vec3> face : faces) {
            for (int i = 0; i < face.size(); i++) {
                final Vec3 from = face.get(i);
                final Vec3 to = face.get((i + 1) % face.size());
                addUniqueEdge(edges, from, to);
            }
        }
        return edges;
    }

    private void addUniqueEdge(final List<OutlineEdge> edges, final Vec3 from, final Vec3 to) {
        if (from.distanceToSqr(to) < MIN_LENGTH_SQR) {
            return;
        }

        for (final OutlineEdge edge : edges) {
            final boolean sameDirection = pointsEqual(edge.from(), from) && pointsEqual(edge.to(), to);
            final boolean oppositeDirection = pointsEqual(edge.from(), to) && pointsEqual(edge.to(), from);
            if (sameDirection || oppositeDirection) {
                return;
            }
        }

        edges.add(new OutlineEdge(from, to));
    }

    private boolean pointsEqual(final Vec3 a, final Vec3 b) {
        return a.distanceToSqr(b) < MIN_LENGTH_SQR;
    }

    private AABB computeBounds(final List<List<Vec3>> faces) {
        if (faces.isEmpty()) {
            AABB box = new AABB(start, start).inflate(Math.max(this.halfWidth, this.halfHeight));
            box = box.minmax(new AABB(end, end).inflate(Math.max(this.halfWidth, this.halfHeight)));
            return box;
        }

        Vec3 first = null;
        for (final List<Vec3> face : faces) {
            if (!face.isEmpty()) {
                first = face.get(0);
                break;
            }
        }

        if (first == null) {
            AABB box = new AABB(start, start).inflate(Math.max(this.halfWidth, this.halfHeight));
            box = box.minmax(new AABB(end, end).inflate(Math.max(this.halfWidth, this.halfHeight)));
            return box;
        }

        AABB box = new AABB(first, first);
        for (final List<Vec3> face : faces) {
            for (final Vec3 point : face) {
                box = box.minmax(new AABB(point, point));
            }
        }
        return box;
    }

    private record OutlineEdge(Vec3 from, Vec3 to) {
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

    private static List<Vec3> getCorners(final Vec3 center, final Vec3 u, final Vec3 v,
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
}
