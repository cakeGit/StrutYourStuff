package com.cake.struts.mixin.compat.create;

import com.cake.struts.content.block.StrutBlockEntity;
import com.simibubi.create.api.contraption.transformable.TransformableBlockEntity;
import com.simibubi.create.content.contraptions.StructureTransform;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = StrutBlockEntity.class, remap = false)
public abstract class StrutTransformCompat implements TransformableBlockEntity {
    @Override
    public void transform(final BlockEntity blockEntity, final StructureTransform transform) {
        if (blockEntity instanceof final StrutBlockEntity strutBlockEntity) {
            strutBlockEntity.transform(transform);
        }
    }
}
