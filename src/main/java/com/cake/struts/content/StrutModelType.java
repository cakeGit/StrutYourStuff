package com.cake.struts.content;

import net.minecraft.resources.ResourceLocation;

public record StrutModelType(ResourceLocation segmentModelLocation, ResourceLocation capTexture, int shapeSizeXPixels,
                             int shapeSizeYPixels,
                             int voxelShapeResolutionPixels) {

    public static final int DEFAULT_VOXEL_SHAPE_RESOLUTION = 4; // 2 is too choppy, 3 doesent round, 4 is highest good feeling resolution

    public StrutModelType {
        if (voxelShapeResolutionPixels < 1) {
            voxelShapeResolutionPixels = 1;
        }
    }

    public StrutModelType(final ResourceLocation segmentModelLocation, final ResourceLocation capTexture, final int shapeSizeXPixels, final int shapeSizeYPixels) {
        this(segmentModelLocation, capTexture, shapeSizeXPixels, shapeSizeYPixels, DEFAULT_VOXEL_SHAPE_RESOLUTION);
    }

    public StrutModelType(final ResourceLocation segmentModelLocation, final ResourceLocation capTexture) {
        this(segmentModelLocation, capTexture, 8, 12, DEFAULT_VOXEL_SHAPE_RESOLUTION);
    }
}

