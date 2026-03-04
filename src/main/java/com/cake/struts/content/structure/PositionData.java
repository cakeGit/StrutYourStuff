package com.cake.struts.content.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-block position data tracking merged and per-connection VoxelShapes
 * for all strut connections that pass through that position.
 */
class PositionData {

    private static final double RAY_EPSILON = 1e-7;

    final Map<ConnectionKey, VoxelShape> perConnectionShapes = new HashMap<>();
    VoxelShape mergedShape = Shapes.empty();

    void add(final ConnectionKey key, final VoxelShape shape) {
        perConnectionShapes.put(key, shape);
        recompute();
    }

    void remove(final ConnectionKey key) {
        perConnectionShapes.remove(key);
        recompute();
    }

    private void recompute() {
        mergedShape = Shapes.empty();
        for (final VoxelShape shape : perConnectionShapes.values()) {
            mergedShape = Shapes.or(mergedShape, shape);
        }
        mergedShape = mergedShape.optimize();
    }

    VoxelShape getOutlineShape(final BlockPos pos, final CollisionContext context) {
        if (perConnectionShapes.isEmpty()) {
            return Shapes.empty();
        }
        final @Nullable Entity entity = context instanceof final EntityCollisionContext ec ? ec.getEntity() : null;
        if (entity == null) {
            return mergedShape;
        }
        final Vec3 direction = entity.getLookAngle();
        if (direction.lengthSqr() <= RAY_EPSILON) {
            return mergedShape;
        }
        final Vec3 origin = entity.getEyePosition(1.0F);
        final Vec3 rayDirection = direction.normalize();
        double bestDistance = Double.POSITIVE_INFINITY;
        VoxelShape bestShape = Shapes.empty();
        for (final VoxelShape shape : perConnectionShapes.values()) {
            final double distance = intersectShape(origin, rayDirection, pos, shape);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestShape = shape;
            }
        }
        return bestShape.isEmpty() ? mergedShape : bestShape;
    }

    @Nullable ConnectionKey getTargetedConnection(final BlockPos pos, final Vec3 origin, final Vec3 direction) {
        if (perConnectionShapes.isEmpty()) {
            return null;
        }
        if (direction.lengthSqr() <= RAY_EPSILON) {
            return null;
        }
        final Vec3 rayDirection = direction.normalize();
        double bestDistance = Double.POSITIVE_INFINITY;
        ConnectionKey bestKey = null;
        for (final Map.Entry<ConnectionKey, VoxelShape> entry : perConnectionShapes.entrySet()) {
            final double distance = intersectShape(origin, rayDirection, pos, entry.getValue());
            if (distance < bestDistance) {
                bestDistance = distance;
                bestKey = entry.getKey();
            }
        }
        return bestDistance == Double.POSITIVE_INFINITY ? null : bestKey;
    }

    private static double intersectShape(final Vec3 origin, final Vec3 direction, final BlockPos pos, final VoxelShape shape) {
        double bestDistance = Double.POSITIVE_INFINITY;
        for (final AABB localBox : shape.toAabbs()) {
            final AABB worldBox = localBox.move(pos);
            final double distance = intersectAabb(origin, direction, worldBox);
            if (distance < bestDistance) {
                bestDistance = distance;
            }
        }
        return bestDistance;
    }

    private static double intersectAabb(final Vec3 origin, final Vec3 direction, final AABB aabb) {
        double tMin = 0.0;
        double tMax = Double.POSITIVE_INFINITY;

        final double dirX = direction.x;
        if (Math.abs(dirX) < RAY_EPSILON) {
            if (origin.x < aabb.minX || origin.x > aabb.maxX) return Double.POSITIVE_INFINITY;
        } else {
            final double invDir = 1.0 / dirX;
            double t1 = (aabb.minX - origin.x) * invDir;
            double t2 = (aabb.maxX - origin.x) * invDir;
            if (t1 > t2) {
                final double tmp = t1;
                t1 = t2;
                t2 = tmp;
            }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMax < tMin) return Double.POSITIVE_INFINITY;
        }

        final double dirY = direction.y;
        if (Math.abs(dirY) < RAY_EPSILON) {
            if (origin.y < aabb.minY || origin.y > aabb.maxY) return Double.POSITIVE_INFINITY;
        } else {
            final double invDir = 1.0 / dirY;
            double t1 = (aabb.minY - origin.y) * invDir;
            double t2 = (aabb.maxY - origin.y) * invDir;
            if (t1 > t2) {
                final double tmp = t1;
                t1 = t2;
                t2 = tmp;
            }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMax < tMin) return Double.POSITIVE_INFINITY;
        }

        final double dirZ = direction.z;
        if (Math.abs(dirZ) < RAY_EPSILON) {
            if (origin.z < aabb.minZ || origin.z > aabb.maxZ) return Double.POSITIVE_INFINITY;
        } else {
            final double invDir = 1.0 / dirZ;
            double t1 = (aabb.minZ - origin.z) * invDir;
            double t2 = (aabb.maxZ - origin.z) * invDir;
            if (t1 > t2) {
                final double tmp = t1;
                t1 = t2;
                t2 = tmp;
            }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMax < tMin) return Double.POSITIVE_INFINITY;
        }

        if (tMax < 0) return Double.POSITIVE_INFINITY;
        return tMin < 0 ? 0.0 : tMin;
    }
}
