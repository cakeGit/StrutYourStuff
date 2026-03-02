package com.cake.struts.mixin.compat.create;

import com.cake.struts.content.block.StrutBlock;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

@Pseudo
@Mixin(value = StrutBlock.class, remap = false)
public abstract class StrutWrenchCompat {

    @Unique
    public InteractionResult onWrenched(final BlockState state, final UseOnContext context) {
        return InteractionResult.PASS;
    }

    @Unique
    public InteractionResult onSneakWrenched(final BlockState state, final UseOnContext context) {
        return InteractionResult.PASS;
    }
}
