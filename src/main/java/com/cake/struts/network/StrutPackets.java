package com.cake.struts.network;

import com.cake.struts.StrutYourStuff;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = StrutYourStuff.MOD_ID)
public class StrutPackets {

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1.0.0");
        registrar.playToServer(
                BreakStrutPacket.TYPE,
                BreakStrutPacket.CODEC,
                (payload, context) -> payload.handle(context)
        );
    }
}
