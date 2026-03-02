package com.cake.struts.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderSystem.class)
public interface StrutRenderSystemAccessor {

    @Accessor("shaderLightDirections")
    static Vector3f[] struts$getShaderLightDirections() {
        throw new UnsupportedOperationException();
    }

}
