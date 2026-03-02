package com.cake.struts.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class StrutItemTags {

    public static final TagKey<Item> WRENCHES = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("struts", "wrenches"));

}
