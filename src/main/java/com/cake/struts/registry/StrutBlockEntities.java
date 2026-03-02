package com.cake.struts.registry;

import com.cake.struts.content.block.StrutBlock;
import com.cake.struts.content.block.StrutBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

public class StrutBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "struts");

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StrutBlockEntity>> GIRDER_STRUT =
            BLOCK_ENTITY_TYPES.register("girder_strut", () -> BlockEntityType.Builder
                    .of(StrutBlockEntity::new, resolveValidGirderStrutBlocks())
                    .build(null));

    private static Block[] resolveValidGirderStrutBlocks() {
        final List<Block> blocks = new ArrayList<>();

        addIfPresent(blocks, "bits_n_bobs", "girder_strut");
        addIfPresent(blocks, "bits_n_bobs", "weathered_girder_strut");
        addIfPresent(blocks, "bits_n_bobs", "wooden_girder_strut");
        addIfPresent(blocks, "bits_n_bobs", "cable_girder_strut");

        addIfPresent(blocks, "struts", "girder_strut");
        addIfPresent(blocks, "struts", "weathered_girder_strut");
        addIfPresent(blocks, "struts", "wooden_girder_strut");
        addIfPresent(blocks, "struts", "cable_girder_strut");

        return blocks.toArray(Block[]::new);
    }

    private static void addIfPresent(final List<Block> blocks, final String namespace, final String path) {
        final ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, path);
        final Block block = BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
        if (block instanceof StrutBlock) {
            blocks.add(block);
        }
    }

    public static void register(final IEventBus modBus) {
        BLOCK_ENTITY_TYPES.register(modBus);
    }
}