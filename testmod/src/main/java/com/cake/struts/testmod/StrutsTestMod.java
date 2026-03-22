package com.cake.struts.testmod;

import com.cake.struts.testmod.registry.TestBlockEntities;
import com.cake.struts.testmod.registry.TestBlocks;
import com.cake.struts.testmod.registry.TestCreativeTab;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(StrutsTestMod.MOD_ID)
public class StrutsTestMod {

    public static final String MOD_ID = "struts_test";

    public StrutsTestMod() {
        final IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        TestBlocks.BLOCKS.register(modBus);
        TestBlocks.ITEMS.register(modBus);
        TestBlockEntities.BLOCK_ENTITY_TYPES.register(modBus);
        TestCreativeTab.CREATIVE_TABS.register(modBus);
    }
}
