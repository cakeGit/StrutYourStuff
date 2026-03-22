package com.cake.struts.testmod.registry;

import com.cake.struts.content.block.StrutBlockItem;
import com.cake.struts.testmod.StrutsTestMod;
import com.cake.struts.testmod.content.TestCableStrutBlock;
import com.cake.struts.testmod.content.TestStrutBlock;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class TestBlocks {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, StrutsTestMod.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, StrutsTestMod.MOD_ID);

    public static final RegistryObject<TestStrutBlock> GIRDER_STRUT = BLOCKS.register("girder_strut",
            () -> new TestStrutBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3f, 6f)
                            .noOcclusion()
                            .sound(SoundType.NETHERITE_BLOCK),
                    TestStrutDefinitions.NORMAL_MODEL
            ));

    public static final RegistryObject<TestCableStrutBlock> CABLE_GIRDER_STRUT = BLOCKS.register("cable_girder_strut",
            () -> new TestCableStrutBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3f, 6f)
                            .noOcclusion()
                            .sound(SoundType.CHAIN),
                    TestStrutDefinitions.CABLE_MODEL,
                    TestStrutDefinitions.CABLE_INFO
            ));

    public static final RegistryObject<StrutBlockItem> GIRDER_STRUT_ITEM = ITEMS.register("girder_strut",
            () -> new StrutBlockItem(GIRDER_STRUT.get(), new Item.Properties()));

    public static final RegistryObject<StrutBlockItem> CABLE_GIRDER_STRUT_ITEM = ITEMS.register("cable_girder_strut",
            () -> new StrutBlockItem(CABLE_GIRDER_STRUT.get(), new Item.Properties()));

    private TestBlocks() {
    }
}
