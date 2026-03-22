package com.cake.struts.content;

import com.cake.struts.content.cap.CapAccumulator;
import com.cake.struts.content.geometry.StrutGeometry;
import com.cake.struts.content.mesh.StrutMeshQuad;
import com.cake.struts.content.mesh.StrutSegmentMesh;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles baking of cable strut connections into {@link BakedQuad} lists.
 * Stems from {@link StrutModelManipulator} to separate cable-specific logic.
 */
@OnlyIn(Dist.CLIENT)
public final class CableStrutModelManipulator {

    private static final double MIN_CABLE_STEP = 0.1;
    private static final double RENDER_START_EXTENSION = 0.0;

    static @NotNull List<BakedQuad> bake(final StrutModelBuilder.GirderConnection connection,
                                         final StrutSegmentMesh mesh,
                                         final StrutModelType modelType) {
        final CableStrutInfo renderInfo = connection.cableRenderInfo();
        if (renderInfo == null) {
            return List.of();
        }

        final Vec3 start = connection.start();
        final Vec3 end = connection.end();
        final double spanLength = end.subtract(start).length();
        if (spanLength <= StrutGeometry.EPSILON) {
            return List.of();
        }

        final double renderLength = Math.min(Math.max(connection.renderLength() - 0.5f, 0.0), spanLength);
        if (renderLength <= StrutGeometry.EPSILON) {
            return List.of();
        }

        final List<Vec3> sampledPoints = sampleCurvePoints(start, end, renderInfo, renderLength);
        if (sampledPoints.size() < 2) {
            return List.of();
        }

        final List<Vec3> renderPoints = new ArrayList<>(sampledPoints);
        extendStart(renderPoints);

        final List<CableSegment> segments = buildSegments(renderPoints);
        if (segments.isEmpty()) {
            return List.of();
        }

        final Vector3f planePoint = toVector3f(connection.surfacePlanePoint());
        final Vector3f planeNormal = toVector3f(connection.surfaceNormal());
        if (planeNormal.lengthSquared() > StrutGeometry.EPSILON) {
            planeNormal.normalize();
        }

        final CapAccumulator capAccumulator = new CapAccumulator(modelType.capTexture());
        final List<BakedQuad> bakedQuads = new ArrayList<>();
        double textureDistance = 0.0;

        for (final CableSegment segment : segments) {
            final Vec3 segmentDelta = segment.end().subtract(segment.start());
            final float length = (float) segmentDelta.length();
            if (length <= StrutGeometry.EPSILON) {
                continue;
            }

            final Vec3 dir = segmentDelta.normalize();
            final double distHorizontal = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
            final float yRot = distHorizontal == 0 ? 0f : (float) Math.atan2(dir.x, dir.z);
            final float xRot = (float) Math.atan2(dir.y, distHorizontal);

            final Vec3 segmentOrigin = segment.start().add(dir.scale(0.5));
            final PoseStack poseStack = new PoseStack();
            poseStack.translate(segmentOrigin.x, segmentOrigin.y, segmentOrigin.z);
            poseStack.mulPose(new Quaternionf().rotationY(yRot));
            poseStack.mulPose(new Quaternionf().rotationX(-xRot));
            poseStack.translate(-0.5f, -0.5f, -0.5f);

            final PoseStack.Pose last = poseStack.last();
            final Matrix4f pose = new Matrix4f(last.pose());
            final Matrix3f normalMatrix = new Matrix3f(last.normal());

            final float textureOffset = (float) (textureDistance - Math.floor(textureDistance));
            final List<StrutMeshQuad> quads = mesh.forLength(length, textureOffset);
            for (final StrutMeshQuad quad : quads) {
                quad.transformAndEmit(pose, normalMatrix, planePoint, planeNormal, capAccumulator, bakedQuads);
            }
            textureDistance += length;
        }

        capAccumulator.emitCaps(planePoint, planeNormal, bakedQuads);
        return bakedQuads;
    }

    public static @NotNull List<Vec3> sampleCurvePoints(final Vec3 start,
                                                        final Vec3 end,
                                                        final CableStrutInfo renderInfo,
                                                        final double renderLength) {
        final Vec3 delta = end.subtract(start);
        final double spanLength = delta.length();
        if (spanLength <= StrutGeometry.EPSILON) {
            return List.of(start, end);
        }

        final double clampedLength = Math.min(Math.max(renderLength, 0.0), spanLength);
        if (clampedLength <= StrutGeometry.EPSILON) {
            return List.of(start, start);
        }

        final double tMax = Math.min(1.0, clampedLength / spanLength);
        final double sagDistance = renderInfo.sag() * spanLength;
        final double minStep = Math.max(renderInfo.minStep(), MIN_CABLE_STEP);
        final int maxSegments = Math.max(renderInfo.maxSegments(), 1);
        final double step = Math.max(minStep, spanLength / maxSegments);
        final double tStep = Math.min(tMax, step / spanLength);

        final List<Vec3> points = new ArrayList<>();
        double t = 0.0;
        while (t < tMax) {
            points.add(sagPoint(start, delta, t, sagDistance));
            t += tStep;
        }
        points.add(sagPoint(start, delta, tMax, sagDistance));
        return points;
    }

    private static @NotNull List<CableSegment> buildSegments(final List<Vec3> points) {
        final List<CableSegment> segments = new ArrayList<>();
        for (int i = 1; i < points.size(); i++) {
            final Vec3 start = points.get(i - 1);
            final Vec3 end = points.get(i);
            if (start.distanceToSqr(end) > StrutGeometry.EPSILON * StrutGeometry.EPSILON) {
                segments.add(new CableSegment(start, end));
            }
        }
        return segments;
    }

    private static void extendStart(final List<Vec3> points) {
        if (points.size() < 2) {
            return;
        }
        final Vec3 start = points.get(0);
        final Vec3 next = points.get(1);
        final Vec3 direction = next.subtract(start);
        if (direction.lengthSqr() <= StrutGeometry.EPSILON) {
            return;
        }
        points.set(0, start.subtract(direction.normalize().scale(RENDER_START_EXTENSION)));
    }

    static Vec3 sagPoint(final Vec3 start, final Vec3 delta, final double t, final double sagDistance) {
        final Vec3 base = start.add(delta.scale(t));
        final double sagFactor = 4.0 * t * (1.0 - t);
        return base.add(0.0, -sagDistance * sagFactor, 0.0);
    }

    private static Vector3f toVector3f(final Vec3 vec) {
        return new Vector3f((float) vec.x, (float) vec.y, (float) vec.z);
    }

    private record CableSegment(Vec3 start, Vec3 end) {
    }
}
