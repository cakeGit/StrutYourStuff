package com.cake.struts.testmod.registry;

import com.cake.struts.content.block.StrutBlockEntity;
import com.cake.struts.testmod.StrutsTestMod;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class TestBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, StrutsTestMod.MOD_ID);

    @SuppressWarnings({"ConstantConditions", "unchecked"})
    public static final RegistryObject<BlockEntityType<StrutBlockEntity>> STRUT =
            BLOCK_ENTITY_TYPES.register("girder_strut", () -> {
                final BlockEntityType<StrutBlockEntity>[] holder = new BlockEntityType[1];
                holder[0] = BlockEntityType.Builder.<StrutBlockEntity>of(
                        (pos, state) -> new StrutBlockEntity(holder[0], pos, state),
                        TestBlocks.GIRDER_STRUT.get(),
                        TestBlocks.CABLE_GIRDER_STRUT.get()
                ).build(null);
                return holder[0];
            });

    private TestBlockEntities() {
    }
}
