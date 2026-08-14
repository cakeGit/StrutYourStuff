package com.cake.struts.content.shape;

import com.cake.struts.content.CableStrutInfo;
import com.cake.struts.content.CableStrutModelManipulator;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ryanhcode.sable.companion.ClientSubLevelAccess;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import net.minecraft.core.BlockPos;
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
        this.buildFrames();
    }

    private static List<Vec3> samplePoints(final Vec3 from, final Vec3 to, final CableStrutInfo info) {
        final double spanLength = to.subtract(from).length();
        if (spanLength <= SurfaceClippingHelper.MIN_LENGTH_SQR) {
            return List.of(from, to);
        }
        return CableStrutModelManipulator.sampleCurvePoints(from, to, info, spanLength);
    }

    private static List<Vec3> sanitizePoints(final List<Vec3> rawPoints) {
        final List<Vec3> cleaned = new ArrayList<>(rawPoints.size());
        for (final Vec3 point : rawPoints) {
            if (cleaned.isEmpty() || cleaned.getLast().distanceToSqr(point) > SurfaceClippingHelper.MIN_LENGTH_SQR) {
                cleaned.add(point);
            }
        }

        if (cleaned.isEmpty()) {
            cleaned.add(new Vec3(0.0, 0.0, 0.0));
        }
        if (cleaned.size() == 1) {
            cleaned.add(cleaned.getFirst());
        }
        return List.copyOf(cleaned);
    }

    protected double getTangentExtension() {
        return TANGENT_EXTENSION;
    }

    @Nullable
    @Override
    public Vec3 intersect(final Vec3 rayFrom, final Vec3 rayTo) {
        if (this.segmentCount <= 0) {
            return null;
        }

        if (!this.bounds.contains(rayFrom) && this.bounds.clip(rayFrom, rayTo).isEmpty()) {
            return null;
        }

        final Vec3 rayDir = rayTo.subtract(rayFrom);
        if (rayDir.lengthSqr() < SurfaceClippingHelper.MIN_LENGTH_SQR) {
            return null;
        }

        double bestDistanceSq = Double.POSITIVE_INFINITY;
        Vec3 bestHit = null;

        for (int segmentIndex = 0; segmentIndex < this.segmentCount; segmentIndex++) {
            final Vec3 start = this.points.get(segmentIndex);
            final Vec3 end = this.points.get(segmentIndex + 1);
            final Vec3 segment = end.subtract(start);
            final double segmentLength = segment.length();
            if (segmentLength * segmentLength < SurfaceClippingHelper.MIN_LENGTH_SQR) {
                continue;
            }

            final Vec3 segmentTangent = segment.scale(1.0 / segmentLength);
            final Frame frame = this.frameForSegment(segmentIndex, segmentIndex + 1, 0.5, segmentTangent);
            final Vec3 segmentCenter = start.add(segment.scale(0.5));
            final double halfLength = segmentLength * 0.5 + this.getTangentExtension();

            final double t = SurfaceClippingHelper.intersectRayWithObb(
                    rayFrom, rayDir, segmentCenter, segmentTangent, frame.u, frame.v,
                    halfLength, this.halfWidth, this.halfHeight
            );
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
    public void drawOutline(final PoseStack ms, final VertexConsumer vb, final Vec3 camera, final int color,
                            final ClientSubLevelAccess containingSubLevel) {
        if (this.segmentCount <= 0) {
            return;
        }

        final float hw = (float) this.halfWidth;
        final float hh = (float) this.halfHeight;

        final Pose3dc renderPose = containingSubLevel != null ? containingSubLevel.renderPose() : null;

        for (int segmentIndex = 0; segmentIndex < this.segmentCount; segmentIndex++) {
            final int endIndex = segmentIndex + 1;

            final Vec3 start = this.points.get(segmentIndex);
            final Vec3 end = this.points.get(endIndex);

            final List<Vec3> startCorners = SurfaceClippingHelper.getCorners(
                    start,
                    this.uAtVertex[segmentIndex],
                    this.vAtVertex[segmentIndex],
                    hw,
                    hh
            );
            final List<Vec3> endCorners = getCornersInClosestOrder(
                    SurfaceClippingHelper.getCorners(end, this.uAtVertex[endIndex], this.vAtVertex[endIndex], hw, hh),
                    startCorners
            );

            for (int cornerIndex = 0; cornerIndex < 4; cornerIndex++) {
                final Vec3 from = renderPose != null ? renderPose.transformPosition(startCorners.get(cornerIndex)) : startCorners.get(
                        cornerIndex);
                final Vec3 to = renderPose != null ? renderPose.transformPosition(endCorners.get(cornerIndex)) : endCorners.get(
                        cornerIndex);
                line(
                        vb,
                        ms,
                        from.subtract(camera),
                        to.subtract(camera),
                        color
                );
            }
        }
    }

    @Override
    public BlockPos getSubLevelReferencePosition() {
        return BlockPos.containing(this.points.getFirst());
    }

    private static AABB computeBounds(final List<Vec3> path, final double inflate) {
        AABB box = new AABB(path.getFirst(), path.getFirst()).inflate(inflate);
        for (int i = 1; i < path.size(); i++) {
            box = box.minmax(new AABB(path.get(i), path.get(i)).inflate(inflate));
        }
        return box;
    }

    private static List<Vec3> getCornersInClosestOrder(final List<Vec3> destinationPoints,
                                                       final List<Vec3> sourcePoints) {
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

    private void buildFrames() {
        if (this.points.size() < 2) {
            return;
        }

        final int count = this.points.size();
        for (int i = 0; i < count; i++) {
            final Vec3 tangent;
            if (i == 0) {
                tangent = SurfaceClippingHelper.safeDirection(this.points.get(1).subtract(this.points.get(0)));
            } else if (i == count - 1) {
                tangent = SurfaceClippingHelper.safeDirection(this.points.get(count - 1).subtract(this.points.get(count - 2)));
            } else {
                final Vec3 in = SurfaceClippingHelper.safeDirection(this.points.get(i).subtract(this.points.get(i - 1)));
                final Vec3 out = SurfaceClippingHelper.safeDirection(this.points.get(i + 1).subtract(this.points.get(i)));
                tangent = SurfaceClippingHelper.safeDirection(in.add(out));
            }
            this.tangentAtVertex[i] = tangent;
        }

        for (int i = 0; i < count; i++) {
            final Vec3 tangent = this.tangentAtVertex[i];
            final Vec3 u;
            if (i == 0) {
                u = SurfaceClippingHelper.perpendicularUnit(tangent);
            } else {
                Vec3 projected = this.uAtVertex[i - 1].subtract(tangent.scale(this.uAtVertex[i - 1].dot(tangent)));
                if (projected.lengthSqr() < SurfaceClippingHelper.MIN_LENGTH_SQR) {
                    projected = SurfaceClippingHelper.perpendicularUnit(tangent);
                } else {
                    projected = projected.normalize();
                }
                u = projected;
            }
            final Vec3 v = SurfaceClippingHelper.safeDirection(tangent.cross(u));
            this.uAtVertex[i] = u;
            this.vAtVertex[i] = v;
        }
    }

    private Frame frameForSegment(final int startIndex,
                                  final int endIndex,
                                  final double segT,
                                  final Vec3 segmentTangent) {
        Vec3 u = this.uAtVertex[startIndex].lerp(this.uAtVertex[endIndex], segT);
        u = u.subtract(segmentTangent.scale(u.dot(segmentTangent)));
        if (u.lengthSqr() < SurfaceClippingHelper.MIN_LENGTH_SQR) {
            u = SurfaceClippingHelper.perpendicularUnit(segmentTangent);
        } else {
            u = u.normalize();
        }
        final Vec3 v = SurfaceClippingHelper.safeDirection(segmentTangent.cross(u));
        return new Frame(u, v);
    }

    private record Frame(Vec3 u, Vec3 v) {
    }
}
