package blue.deniz.shitfixes.mixin;

import blue.deniz.shitfixes.FlashbackRecording;
import dev.ryanhcode.sable.network.udp.AddressedSableUDPPacket;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Once a client answers Sable's UDP handshake, the server sends sub-level movement over UDP instead of
// the game connection, so Flashback never sees it. The UDP packets that are also payloads are the same
// ones the server would otherwise have sent over the game connection.
@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.network.udp.handler.SableUDPChannelHandlerClient")
abstract class RecordSableUdpMovementMixin {
    @Inject(
            method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Ldev/ryanhcode/sable/network/udp/AddressedSableUDPPacket;)V",
            at = @At("HEAD"),
            require = 0)
    private void shitfixes$recordAsGamePayload(ChannelHandlerContext ctx, AddressedSableUDPPacket msg, CallbackInfo ci) {
        if (msg.packet() instanceof CustomPacketPayload payload) {
            FlashbackRecording.writeReceivedPacket(new ClientboundCustomPayloadPacket(payload));
        }
    }
}
