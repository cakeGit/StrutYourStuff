package com.cake.struts.compat.flywheel;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class StrutBakedModel extends BakedModelWrapper<BakedModel> {

    private static final List<BakedQuad> EMPTY = List.of();
    private static final ChunkRenderTypeSet RENDER_TYPES = ChunkRenderTypeSet.of(RenderType.solid());

    private final List<BakedQuad> shadedQuads;

    public StrutBakedModel(final BakedModel originalModel, final List<BakedQuad> quads) {
        super(originalModel);
        this.shadedQuads = quads.stream()
                .map(q -> q.isShade() ? q : new BakedQuad(q.getVertices(), q.getTintIndex(), q.getDirection(), q.getSprite(), true))
                .toList();
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(final @Nullable BlockState state, final @Nullable Direction side, final @NotNull RandomSource rand) {
        return side == null ? this.shadedQuads : EMPTY;
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(final @Nullable BlockState state,
                                             final @Nullable Direction side,
                                             final @NotNull RandomSource rand,
                                             final @NotNull ModelData extraData,
                                             final @Nullable RenderType renderType) {
        if (side != null) {
            return EMPTY;
        }
        if (renderType != null && !RENDER_TYPES.contains(renderType)) {
            return EMPTY;
        }
        return this.shadedQuads;
    }

    @Override
    public @NotNull ChunkRenderTypeSet getRenderTypes(final @NotNull BlockState state, final @NotNull RandomSource rand, final @NotNull ModelData data) {
        return RENDER_TYPES;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return false;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }
}
