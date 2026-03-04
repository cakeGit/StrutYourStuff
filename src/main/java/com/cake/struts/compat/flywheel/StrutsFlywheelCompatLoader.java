package com.cake.struts.compat.flywheel;

import com.cake.struts.content.block.StrutBlockEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class StrutsFlywheelCompatLoader {

    private static final String FLYWHEEL_ID = "flywheel";
    private static @Nullable Boolean flywheelLoaded;
    private static @Nullable FlywheelCompat compat;
    private static boolean initialized;

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        if (!isFlywheelLoaded()) {
            return;
        }
        compat = new FlywheelCompat();
    }

    public static void registerStrutVisual(final BlockEntityType<? extends StrutBlockEntity> blockEntityType) {
        if (!isFlywheelLoaded()) {
            return;
        }
        if (compat == null) {
            compat = new FlywheelCompat();
        }
        initialized = true;
        compat.register(blockEntityType);
    }

    public static boolean supportsVisualization(final @Nullable LevelAccessor level) {
        if (!isFlywheelLoaded() || compat == null) {
            return false;
        }
        return compat.supportsVisualization(level);
    }

    public static void queueUpdate(final @NotNull BlockEntity blockEntity) {
        if (!isFlywheelLoaded() || compat == null) {
            return;
        }
        compat.queueUpdate(blockEntity);
    }

    private static boolean isFlywheelLoaded() {
        if (flywheelLoaded != null) {
            return flywheelLoaded;
        }
        final ModList modList = ModList.get();
        flywheelLoaded = modList != null && modList.isLoaded(FLYWHEEL_ID);
        return flywheelLoaded;
    }
}
