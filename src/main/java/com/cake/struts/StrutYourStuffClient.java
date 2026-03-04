package com.cake.struts;

import com.cake.struts.content.StrutModelManipulator;
import com.cake.struts.content.StrutPlacementEffects;
import com.cake.struts.content.block.StrutBlockEntity;
import com.cake.struts.content.shape.StrutInteractionHandler;
import com.cake.struts.internal.microliner.Microliner;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public class StrutYourStuffClient {

    public StrutYourStuffClient(final IEventBus modBus) {
        // Wire up client-side block entity listeners for BigOutline shape tracking.
        StrutBlockEntity.CLIENT_UPDATE_LISTENER = StrutInteractionHandler::updateOutlineShapes;
        StrutBlockEntity.CLIENT_REMOVE_LISTENER = StrutInteractionHandler::removeOutlineShapes;
    }

    private static final ResourceManagerReloadListener STRUT_MESH_RELOAD_LISTENER = new ResourceManagerReloadListener() {
        @Override
        public void onResourceManagerReload(final ResourceManager resourceManager) {
            StrutModelManipulator.invalidateMeshes();
        }
    };

    @EventBusSubscriber(modid = "struts", value = Dist.CLIENT)
    public static class Events {

        @SubscribeEvent
        public static void onRegisterReloadListeners(final RegisterClientReloadListenersEvent event) {
            event.registerReloadListener(STRUT_MESH_RELOAD_LISTENER);
        }

        @SubscribeEvent
        public static void onClientTick(final ClientTickEvent.Pre event) {
            Microliner.get().tick();
            final Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                StrutPlacementEffects.tick(minecraft.player);
            }
        }

        @SubscribeEvent
        public static void onRenderLevelStage(final RenderLevelStageEvent event) {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
                Microliner.get().render(event.getPoseStack(), Minecraft.getInstance().renderBuffers().bufferSource(), event.getCamera().getPosition());
            }
        }
    }
}
