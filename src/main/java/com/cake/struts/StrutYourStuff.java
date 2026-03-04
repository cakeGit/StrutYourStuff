package com.cake.struts;

import com.cake.struts.registry.StrutBlocks;
import com.cake.struts.registry.StrutDataComponents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;

import static com.cake.struts.StrutYourStuff.MOD_ID;

@Mod(MOD_ID)
public class StrutYourStuff {

    public static final String MOD_ID = "struts";

    public StrutYourStuff(final IEventBus modBus) {
        StrutBlocks.BLOCKS.register(modBus);
        StrutDataComponents.DATA_COMPONENTS.register(modBus);
        if (FMLLoader.getDist() == Dist.CLIENT) {
            new StrutYourStuffClient(modBus);
        }
    }

}
