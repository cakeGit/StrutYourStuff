package com.cake.struts.network;

import com.cake.struts.content.StrutBreakerHelper;
import com.cake.struts.content.structure.ConnectionKey;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class BreakStrutPacket {

    private final ConnectionKey target;
    private final boolean isWrench;

    public BreakStrutPacket(final ConnectionKey target, final boolean isWrench) {
        this.target = target;
        this.isWrench = isWrench;
    }

    public static void encode(final BreakStrutPacket packet, final FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.target.a());
        buf.writeBlockPos(packet.target.b());
        buf.writeBoolean(packet.isWrench);
    }

    public static BreakStrutPacket decode(final FriendlyByteBuf buf) {
        return new BreakStrutPacket(new ConnectionKey(buf.readBlockPos(), buf.readBlockPos()), buf.readBoolean());
    }

    public static void handle(final BreakStrutPacket packet, final Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            final Player player = ctx.get().getSender();
            if (player != null) {
                StrutBreakerHelper.breakStrut(player, packet.target, packet.isWrench);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
