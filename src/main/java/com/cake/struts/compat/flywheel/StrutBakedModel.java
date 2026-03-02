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

    // Quads with shade=false so that vanilla BakedModelBufferer does not bake block-face directional
    // shading into vertex colours. Flywheel's fragment shader applies diffuseFromLightDirections
    // (CardinalLightingMode.ENTITY) in a single pass instead, matching the non-Flywheel BER path.
    private final List<BakedQuad> unshadedQuads;

    public StrutBakedModel(final BakedModel originalModel, final List<BakedQuad> quads) {
        super(originalModel);
        this.unshadedQuads = quads.stream()
                .map(q -> q.isShade() ? new BakedQuad(q.getVertices(), q.getTintIndex(), q.getDirection(), q.getSprite(), false) : q)
                .toList();
    }

    @Override
    public List<BakedQuad> getQuads(final @Nullable BlockState state, final @Nullable Direction side, final RandomSource rand) {
        return side == null ? unshadedQuads : EMPTY;
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
        return unshadedQuads;
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
