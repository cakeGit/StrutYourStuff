package com.cake.struts.registry;

import com.cake.struts.content.structure.GirderStrutStructureBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class StrutBlocks {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "struts");

    public static final RegistryObject<GirderStrutStructureBlock> GIRDER_STRUT_STRUCTURE =
            BLOCKS.register("girder_strut_structure", () -> new GirderStrutStructureBlock(
                    BlockBehaviour.Properties.of()
                            .noLootTable()
                            .replaceable()
                            .noOcclusion()
                            .noCollission()
                            .pushReaction(PushReaction.DESTROY)
                            .mapColor(MapColor.METAL)
                            .strength(3f, 6f)
                            .sound(SoundType.NETHERITE_BLOCK)));

    private StrutBlocks() {
    }
}
