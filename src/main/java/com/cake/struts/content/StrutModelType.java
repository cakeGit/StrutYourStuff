package com.cake.struts.content;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.function.Supplier;

public record StrutModelType(ResourceLocation segmentModelLocation, ResourceLocation capTexture, int shapeSizeXPixels,
                             int shapeSizeYPixels,
                             int voxelShapeResolutionPixels,
                             Supplier<Supplier<RenderType>> renderType) {

    public static final int DEFAULT_VOXEL_SHAPE_RESOLUTION = 4; // 2 is too choppy, 3 doesent round, 4 is highest good feeling resolution

    public StrutModelType {
        if (voxelShapeResolutionPixels < 1) {
            voxelShapeResolutionPixels = 1;
        }
    }

    public StrutModelType(final ResourceLocation segmentModelLocation, final ResourceLocation capTexture, final int shapeSizeXPixels, final int shapeSizeYPixels) {
        this(segmentModelLocation, capTexture, shapeSizeXPixels, shapeSizeYPixels, DEFAULT_VOXEL_SHAPE_RESOLUTION, () -> RenderType::solid);
    }

    public StrutModelType(final ResourceLocation segmentModelLocation, final ResourceLocation capTexture) {
        this(segmentModelLocation, capTexture, 8, 12, DEFAULT_VOXEL_SHAPE_RESOLUTION, () -> RenderType::solid);
    }


    public StrutModelType(final ResourceLocation segmentModelLocation, final ResourceLocation capTexture,
                          Supplier<Supplier<RenderType>> renderType) {
        this(segmentModelLocation, capTexture, 8, 12, DEFAULT_VOXEL_SHAPE_RESOLUTION, renderType);
    }

    @OnlyIn(Dist.CLIENT)
    public RenderType getRenderType() {
        return renderType.get().get();
    }
}

