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
        this.showAABB(id, box, MicrolinerCoordinateTransform.identity(), params);
    }

    public void showAABB(final String id, final AABB box, final MicrolinerCoordinateTransform transform, final MicrolinerParams params) {
        this.entries.put(id, new MicrolinerEntry(new MicrolinerAABBOutline(box), transform, params));
    }

    public void showOutline(final String id, final MicrolinerOutline outline, final MicrolinerParams params) {
        this.showOutline(id, outline, MicrolinerCoordinateTransform.identity(), params);
    }

    public void showOutline(final String id, final MicrolinerOutline outline, final MicrolinerCoordinateTransform transform, final MicrolinerParams params) {
        this.entries.put(id, new MicrolinerEntry(outline, transform, params));
    }

    public void tick() {
        this.entries.entrySet().removeIf(entry -> !entry.getValue().tick());
    }

    public void render(final PoseStack poseStack, final MultiBufferSource buffer, final Vec3 camera) {
        for (final MicrolinerEntry entry : this.entries.values()) {
            entry.outline().render(poseStack, buffer, camera, entry.transform(), entry.params());
        }
    }
}
