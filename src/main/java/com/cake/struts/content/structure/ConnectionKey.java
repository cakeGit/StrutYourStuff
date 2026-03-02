package com.cake.struts.content.structure;

import net.minecraft.core.BlockPos;

/**
 * Canonical sorted pair of anchor positions identifying a strut connection.
 * Order is normalised so that {@code a.compareTo(b) <= 0} always holds.
 */
public record ConnectionKey(BlockPos a, BlockPos b) {

    public ConnectionKey {
        if (a.compareTo(b) > 0) {
            final BlockPos temp = a;
            a = b;
            b = temp;
        }
    }
}
