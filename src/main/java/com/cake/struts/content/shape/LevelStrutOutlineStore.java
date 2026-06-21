package com.cake.struts.content.shape;

import com.cake.struts.content.block.StrutBlock;
import com.cake.struts.content.structure.ConnectionKey;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side per-level store mapping each registered {@link ConnectionKey}
 * to its {@link StrutConnectionShape} for hit-testing and outline rendering.
 */
class LevelStrutOutlineStore {

    private static final long VALIDATE_INTERVAL_TICKS = 10L;

    private final Map<ConnectionKey, StrutConnectionShape> shapes = new ConcurrentHashMap<>();
    private long nextValidationGameTime = Long.MIN_VALUE;

    void put(final ConnectionKey key, final StrutConnectionShape shape) {
        this.shapes.put(key, shape);
    }

    void remove(final ConnectionKey key) {
        this.shapes.remove(key);
    }

    void removeAllFor(final BlockPos pos) {
        this.shapes.keySet().removeIf(key -> key.a().equals(pos) || key.b().equals(pos));
    }

    Iterable<Map.Entry<ConnectionKey, StrutConnectionShape>> entries() {
        return this.shapes.entrySet();
    }

    /**
     * Periodically removes entries whose anchor blocks are no longer present as
     * {@link StrutBlock} instances in the level (e.g. after the block was broken).
     */
    void validate(final Level level) {
        final long gameTime = level.getGameTime();
        if (gameTime < this.nextValidationGameTime) {
            return;
        }
        this.nextValidationGameTime = gameTime + VALIDATE_INTERVAL_TICKS;

        this.shapes.keySet().removeIf(key -> {
            if (!level.isLoaded(key.a()) || !level.isLoaded(key.b())) {
                return false; // don't evict unloaded chunks; they may just be temporarily unavailable
            }
            return !(level.getBlockState(key.a()).getBlock() instanceof StrutBlock)
                    && !(level.getBlockState(key.b()).getBlock() instanceof StrutBlock);
        });
    }
}
