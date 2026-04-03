package com.cake.struts.content;

import com.cake.struts.content.block.StrutBlock;
import com.cake.struts.content.block.StrutBlockEntity;
import com.cake.struts.content.connection.GirderConnectionNode;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.common.util.TriState;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ApiStatus.ScheduledForRemoval
public class StrutModelBuilder extends BakedModelWrapper<BakedModel> {

    private static final ModelProperty<GirderStrutModelData> GIRDER_PROPERTY = new ModelProperty<>();
    private static final double SURFACE_OFFSET = (6 / 16f) + 1e-3;
    public static final double SURFACE_CLIPPING_OFFSET = 10 / 16f + 1e-3;

    public StrutModelBuilder(final BakedModel originalModel) {
        super(originalModel);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return false;
    }

    @Override
    public @NotNull TriState useAmbientOcclusion(final @NotNull BlockState state, final @NotNull ModelData data, final @NotNull RenderType renderType) {
        return TriState.FALSE;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(final BlockState state, final Direction side, final @NotNull RandomSource rand, final @NotNull ModelData data, final RenderType renderType) {
        final List<BakedQuad> base = new ArrayList<>(super.getQuads(state, side, rand, data, renderType));
//        if (renderType != null && renderType != RenderType.solid()) {
//            return base;
//        }
//        if (sideFactor != null) { //Fuck this shit took me way to long to figure out
//            return base;
//        }
//        if (!data.has(GIRDER_PROPERTY)) {
//            return base;
//        }
//        GirderStrutModelData girderData = data.get(GIRDER_PROPERTY);
//        if (girderData == null || girderData.connections().isEmpty()) {
//            return base;
//        }
//        for (GirderConnection connection : girderData.connections()) {
//            base.addAll(GirderStrutModelManipulator.bakeConnection(connection));
//        }
        return base;
    }

    @Override
    public @NotNull ModelData getModelData(final BlockAndTintGetter level, final @NotNull BlockPos pos, final @NotNull BlockState state, final @NotNull ModelData blockEntityData) {
        if (!(level.getBlockEntity(pos) instanceof final StrutBlockEntity blockEntity)) {
            return ModelData.EMPTY;
        }
        blockEntity.connectionQuadCache = null;
//        GirderStrutModelData data = GirderStrutModelData.collect(level, pos, state, blockEntity);
        return ModelData.builder()
//            .with(GIRDER_PROPERTY, data)
                .build();
    }

    public static @NotNull List<BakedQuad> buildConnectionQuads(final @NotNull BlockAndTintGetter level,
                                                                final @NotNull BlockPos pos,
                                                                final @NotNull BlockState state,
                                                                final @NotNull StrutBlockEntity blockEntity,
                                                                final @NotNull StrutModelType modelType) {
        final GirderStrutModelData connectionData = GirderStrutModelData.collect(level, pos, state, blockEntity);
        return connectionData.connections()
                .stream()
                .flatMap(connection -> StrutModelManipulator.bakeConnection(connection, modelType).stream())
                .toList();
    }

    static final class GirderStrutModelData {
        private final List<GirderConnection> connections;
        private final BlockPos pos;

        private GirderStrutModelData(final List<GirderConnection> connections, final BlockPos pos) {
            this.connections = connections;
            this.pos = pos;
        }

        static GirderStrutModelData collect(final BlockAndTintGetter level, final BlockPos pos, final BlockState state, final StrutBlockEntity blockEntity) {
            if (!(state.getBlock() instanceof final StrutBlock block)) {
                return new GirderStrutModelData(List.of(), pos);
            }
            final Direction facing = state.getValue(StrutBlock.FACING);
            final CableStrutInfo cableRenderInfo = block.getCableRenderInfo();
            final Vec3 blockOrigin = Vec3.atLowerCornerOf(pos);
            final Vec3 facePoint = Vec3.atCenterOf(pos).relative(facing, -SURFACE_CLIPPING_OFFSET);
            final Vec3 thisSurface = Vec3.atCenterOf(pos).relative(facing, -SURFACE_OFFSET);

            final List<GirderConnection> connections = new ArrayList<>();

            for (final GirderConnectionNode data : blockEntity.getConnectionsCopy()) {
                final BlockPos otherPos = data.absoluteFrom(pos);
                final Direction otherFacing = data.peerFacing();
                final Vec3 otherSurface = Vec3.atCenterOf(otherPos).relative(otherFacing, -SURFACE_OFFSET);
                final Vec3 span = otherSurface.subtract(thisSurface);
                if (span.lengthSqr() < 1.0e-4) {
                    continue;
                }
                final Vec3 halfVector = span.scale(0.5);
                final double renderLength = halfVector.length() + 0.5f;
                if (renderLength <= 1.0e-4) {
                    continue;
                }

                final Vec3 startLocal = thisSurface.subtract(blockOrigin);
                final Vec3 endLocal = otherSurface.subtract(blockOrigin);
                final Vec3 planePointLocal = facePoint.subtract(blockOrigin);

                final CableStrutInfo effectiveRenderInfo = cableRenderInfo != null && blockEntity.isTensioned(data.relativeOffset())
                        ? cableRenderInfo.withZeroSag()
                        : cableRenderInfo;
                connections.add(new GirderConnection(
                        startLocal,
                        endLocal,
                        renderLength,
                        planePointLocal,
                        Vec3.atLowerCornerOf(facing.getNormal()),
                        effectiveRenderInfo
                ));
            }

            return new GirderStrutModelData(Collections.unmodifiableList(connections), pos);
        }

        public BlockPos getPos() {
            return pos;
        }

        List<GirderConnection> connections() {
            return connections;
        }
    }

    public record GirderConnection(Vec3 start, Vec3 end, double renderLength, Vec3 surfacePlanePoint,
                                   Vec3 surfaceNormal,
                                   CableStrutInfo cableRenderInfo) {
    }
}
