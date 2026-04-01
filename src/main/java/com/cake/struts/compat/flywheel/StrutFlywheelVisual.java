package com.cake.struts.compat.flywheel;

import com.cake.struts.content.StrutModelBuilder;
import com.cake.struts.content.block.StrutBlock;
import com.cake.struts.content.block.StrutBlockEntity;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
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
        this.refreshModel();
    }

    @Override
    public void update(final float partialTick) {
        this.refreshModel();
    }

    @Override
    public void updateLight(final float partialTick) {
        this.cachedModel = null;
        this.refreshModel();
    }

    @Override
    public void collectCrumblingInstances(final Consumer<@Nullable Instance> consumer) {
        consumer.accept(this.instance);
    }

    @Override
    protected void _delete() {
        this.clearInstance();
    }

    private void refreshModel() {
        if (!(this.blockState.getBlock() instanceof final StrutBlock strutBlock)) {
            this.clearInstance();
            return;
        }

        final int connectionHash = this.blockEntity.getConnectionHash();
        if (connectionHash == this.cachedConnectionHash && this.cachedQuads != null && this.cachedModel != null && this.instance != null) {
            return;
        }

        if (this.cachedQuads == null || connectionHash != this.cachedConnectionHash) {
            this.cachedQuads = this.resolveQuads(strutBlock);
            this.cachedConnectionHash = connectionHash;
            this.cachedModel = null;
        }

        if (this.cachedQuads.isEmpty()) {
            this.clearInstance();
            return;
        }

        if (this.cachedModel == null) {
            final boolean constantAmbientLight = this.level instanceof final ClientLevel cl
                    && cl.effects().constantAmbientLight();
            this.cachedModel = FlywheelMeshBuilder.buildLitModel(this.cachedQuads, this.blockEntity.createLighter(), constantAmbientLight);
        }

        if (this.instance != null) {
            this.instance.delete();
        }

        this.instance = this.instancerProvider().instancer(
                InstanceTypes.TRANSFORMED,
                this.cachedModel
        ).createInstance();

        this.instance.setIdentityTransform().translate(this.getVisualPosition());
        this.instance.light(0);
        this.instance.handle().setChanged();
    }

    private @NotNull List<BakedQuad> resolveQuads(final @NotNull StrutBlock strutBlock) {
        final List<BakedQuad> quadCache = this.blockEntity.connectionQuadCache;
        if (quadCache != null) {
            return quadCache;
        }
        return StrutModelBuilder.buildConnectionQuads(
                this.level, this.pos, this.blockState,
                this.blockEntity, strutBlock.getModelType()
        );
    }

    private void clearInstance() {
        if (this.instance != null) {
            this.instance.delete();
            this.instance = null;
        }
        this.cachedModel = null;
    }
}
