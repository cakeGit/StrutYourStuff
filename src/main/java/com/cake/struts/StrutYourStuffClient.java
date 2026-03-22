package com.cake.struts;

import com.cake.struts.content.StrutModelManipulator;
import com.cake.struts.content.StrutPlacementEffects;
import com.cake.struts.content.block.StrutBlockEntity;
import com.cake.struts.content.shape.StrutInteractionHandler;
import com.cake.struts.internal.microliner.Microliner;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;

public class StrutYourStuffClient {

    public StrutYourStuffClient(final IEventBus modBus) {
        StrutBlockEntity.CLIENT_UPDATE_LISTENER = StrutInteractionHandler::updateOutlineShapes;
        StrutBlockEntity.CLIENT_REMOVE_LISTENER = StrutInteractionHandler::removeOutlineShapes;
    }

    private static final ResourceManagerReloadListener STRUT_MESH_RELOAD_LISTENER = new ResourceManagerReloadListener() {
        @Override
        public void onResourceManagerReload(final ResourceManager resourceManager) {
            StrutModelManipulator.invalidateMeshes();
        }
    };

    @Mod.EventBusSubscriber(modid = "struts", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {

        @SubscribeEvent
        public static void onRegisterReloadListeners(final RegisterClientReloadListenersEvent event) {
            event.registerReloadListener(STRUT_MESH_RELOAD_LISTENER);
        }
    }

    @Mod.EventBusSubscriber(modid = "struts", value = Dist.CLIENT)
    public static class ForgeEvents {

        @SubscribeEvent
        public static void onClientTick(final TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.START) return;
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
