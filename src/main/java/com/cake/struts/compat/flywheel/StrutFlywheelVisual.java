package com.cake.struts.compat.flywheel;

import com.cake.struts.content.StrutModelBuilder;
import com.cake.struts.content.block.StrutBlock;
import com.cake.struts.content.block.StrutBlockEntity;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.baked.BakedModelBuilder;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class StrutFlywheelVisual extends AbstractBlockEntityVisual<StrutBlockEntity> {

    private @Nullable TransformedInstance instance;
    private @Nullable List<BakedQuad> cachedQuads;
    private @Nullable Model cachedModel;
    private int cachedConnectionHash = Integer.MIN_VALUE;

    public StrutFlywheelVisual(final @NotNull VisualizationContext ctx,
                               final @NotNull StrutBlockEntity blockEntity,
                               final float partialTick) {
        super(ctx, blockEntity, partialTick);
        refreshModel();
    }

    @Override
    public void update(final float partialTick) {
        refreshModel();
    }

    @Override
    public void updateLight(final float partialTick) {
        if (instance != null) {
            relight(instance);
            instance.handle().setChanged();
        }
    }

    @Override
    public void collectCrumblingInstances(final Consumer<@Nullable Instance> consumer) {
        consumer.accept(instance);
    }

    @Override
    protected void _delete() {
        clearInstance();
    }

    private void refreshModel() {
        if (!(blockState.getBlock() instanceof final StrutBlock strutBlock)) {
            clearInstance();
            return;
        }

        final int connectionHash = blockEntity.getConnectionHash();
        if (connectionHash == cachedConnectionHash && cachedQuads != null && cachedModel != null && instance != null) {
            return;
        }

        if (cachedQuads == null || connectionHash != cachedConnectionHash) {
            cachedQuads = resolveQuads(strutBlock);
            cachedConnectionHash = connectionHash;
            cachedModel = null;
        }

        if (cachedQuads.isEmpty()) {
            clearInstance();
            return;
        }

        if (cachedModel == null) {
            cachedModel = buildModel(cachedQuads);
        }

        if (instance != null) {
            instance.delete();
        }

        instance = instancerProvider().instancer(InstanceTypes.TRANSFORMED, cachedModel).createInstance();
        instance.setIdentityTransform().translate(getVisualPosition());
        relight(instance);
        instance.handle().setChanged();
    }

    private @NotNull List<BakedQuad> resolveQuads(final @NotNull StrutBlock strutBlock) {
        final List<BakedQuad> quadCache = blockEntity.connectionQuadCache;
        if (quadCache != null) {
            return quadCache;
        }
        return StrutModelBuilder.buildConnectionQuads(level, pos, blockState, blockEntity, strutBlock.getModelType());
    }

    private @NotNull Model buildModel(final @NotNull List<BakedQuad> quads) {
        final BakedModel baseModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(blockState);
        final BakedModel model = new StrutBakedModel(baseModel, quads);
        // StrutBakedModel returns shade=false quads to prevent vanilla BakedModelBufferer from baking
        // block-face directional shading into vertex colours. We keep the Flywheel builder minimal
        // to avoid version-specific material function classes during runtime.
        return new BakedModelBuilder(model)
                .level(level)
                .pos(pos)
                .build();
    }

    private void clearInstance() {
        if (instance != null) {
            instance.delete();
            instance = null;
        }
        cachedModel = null;
    }
}
