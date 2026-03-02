package com.cake.struts.registry;

import com.cake.struts.content.structure.GirderStrutStructureBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class StrutBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("struts");

    public static final DeferredBlock<GirderStrutStructureBlock> GIRDER_STRUT_STRUCTURE =
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
