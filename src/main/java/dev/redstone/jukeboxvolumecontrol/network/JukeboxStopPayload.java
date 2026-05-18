package dev.redstone.jukeboxvolumecontrol.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record JukeboxStopPayload(BlockPos pos) implements CustomPayload {

    public static final Id<JukeboxStopPayload> ID =
            new Id<>(Identifier.of("jukeboxvolumecontrol", "stop"));

    public static final PacketCodec<RegistryByteBuf, JukeboxStopPayload> CODEC =
            PacketCodec.tuple(
                    BlockPos.PACKET_CODEC, JukeboxStopPayload::pos,
                    JukeboxStopPayload::new
            );

    @Override
    public Id<JukeboxStopPayload> getId() { return ID; }
}