package com.cake.struts.content.cap;

import com.cake.struts.content.geometry.StrutGeometry;
import com.cake.struts.content.geometry.StrutVertex;
import com.cake.struts.content.mesh.StrutMeshEdge;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class CapAccumulator {

    private final ResourceLocation capTexture;
    private final List<CapSegment> segments = new ArrayList<>();

    public CapAccumulator(final ResourceLocation capTexture) {
        this.capTexture = capTexture;
    }

    public void addEdges(final TextureAtlasSprite sourceSprite, final int tintIndex, final boolean shade, final List<StrutMeshEdge> newSegments) {
        for (final StrutMeshEdge segment : newSegments) {
            final CapVertex start = new CapVertex(segment.start(), sourceSprite);
            final CapVertex end = new CapVertex(segment.end(), sourceSprite);
            if (StrutGeometry.positionsEqual(start.position(), end.position())) {
                continue;
            }
            segments.add(new CapSegment(start, end, tintIndex, shade));
        }
    }

    public void emitCapsToConsumer(final Vector3f planeNormal, final List<Consumer<BufferBuilder>> bufferConsumer, final Function<Vector3f, Integer> lightFunction) {
        emitPreparedLoops(planeNormal, (loop, vertices, normal, sprite) ->
                emitLoopToConsumer(loop, vertices, normal, bufferConsumer, lightFunction));
    }

    public void emitCaps(final Vector3f planePoint, final Vector3f planeNormal, final List<BakedQuad> consumer) {
        emitPreparedLoops(planeNormal, (loop, vertices, normal, sprite) ->
                emitLoop(loop, vertices, normal, sprite, consumer));
    }

    private void emitPreparedLoops(final Vector3f planeNormal, final LoopEmitter emitter) {
        if (segments.isEmpty()) {
            return;
        }
        final Vector3f normal = new Vector3f(planeNormal);
        if (normal.lengthSquared() <= StrutGeometry.EPSILON) {
            return;
        }
        normal.normalize();

        final TextureAtlasSprite capSprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(capTexture);
        final List<CapVertex> uniqueVertices = new ArrayList<>();
        final List<LoopEdge> edges = buildEdges(uniqueVertices);

        for (final ClosedLoop loop : extractClosedLoops(uniqueVertices, edges)) {
            applyCapUVToVertices(loop.indices(), uniqueVertices, normal, capSprite);
            emitter.emit(loop, uniqueVertices, normal, capSprite);
        }

        segments.clear();
    }

    private List<LoopEdge> buildEdges(final List<CapVertex> uniqueVertices) {
        final List<LoopEdge> edges = new ArrayList<>();
        for (final CapSegment segment : segments) {
            final int startIndex = indexFor(uniqueVertices, segment.start());
            final int endIndex = indexFor(uniqueVertices, segment.end());
            if (startIndex != endIndex) {
                edges.add(new LoopEdge(startIndex, endIndex, segment.tintIndex(), segment.shade()));
            }
        }
        return edges;
    }

    private List<ClosedLoop> extractClosedLoops(final List<CapVertex> uniqueVertices, final List<LoopEdge> edges) {
        final List<ClosedLoop> loops = new ArrayList<>();
        while (true) {
            final LoopEdge startEdge = findUnusedEdge(edges);
            if (startEdge == null) {
                break;
            }

            final List<Integer> loop = new ArrayList<>();
            loop.add(startEdge.start());
            loop.add(startEdge.end());
            startEdge.markUsed();

            int current = startEdge.end();
            boolean closed = false;
            final int maxSteps = edges.size() + 1;
            int steps = 0;
            while (steps < maxSteps) {
                steps++;
                if (current == loop.get(0)) {
                    closed = true;
                    break;
                }

                final LoopEdge nextEdge = findAndUseEdge(edges, current);
                if (nextEdge == null) {
                    closed = tryCloseWithParallelEdge(loop, uniqueVertices, edges, current);
                    break;
                }

                final int nextVertex = nextEdge.other(current);
                loop.add(nextVertex);
                current = nextVertex;
            }

            if (closed && loop.size() > 2) {
                loop.remove(loop.size() - 1);
                loops.add(new ClosedLoop(loop, startEdge.tintIndex(), startEdge.shade()));
            }
        }
        return loops;
    }

    private boolean tryCloseWithParallelEdge(final List<Integer> loop,
                                             final List<CapVertex> uniqueVertices,
                                             final List<LoopEdge> edges,
                                             final int current) {
        boolean closed = false;
        final int loopPre = loop.get(loop.size() - 2);
        final int loopPost = loop.get(loop.size() - 1);
        final Vector3f dir = new Vector3f(uniqueVertices.get(loopPost).position()).sub(uniqueVertices.get(loopPre).position()).normalize();

        for (final LoopEdge edge : edges) {
            if (edge.used() || edge.start() == current || edge.end() == current) {
                continue;
            }
            final Vector3f edgeDir = new Vector3f(uniqueVertices.get(edge.end()).position()).sub(uniqueVertices.get(edge.start()).position()).normalize();
            if (Math.abs(dir.dot(edgeDir)) <= 0.999f) {
                continue;
            }

            final Vector3f toStart = new Vector3f(uniqueVertices.get(edge.start()).position()).sub(uniqueVertices.get(loopPost).position()).normalize();
            final Vector3f toEnd = new Vector3f(uniqueVertices.get(edge.end()).position()).sub(uniqueVertices.get(loopPre).position()).normalize();
            if (toStart.dot(toEnd) < 0.01f) {
                edge.markUsed();
                loop.add(edge.end());
                loop.add(edge.start());
                loop.add(edge.end());
                closed = true;
            }

            final Vector3f toStartR = new Vector3f(uniqueVertices.get(edge.start()).position()).sub(uniqueVertices.get(loop.get(0)).position()).normalize();
            final Vector3f toEndR = new Vector3f(uniqueVertices.get(edge.end()).position()).sub(uniqueVertices.get(loop.get(1)).position()).normalize();
            if (toStartR.dot(toEndR) < 0.01f) {
                edge.markUsed();
                loop.add(edge.end());
                loop.add(edge.start());
                loop.add(edge.end());
                closed = true;
            }
        }
        return closed;
    }

    private void emitLoopToConsumer(final ClosedLoop loop,
                                    final List<CapVertex> vertices,
                                    final Vector3f normal,
                                    final List<Consumer<BufferBuilder>> bufferConsumer,
                                    final Function<Vector3f, Integer> lightFunction) {
        final List<StrutVertex> cleaned = prepareLoopVertices(loop.indices(), vertices, normal);
        if (cleaned.size() < 3) {
            return;
        }
        StrutGeometry.emitPolygonToConsumer(cleaned, bufferConsumer, lightFunction);
    }

    private void emitLoop(
            final ClosedLoop loop,
            final List<CapVertex> vertices,
            final Vector3f normal,
            final TextureAtlasSprite stoneSprite,
            final List<BakedQuad> consumer
    ) {
        final List<StrutVertex> cleaned = prepareLoopVertices(loop.indices(), vertices, normal);
        if (cleaned.size() < 3) {
            return;
        }
        final Vector3f faceNormal = cleaned.get(0).normal();
        final Direction face = Direction.getNearest(faceNormal.x, faceNormal.y, faceNormal.z);
        StrutGeometry.emitPolygon(cleaned, stoneSprite, face, loop.tintIndex(), loop.shade(), consumer);
    }

    private List<StrutVertex> prepareLoopVertices(final List<Integer> loopIndices, final List<CapVertex> vertices, final Vector3f normal) {
        final Vector3f normalizedPlane = new Vector3f(normal);
        if (normalizedPlane.lengthSquared() > StrutGeometry.EPSILON) {
            normalizedPlane.normalize();
        }
        final Vector3f faceNormal = new Vector3f(normalizedPlane).negate();
        final List<StrutVertex> cleaned = StrutGeometry.dedupeLoopVertices(getStrutVertices(loopIndices, vertices, faceNormal));
        if (cleaned.size() < 3) {
            return cleaned;
        }
        final Vector3f polygonNormal = StrutGeometry.computePolygonNormal(cleaned);
        if (polygonNormal.lengthSquared() > StrutGeometry.EPSILON && polygonNormal.dot(faceNormal) < 0f) {
            Collections.reverse(cleaned);
        }
        return cleaned;
    }

    private void applyCapUVToVertices(final List<Integer> loop, final List<CapVertex> uniqueVertices, final Vector3f normal, final TextureAtlasSprite sprite) {
        final Vector3f uvUp = new Vector3f(0, 1, 0).cross(normal);
        if (uvUp.lengthSquared() <= StrutGeometry.EPSILON) {
            uvUp.set(1, 0, 0);
        }
        uvUp.normalize();

        final Vector3f uvRight = new Vector3f(normal).cross(uvUp).normalize();

        final Vector3f uvOrigin = new Vector3f();
        for (final int index : loop) {
            uvOrigin.add(uniqueVertices.get(index).position());
        }
        uvOrigin.mul(1f / loop.size());

        final float uScale = sprite.getU1() - sprite.getU0();
        final float vScale = sprite.getV1() - sprite.getV0();
        final float uOffset = sprite.getU0() + uScale / 2f;
        final float vOffset = sprite.getV0() + vScale / 2f;

        for (final int index : loop) {
            final CapVertex vertex = uniqueVertices.get(index);
            final Vector3f toVertex = new Vector3f(vertex.position()).sub(uvOrigin);
            final float u = Math.clamp(toVertex.dot(uvRight), -0.5f, 0.5f);
            final float v = Math.clamp(toVertex.dot(uvUp), -0.5f, 0.5f);
            uniqueVertices.set(index, new CapVertex(vertex.position(), uScale * u + uOffset, vScale * v + vOffset, vertex.color(), vertex.light(), vertex.sourceSprite()));
        }
    }

    private int indexFor(final List<CapVertex> vertices, final CapVertex vertex) {
        for (int i = 0; i < vertices.size(); i++) {
            if (positionsClose(vertices.get(i).position(), vertex.position())) {
                return i;
            }
        }
        vertices.add(vertex.copy());
        return vertices.size() - 1;
    }

    private static boolean positionsClose(final Vector3f a, final Vector3f b) {
        final float dx = a.x - b.x;
        final float dy = a.y - b.y;
        final float dz = a.z - b.z;
        final float tol = 0.01f;
        return dx * dx + dy * dy + dz * dz <= tol * tol;
    }

    private LoopEdge findUnusedEdge(final List<LoopEdge> edges) {
        for (final LoopEdge edge : edges) {
            if (!edge.used()) {
                return edge;
            }
        }
        return null;
    }

    private LoopEdge findAndUseEdge(final List<LoopEdge> edges, final int vertexIndex) {
        for (final LoopEdge edge : edges) {
            if (!edge.used() && (edge.start() == vertexIndex || edge.end() == vertexIndex)) {
                edge.markUsed();
                return edge;
            }
        }
        return null;
    }

    private static @NotNull List<StrutVertex> getStrutVertices(final List<Integer> loopIndices, final List<CapVertex> vertices, final Vector3f faceNormal) {
        final List<StrutVertex> loopVertices = new ArrayList<>(loopIndices.size());
        for (final int index : loopIndices) {
            final CapVertex data = vertices.get(index);
            loopVertices.add(new StrutVertex(
                    new Vector3f(data.position()),
                    new Vector3f(faceNormal),
                    data.u,
                    data.v,
                    data.color(),
                    data.light()
            ));
        }
        return loopVertices;
    }

    private record CapSegment(CapVertex start, CapVertex end, int tintIndex, boolean shade) {
    }

    private record ClosedLoop(List<Integer> indices, int tintIndex, boolean shade) {
    }

    private interface LoopEmitter {
        void emit(ClosedLoop loop, List<CapVertex> vertices, Vector3f normal, TextureAtlasSprite sprite);
    }

    private static class CapVertex {

        private final Vector3f position;
        private final float u;
        private final float v;
        private final int color;
        private final int light;
        private final TextureAtlasSprite sourceSprite;

        CapVertex(final StrutVertex vertex, final TextureAtlasSprite sprite) {
            this(new Vector3f(vertex.position()), vertex.u(), vertex.v(), vertex.color(), vertex.light(), sprite);
        }

        private CapVertex(final Vector3f position, final float u, final float v, final int color, final int light, final TextureAtlasSprite sourceSprite) {
            this.position = position;
            this.u = u;
            this.v = v;
            this.color = color;
            this.light = light;
            this.sourceSprite = sourceSprite;
        }

        Vector3f position() {
            return position;
        }

        float u() {
            return u;
        }

        float v() {
            return v;
        }

        int color() {
            return color;
        }

        int light() {
            return light;
        }

        TextureAtlasSprite sourceSprite() {
            return sourceSprite;
        }

        CapVertex copy() {
            return new CapVertex(new Vector3f(position), u, v, color, light, sourceSprite);
        }
    }

    private static class LoopEdge {

        private final int start;
        private final int end;
        private final int tintIndex;
        private final boolean shade;
        private boolean used;

        LoopEdge(final int start, final int end, final int tintIndex, final boolean shade) {
            this.start = start;
            this.end = end;
            this.tintIndex = tintIndex;
            this.shade = shade;
        }

        int start() {
            return start;
        }

        int end() {
            return end;
        }

        int tintIndex() {
            return tintIndex;
        }

        boolean shade() {
            return shade;
        }

        boolean used() {
            return used;
        }

        void markUsed() {
            used = true;
        }

        int other(final int vertex) {
            return vertex == start ? end : start;
        }
    }
}
