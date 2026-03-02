package com.cake.struts.mixin.compat.create;

import com.cake.struts.content.block.StrutBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(value = StrutBlockEntity.class, remap = false)
public abstract class StrutTransformCompat {
}
