package com.cake.struts.network;

import com.cake.struts.StrutYourStuff;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class StrutPackets {

    private static final String PROTOCOL_VERSION = "1.0.0";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(StrutYourStuff.MOD_ID, "network"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int id = 0;

    public static void register() {
        CHANNEL.messageBuilder(BreakStrutPacket.class, id++)
                .encoder(BreakStrutPacket::encode)
                .decoder(BreakStrutPacket::decode)
                .consumerMainThread(BreakStrutPacket::handle)
                .add();
    }
}
