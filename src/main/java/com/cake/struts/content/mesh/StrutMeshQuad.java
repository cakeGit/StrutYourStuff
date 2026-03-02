package com.cake.struts.content.mesh;

import com.cake.struts.content.cap.CapAccumulator;
import com.cake.struts.content.geometry.StrutGeometry;
import com.cake.struts.content.geometry.StrutVertex;
import com.cake.struts.internal.util.BakedQuadHelper;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public final class StrutMeshQuad {

    private final StrutVertex[] vertices;
    private final TextureAtlasSprite sprite;
    private final Direction nominalFace;
    private final int tintIndex;
    private final boolean shade;

    private StrutMeshQuad(final StrutVertex[] vertices, final TextureAtlasSprite sprite, final Direction nominalFace, final int tintIndex, final boolean shade) {
        this.vertices = vertices;
        this.sprite = sprite;
        this.nominalFace = nominalFace;
        this.tintIndex = tintIndex;
        this.shade = shade;
    }

    public static StrutMeshQuad from(final BakedQuad quad) {
        final int[] data = quad.getVertices();
        final int stride = BakedQuadHelper.VERTEX_STRIDE;
        final StrutVertex[] vertices = new StrutVertex[4];
        for (int i = 0; i < 4; i++) {
            final Vector3f pos = toVector3f(BakedQuadHelper.getXYZ(data, i));
            final Vector3f normal = toVector3f(BakedQuadHelper.getNormalXYZ(data, i));
            final float u = BakedQuadHelper.getU(data, i);
            final float v = BakedQuadHelper.getV(data, i);
            final int baseIndex = stride * i;
            final int color = data.length > baseIndex + BakedQuadHelper.COLOR_OFFSET ? data[baseIndex + BakedQuadHelper.COLOR_OFFSET] : StrutGeometry.DEFAULT_COLOR;
            final int light = data.length > baseIndex + BakedQuadHelper.LIGHT_OFFSET ? data[baseIndex + BakedQuadHelper.LIGHT_OFFSET] : StrutGeometry.DEFAULT_COLOR;
            vertices[i] = new StrutVertex(pos, normal, u, v, color, light);
        }
        return new StrutMeshQuad(vertices, quad.getSprite(), quad.getDirection(), quad.getTintIndex(), quad.isShade());
    }

    public StrutMeshQuad translate(final float dx, final float dy, final float dz) {
        final StrutVertex[] translated = new StrutVertex[vertices.length];
        for (int i = 0; i < vertices.length; i++) {
            final StrutVertex vertex = vertices[i];
            final Vector3f pos = new Vector3f(vertex.position()).add(dx, dy, dz);
            translated[i] = new StrutVertex(pos, new Vector3f(vertex.normal()), vertex.u(), vertex.v(), vertex.color(), vertex.light());
        }
        return new StrutMeshQuad(translated, sprite, nominalFace, tintIndex, shade);
    }

    public StrutMeshQuad scaleZWithUv(final float scale) {
        float minZ = Float.POSITIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;
        float minU = Float.POSITIVE_INFINITY;
        float minV = Float.POSITIVE_INFINITY;

        for (final StrutVertex vertex : vertices) {
            final float z = vertex.position().z;
            minZ = Math.min(minZ, z);
            maxZ = Math.max(maxZ, z);
            minU = Math.min(minU, vertex.u());
            minV = Math.min(minV, vertex.v());
        }

        final float zRange = maxZ - minZ;
        if (Math.abs(zRange) <= StrutGeometry.EPSILON) {
            return this;
        }

        float slopeU = 0f;
        float slopeV = 0f;
        int slopeSamples = 0;
        for (int i = 0; i < vertices.length; i++) {
            final StrutVertex current = vertices[i];
            final StrutVertex next = vertices[(i + 1) % vertices.length];
            final float dz = next.position().z - current.position().z;
            if (Math.abs(dz) > StrutGeometry.EPSILON) {
                slopeU += Math.abs((next.u() - current.u()) / dz);
                slopeV += Math.abs((next.v() - current.v()) / dz);
                slopeSamples++;
            }
        }

        final boolean scaleU = slopeSamples > 0 && slopeU >= slopeV;
        final boolean scaleV = slopeSamples > 0 && slopeV > slopeU;

        final StrutVertex[] scaled = new StrutVertex[vertices.length];
        for (int i = 0; i < vertices.length; i++) {
            final StrutVertex vertex = vertices[i];
            final Vector3f pos = new Vector3f(vertex.position().x, vertex.position().y, minZ + (vertex.position().z - minZ) * scale);
            float u = vertex.u();
            float v = vertex.v();
            if (scaleU) {
                u = minU + (u - minU) * scale;
            } else if (scaleV) {
                v = minV + (v - minV) * scale;
            }
            scaled[i] = new StrutVertex(pos, new Vector3f(vertex.normal()), u, v, vertex.color(), vertex.light());
        }
        return new StrutMeshQuad(scaled, sprite, nominalFace, tintIndex, shade);
    }

    public StrutMeshQuad clipZ(final float maxZ) {
        float minZ = Float.POSITIVE_INFINITY;
        float maxOriginalZ = Float.NEGATIVE_INFINITY;
        for (final StrutVertex vertex : vertices) {
            final float z = vertex.position().z;
            minZ = Math.min(minZ, z);
            maxOriginalZ = Math.max(maxOriginalZ, z);
        }
        if (maxZ >= maxOriginalZ - StrutGeometry.EPSILON) {
            return this;
        }
        if (maxZ <= minZ + StrutGeometry.EPSILON) {
            final float translation = maxZ - maxOriginalZ;
            final StrutVertex[] shifted = new StrutVertex[vertices.length];
            for (int i = 0; i < vertices.length; i++) {
                final StrutVertex vertex = vertices[i];
                final Vector3f pos = new Vector3f(vertex.position()).add(0f, 0f, translation);
                shifted[i] = new StrutVertex(pos, new Vector3f(vertex.normal()), vertex.u(), vertex.v(), vertex.color(), vertex.light());
            }
            return new StrutMeshQuad(shifted, sprite, nominalFace, tintIndex, shade);
        }
        final List<StrutVertex> clipped = new ArrayList<>();

        for (int i = 0; i < vertices.length; i++) {
            final StrutVertex current = vertices[i];
            final StrutVertex next = vertices[(i + 1) % vertices.length];

            final boolean currentInside = current.position().z <= maxZ + StrutGeometry.EPSILON;
            final boolean nextInside = next.position().z <= maxZ + StrutGeometry.EPSILON;

            if (currentInside && nextInside) {
                clipped.add(next);
            } else if (currentInside && !nextInside) {
                clipped.add(StrutGeometry.interpolate(current, next, clampT(current, next, maxZ)));
            } else if (!currentInside && nextInside) {
                clipped.add(StrutGeometry.interpolate(current, next, clampT(current, next, maxZ)));
                clipped.add(next);
            }
        }

        if (clipped.size() < 3) {
            return null;
        }

        return new StrutMeshQuad(clipped.toArray(new StrutVertex[0]), sprite, nominalFace, tintIndex, shade);
    }

    public StrutMeshQuad clipMinZ(final float minZ) {
        float minOriginalZ = Float.POSITIVE_INFINITY;
        float maxOriginalZ = Float.NEGATIVE_INFINITY;
        for (final StrutVertex vertex : vertices) {
            final float z = vertex.position().z;
            minOriginalZ = Math.min(minOriginalZ, z);
            maxOriginalZ = Math.max(maxOriginalZ, z);
        }
        if (minZ <= minOriginalZ + StrutGeometry.EPSILON) {
            return this;
        }
        if (minZ >= maxOriginalZ - StrutGeometry.EPSILON) {
            return null;
        }

        final List<StrutVertex> clipped = new ArrayList<>();
        for (int i = 0; i < vertices.length; i++) {
            final StrutVertex current = vertices[i];
            final StrutVertex next = vertices[(i + 1) % vertices.length];

            final boolean currentInside = current.position().z >= minZ - StrutGeometry.EPSILON;
            final boolean nextInside = next.position().z >= minZ - StrutGeometry.EPSILON;

            if (currentInside && nextInside) {
                clipped.add(next);
            } else if (currentInside) {
                clipped.add(StrutGeometry.interpolate(current, next, clampT(current, next, minZ)));
            } else if (nextInside) {
                clipped.add(StrutGeometry.interpolate(current, next, clampT(current, next, minZ)));
                clipped.add(next);
            }
        }

        if (clipped.size() < 3) {
            return null;
        }

        return new StrutMeshQuad(clipped.toArray(new StrutVertex[0]), sprite, nominalFace, tintIndex, shade);
    }

    private float clampT(final StrutVertex current, final StrutVertex next, final float maxZ) {
        final float delta = next.position().z - current.position().z;
        if (Math.abs(delta) < StrutGeometry.EPSILON) {
            return 0f;
        }
        return (maxZ - current.position().z) / delta;
    }

    public void transformAndEmit(
            final Matrix4f pose,
            final Matrix3f normalMatrix,
            final Vector3f planePoint,
            final Vector3f planeNormal,
            final CapAccumulator capAccumulator,
            final List<BakedQuad> consumer
    ) {
        final List<StrutVertex> transformed = transform(pose, normalMatrix);
        final StrutPlaneClipper.ClipResult clipResult = StrutPlaneClipper.clip(transformed, planePoint, planeNormal);
        if (clipResult.polygon().size() >= 3) {
            StrutGeometry.emitPolygon(clipResult.polygon(), sprite, nominalFace, tintIndex, shade, consumer);
        }
        if (clipResult.clipped() && planeNormal.lengthSquared() > StrutGeometry.EPSILON) {
            capAccumulator.addEdges(sprite, tintIndex, shade, clipResult.segments());
        }
    }

    public void transformAndEmitToConsumer(
            final Matrix4f pose,
            final Matrix3f normalMatrix,
            final Vector3f planePoint,
            final Vector3f planeNormal,
            final CapAccumulator capAccumulator,
            final List<Consumer<BufferBuilder>> bufferConsumer,
            final Function<Vector3f, Integer> lightFunction
    ) {
        final List<StrutVertex> transformed = transform(pose, normalMatrix);
        final StrutPlaneClipper.ClipResult clipResult = StrutPlaneClipper.clip(transformed, planePoint, planeNormal);
        if (clipResult.polygon().size() >= 3) {
            StrutGeometry.emitPolygonToConsumer(clipResult.polygon(), bufferConsumer, lightFunction);
        }
        if (clipResult.clipped() && planeNormal.lengthSquared() > StrutGeometry.EPSILON) {
            capAccumulator.addEdges(sprite, tintIndex, shade, clipResult.segments());
        }
    }

    private List<StrutVertex> transform(final Matrix4f pose, final Matrix3f normalMatrix) {
        final List<StrutVertex> transformed = new ArrayList<>(vertices.length);
        for (final StrutVertex vertex : vertices) {
            final Vector3f position = new Vector3f(vertex.position());
            pose.transformPosition(position);
            final Vector3f normal = new Vector3f(vertex.normal());
            normalMatrix.transform(normal);
            if (normal.lengthSquared() > StrutGeometry.EPSILON) {
                normal.normalize();
            }
            transformed.add(new StrutVertex(position, normal, vertex.u(), vertex.v(), vertex.color(), vertex.light()));
        }
        return transformed;
    }

    private static Vector3f toVector3f(final net.minecraft.world.phys.Vec3 vec) {
        return new Vector3f((float) vec.x, (float) vec.y, (float) vec.z);
    }
}
