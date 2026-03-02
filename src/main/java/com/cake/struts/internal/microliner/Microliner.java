package com.cake.struts.internal.microliner;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "This code is modified and used under MIT licence, full credit goes to the Create / Ponder team."
 */
public class Microliner {

    private static final Microliner INSTANCE = new Microliner();
    private final Map<String, MicrolinerEntry> entries = new ConcurrentHashMap<>();

    public static Microliner get() {
        return INSTANCE;
    }

    public void showAABB(final String id, final AABB box, final MicrolinerParams params) {
        entries.put(id, new MicrolinerEntry(new MicrolinerAABBOutline(box), params));
    }

    public void tick() {
        entries.entrySet().removeIf(entry -> !entry.getValue().tick());
    }

    public void render(final PoseStack poseStack, final MultiBufferSource buffer, final Vec3 camera) {
        for (final MicrolinerEntry entry : entries.values()) {
            entry.outline().render(poseStack, buffer, camera, entry.params());
        }
    }
}
