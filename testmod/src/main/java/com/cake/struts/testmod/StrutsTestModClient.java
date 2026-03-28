package com.cake.struts.testmod;

import com.cake.struts.compat.flywheel.StrutsFlywheelCompatLoader;
import com.cake.struts.content.block.StrutBlockEntityRenderer;
import com.cake.struts.testmod.registry.TestBlockEntities;
import com.cake.struts.testmod.registry.TestStrutDefinitions;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = StrutsTestMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class StrutsTestModClient {

    @SubscribeEvent
    public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(TestBlockEntities.STRUT.get(), StrutBlockEntityRenderer::new);
        StrutsFlywheelCompatLoader.registerStrutVisual(TestBlockEntities.STRUT.get());
    }

    @SubscribeEvent
    public static void registerAdditionalModels(final ModelEvent.RegisterAdditional event) {
        event.register(TestStrutDefinitions.NORMAL_MODEL.segmentModelLocation());
        event.register(TestStrutDefinitions.CABLE_MODEL.segmentModelLocation());
    }
}
