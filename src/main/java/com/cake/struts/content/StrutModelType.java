package com.cake.struts.content;

import net.minecraft.resources.ResourceLocation;

public record StrutModelType(ResourceLocation segmentModelLocation, ResourceLocation capTexture, int shapeSizeXPixels,
                             int shapeSizeYPixels,
                             int voxelShapeResolutionPixels) {

    public StrutModelType {
        if (voxelShapeResolutionPixels < 1) {
            voxelShapeResolutionPixels = 1;
        }
    }

    public StrutModelType(final ResourceLocation segmentModelLocation, final ResourceLocation capTexture, final int shapeSizeXPixels, final int shapeSizeYPixels) {
        this(segmentModelLocation, capTexture, shapeSizeXPixels, shapeSizeYPixels, 2);
    }

    public StrutModelType(final ResourceLocation segmentModelLocation, final ResourceLocation capTexture) {
        this(segmentModelLocation, capTexture, 8, 12, 2);
    }
}

