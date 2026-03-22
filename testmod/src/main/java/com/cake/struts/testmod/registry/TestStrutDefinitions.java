package com.cake.struts.testmod.registry;

import com.cake.struts.content.CableStrutInfo;
import com.cake.struts.content.StrutModelType;
import com.cake.struts.testmod.StrutsTestMod;
import net.minecraft.resources.ResourceLocation;

public class TestStrutDefinitions {

    public static final StrutModelType NORMAL_MODEL = new StrutModelType(
            new ResourceLocation(StrutsTestMod.MOD_ID, "block/girder_strut/girder"),
            new ResourceLocation(StrutsTestMod.MOD_ID, "block/industrial_iron_block")
    );

    public static final StrutModelType CABLE_MODEL = new StrutModelType(
            new ResourceLocation(StrutsTestMod.MOD_ID, "block/girder_strut/cable"),
            new ResourceLocation(StrutsTestMod.MOD_ID, "block/industrial_iron_block"),
            2, 2
    );

    public static final CableStrutInfo CABLE_INFO = new CableStrutInfo(0.05f);

    private TestStrutDefinitions() {
    }
}
