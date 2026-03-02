package com.cake.struts.content.structure;

import com.cake.struts.internal.util.ChunkedMap;
import net.minecraft.world.level.ChunkPos;

/**
 * Wraps the geometry and chunk membership for a single registered strut connection,
 * for use in the {@link ChunkedMap} eviction system.
 */
record GirderStrutConnectionShape(ConnectionKey key,
                                  BlockyStrutLineGeometry geometry,
                                  ChunkPos[] chunks) implements ChunkedMap.IChunkedObject {

    @Override
    public ChunkPos[] getChunks() {
        return chunks;
    }
}
