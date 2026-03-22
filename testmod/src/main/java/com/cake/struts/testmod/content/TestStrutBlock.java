package com.cake.struts.testmod.content;

import com.cake.struts.content.CableStrutInfo;
import com.cake.struts.content.StrutModelType;
import com.cake.struts.content.block.StrutBlock;
import com.cake.struts.content.block.StrutBlockEntity;
import com.cake.struts.testmod.registry.TestBlockEntities;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

public class TestStrutBlock extends StrutBlock {

    public TestStrutBlock(Properties properties, StrutModelType modelType) {
        super(properties, modelType);
    }

    public TestStrutBlock(Properties properties, StrutModelType modelType, @Nullable CableStrutInfo cableRenderInfo) {
        super(properties, modelType, cableRenderInfo);
    }

    @Override
    protected BlockEntityType<? extends StrutBlockEntity> getStrutBlockEntityType() {
        return TestBlockEntities.STRUT.get();
    }
}
