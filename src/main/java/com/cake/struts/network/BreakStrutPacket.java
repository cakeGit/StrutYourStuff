package com.cake.struts.network;

import com.cake.struts.StrutYourStuff;
import com.cake.struts.content.StrutBreakerHelper;
import com.cake.struts.content.structure.ConnectionKey;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record BreakStrutPacket(ConnectionKey target, boolean isWrench) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<BreakStrutPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(StrutYourStuff.MOD_ID, "break_strut"));

    public static final StreamCodec<FriendlyByteBuf, BreakStrutPacket> CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeBlockPos(packet.target().a());
                buf.writeBlockPos(packet.target().b());
                buf.writeBoolean(packet.isWrench());
            },
            buf -> new BreakStrutPacket(new ConnectionKey(buf.readBlockPos(), buf.readBlockPos()), buf.readBoolean())
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(final IPayloadContext context) {
        context.enqueueWork(() -> {
            final Player player = context.player();
            StrutBreakerHelper.breakStrut(player, target, isWrench);
        });
    }
}
