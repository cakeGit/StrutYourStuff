package com.cake.struts.internal.microliner;

import net.minecraft.world.phys.Vec3;

@FunctionalInterface
public interface MicrolinerCoordinateTransform {

    MicrolinerCoordinateTransform IDENTITY = point -> point;

    Vec3 transform(Vec3 point);

    static MicrolinerCoordinateTransform identity() {
        return IDENTITY;
    }
}
