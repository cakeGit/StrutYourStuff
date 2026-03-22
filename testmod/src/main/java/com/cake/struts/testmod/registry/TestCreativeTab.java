package com.cake.struts.testmod.registry;

import com.cake.struts.testmod.StrutsTestMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class TestCreativeTab {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, StrutsTestMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> TAB = CREATIVE_TABS.register("struts_test_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.literal("Struts Test"))
                    .icon(() -> new ItemStack(TestBlocks.GIRDER_STRUT_ITEM.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(TestBlocks.GIRDER_STRUT_ITEM.get());
                        output.accept(TestBlocks.CABLE_GIRDER_STRUT_ITEM.get());
                    })
                    .build()
    );

    private TestCreativeTab() {
    }
}
