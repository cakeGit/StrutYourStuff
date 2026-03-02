package com.cake.struts.compat.flywheel;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.util.TriState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class StrutBakedModel extends BakedModelWrapper<BakedModel> {

    private static final List<BakedQuad> EMPTY = List.of();
    private static final ChunkRenderTypeSet RENDER_TYPES = ChunkRenderTypeSet.of(RenderType.solid());

    // Force shade=true so Flywheel's baked-model path keeps directional diffuse consistent with the
    // non-Flywheel BER path, which always applies diffuse shading for strut quads.
    private final List<BakedQuad> shadedQuads;

    public StrutBakedModel(final BakedModel originalModel, final List<BakedQuad> quads) {
        super(originalModel);
        this.shadedQuads = quads.stream()
                .map(q -> q.isShade() ? q : new BakedQuad(q.getVertices(), q.getTintIndex(), q.getDirection(), q.getSprite(), true, q.hasAmbientOcclusion()))
                .toList();
    }

    @Override
    public List<BakedQuad> getQuads(final @Nullable BlockState state, final @Nullable Direction side, final RandomSource rand) {
        return side == null ? shadedQuads : EMPTY;
    }

    @Override
    public List<BakedQuad> getQuads(final @Nullable BlockState state,
                                    final @Nullable Direction side,
                                    final RandomSource rand,
                                    final ModelData extraData,
                                    final @Nullable RenderType renderType) {
        if (side != null) {
            return EMPTY;
        }
        if (renderType != null && !RENDER_TYPES.contains(renderType)) {
            return EMPTY;
        }
        return shadedQuads;
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(final BlockState state, final RandomSource rand, final ModelData data) {
        return RENDER_TYPES;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return false;
    }

    @Override
    public TriState useAmbientOcclusion(final BlockState state, final ModelData data, final RenderType renderType) {
        return TriState.FALSE;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }
}
