package com.cake.struts;

import com.cake.struts.network.StrutPackets;
import com.cake.struts.registry.StrutBlocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;

import static com.cake.struts.StrutYourStuff.MOD_ID;

@Mod(MOD_ID)
public class StrutYourStuff {

    public static final String MOD_ID = "struts";

    public StrutYourStuff() {
        final IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        StrutBlocks.BLOCKS.register(modBus);
        StrutPackets.register();
        if (FMLLoader.getDist() == Dist.CLIENT) {
            new StrutYourStuffClient(modBus);
        }
    }
}
