package dev.redstone.jukeboxvolumecontrol.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record JukeboxRemovePayload(BlockPos pos) implements CustomPayload {

    public static final Id<JukeboxRemovePayload> ID =
            new Id<>(Identifier.of("jukeboxvolumecontrol", "remove"));

    public static final PacketCodec<RegistryByteBuf, JukeboxRemovePayload> CODEC =
            PacketCodec.tuple(
                    BlockPos.PACKET_CODEC, JukeboxRemovePayload::pos,
                    JukeboxRemovePayload::new
            );

    @Override
    public Id<JukeboxRemovePayload> getId() { return ID; }
}
