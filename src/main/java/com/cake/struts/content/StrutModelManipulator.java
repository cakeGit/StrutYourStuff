package com.cake.struts.content;

import com.cake.struts.content.cap.CapAccumulator;
import com.cake.struts.content.geometry.StrutGeometry;
import com.cake.struts.content.mesh.StrutMeshQuad;
import com.cake.struts.content.mesh.StrutSegmentMesh;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class StrutModelManipulator {

    private static final Map<StrutModelType, StrutSegmentMesh> segmentMeshes = new HashMap<>();

    static List<BakedQuad> bakeConnection(final StrutModelBuilder.GirderConnection connection, final StrutModelType modelType) {
        if (connection.cableRenderInfo() != null) {
            return CableStrutModelManipulator.bake(connection, getSegmentMesh(modelType), modelType);
        }

        if (connection.renderLength() <= StrutGeometry.EPSILON) {
            return List.of();
        }

        final Vec3 span = connection.end().subtract(connection.start());
        final double spanLength = span.length();
        if (spanLength <= StrutGeometry.EPSILON) {
            return List.of();
        }
        final double renderLength = Math.min(connection.renderLength(), spanLength);
        if (renderLength <= StrutGeometry.EPSILON) {
            return List.of();
        }

        final StrutSegmentMesh mesh = getSegmentMesh(modelType);
        final List<StrutMeshQuad> quads = mesh.forLength((float) renderLength);

        final PoseStack poseStack = StrutGeometry.poseAlong(connection.start(), span.normalize());
        final PoseStack.Pose last = poseStack.last();
        final Matrix4f pose = new Matrix4f(last.pose());
        final Matrix3f normalMatrix = new Matrix3f(last.normal());

        final Vector3f planePoint = StrutGeometry.toVector3f(connection.surfacePlanePoint());
        final Vector3f planeNormal = StrutGeometry.toVector3f(connection.surfaceNormal());
        if (planeNormal.lengthSquared() > StrutGeometry.EPSILON) {
            planeNormal.normalize();
        }

        final List<BakedQuad> bakedQuads = new ArrayList<>();
        final CapAccumulator capAccumulator = new CapAccumulator(modelType.capTexture());
        for (final StrutMeshQuad quad : quads) {
            quad.transformAndEmit(pose, normalMatrix, planePoint, planeNormal, capAccumulator, bakedQuads);
        }
        capAccumulator.emitCaps(planePoint, planeNormal, bakedQuads);
        return bakedQuads;
    }

    static @NotNull StrutSegmentMesh getSegmentMesh(final StrutModelType modelType) {
        StrutSegmentMesh strutSegmentMesh = segmentMeshes.get(modelType);
        if (strutSegmentMesh == null) {
            final ModelManager modelManager = Minecraft.getInstance().getModelManager();
            final ModelResourceLocation modelLocation = ModelResourceLocation.standalone(modelType.segmentModelLocation());
            final BakedModel bakedModel = modelManager.getModel(modelLocation);
            final RandomSource random = RandomSource.create();
            final List<BakedQuad> bakedQuads = new ArrayList<>(bakedModel.getQuads(
                    null,
                    null,
                    random,
                    ModelData.EMPTY,
                    null
            ));
            segmentMeshes.put(modelType, strutSegmentMesh = new StrutSegmentMesh(bakedQuads));
        }
        return strutSegmentMesh;
    }

    public static void invalidateMeshes() {
        segmentMeshes.clear();
    }

}

