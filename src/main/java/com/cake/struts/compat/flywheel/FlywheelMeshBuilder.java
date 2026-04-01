package com.cake.struts.compat.flywheel;

import com.cake.struts.content.StrutDiffuseHelper;
import com.cake.struts.internal.util.BakedQuadHelper;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.lib.memory.MemoryBlock;
import dev.engine_room.flywheel.lib.model.ModelUtil;
import dev.engine_room.flywheel.lib.model.SimpleModel;
import dev.engine_room.flywheel.lib.model.SimpleQuadMesh;
import dev.engine_room.flywheel.lib.vertex.NoOverlayVertexView;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Vec3i;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Function;

public class FlywheelMeshBuilder {

    public static Model buildLitModel(final List<BakedQuad> quads,
                                      final Function<Vector3f, Integer> lighter,
                                      final boolean constantAmbientLight) {
        final int vertexCount = quads.size() * 4;
        final MemoryBlock memory = MemoryBlock.mallocTracked(vertexCount * NoOverlayVertexView.STRIDE);
        final NoOverlayVertexView view = new NoOverlayVertexView();
        view.ptr(memory.ptr());
        view.vertexCount(vertexCount);
        view.nativeMemoryOwner(memory);

        int vertexIndex = 0;
        for (final BakedQuad quad : quads) {
            final int[] data = quad.getVertices();
            final Vec3i faceNormal = quad.getDirection().getNormal();
            final float diffuse = StrutDiffuseHelper.calculateWorldSpaceDiffuse(
                    new Vector3f(faceNormal.getX(), faceNormal.getY(), faceNormal.getZ()), constantAmbientLight);

            for (int v = 0; v < 4; v++) {
                final int base = v * BakedQuadHelper.VERTEX_STRIDE;
                final float x = Float.intBitsToFloat(data[base]);
                final float y = Float.intBitsToFloat(data[base + 1]);
                final float z = Float.intBitsToFloat(data[base + 2]);

                final int color = data[base + BakedQuadHelper.COLOR_OFFSET];
                final float r = ((color >> 16) & 0xFF) / 255.0f * diffuse;
                final float g = ((color >> 8) & 0xFF) / 255.0f * diffuse;
                final float b = (color & 0xFF) / 255.0f * diffuse;
                final float a = ((color >> 24) & 0xFF) / 255.0f;

                final float u = Float.intBitsToFloat(data[base + BakedQuadHelper.UV_OFFSET]);
                final float vt = Float.intBitsToFloat(data[base + BakedQuadHelper.UV_OFFSET + 1]);

                final int packedNormal = data[base + BakedQuadHelper.NORMAL_OFFSET];
                final float nx = (byte) (packedNormal & 0xFF) / 127.0f;
                final float ny = (byte) ((packedNormal >> 8) & 0xFF) / 127.0f;
                final float nz = (byte) ((packedNormal >> 16) & 0xFF) / 127.0f;

                final int light = lighter.apply(new Vector3f(x, y, z));

                view.x(vertexIndex, x);
                view.y(vertexIndex, y);
                view.z(vertexIndex, z);
                view.r(vertexIndex, r);
                view.g(vertexIndex, g);
                view.b(vertexIndex, b);
                view.a(vertexIndex, a);
                view.u(vertexIndex, u);
                view.v(vertexIndex, vt);
                view.light(vertexIndex, light);
                view.normalX(vertexIndex, nx);
                view.normalY(vertexIndex, ny);
                view.normalZ(vertexIndex, nz);

                vertexIndex++;
            }
        }

        final Material material = ModelUtil.getMaterial(RenderType.solid(), false);
        final SimpleQuadMesh mesh = new SimpleQuadMesh(view, "strut_lit");
        return new SimpleModel(List.of(new Model.ConfiguredMesh(material, mesh)));
    }
}
