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
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
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

        final Vec3 dir = span.normalize();
        final double distHorizontal = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        final float yRot = distHorizontal == 0 ? 0f : (float) Math.atan2(dir.x, dir.z);
        final float xRot = (float) Math.atan2(dir.y, distHorizontal);

        final PoseStack poseStack = new PoseStack();
        poseStack.translate(connection.start().x, connection.start().y, connection.start().z);
        poseStack.mulPose(new Quaternionf().rotationY(yRot));
        poseStack.mulPose(new Quaternionf().rotationX(-xRot));
        poseStack.translate(-0.5f, -0.5f, -0.5f);

        final PoseStack.Pose last = poseStack.last();
        final Matrix4f pose = new Matrix4f(last.pose());
        final Matrix3f normalMatrix = new Matrix3f(last.normal());

        final Vector3f planePoint = toVector3f(connection.surfacePlanePoint());
        final Vector3f planeNormal = toVector3f(connection.surfaceNormal());
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
            final BakedModel bakedModel = modelManager.getModel(modelType.segmentModelLocation());
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

    private static Vector3f toVector3f(final Vec3 vec) {
        return new Vector3f((float) vec.x, (float) vec.y, (float) vec.z);
    }

    public static void invalidateMeshes() {
        segmentMeshes.clear();
    }

}

