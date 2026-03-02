package com.cake.struts.registry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.UnaryOperator;

public class StrutDataComponents {

    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, "struts");

    public static final DataComponentType<BlockPos> GIRDER_STRUT_FROM = register(
            "girder_strut_from",
            b -> b.persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC));

    public static final DataComponentType<Direction> GIRDER_STRUT_FROM_FACE = register(
            "girder_strut_from_face",
            b -> b.persistent(Direction.CODEC).networkSynchronized(Direction.STREAM_CODEC));

    private static <T> DataComponentType<T> register(final String name, final UnaryOperator<DataComponentType.Builder<T>> op) {
        final DataComponentType<T> type = op.apply(DataComponentType.builder()).build();
        DATA_COMPONENTS.register(name, () -> type);
        return type;
    }

    private StrutDataComponents() {
    }
}
