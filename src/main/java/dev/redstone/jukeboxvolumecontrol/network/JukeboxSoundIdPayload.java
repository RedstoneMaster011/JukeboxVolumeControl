package dev.redstone.jukeboxvolumecontrol.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record JukeboxSoundIdPayload(BlockPos pos, Identifier soundId) implements CustomPayload {

    public static final Id<JukeboxSoundIdPayload> ID =
            new Id<>(Identifier.of("jukeboxvolumecontrol", "sound_id"));

    public static final PacketCodec<RegistryByteBuf, JukeboxSoundIdPayload> CODEC =
            PacketCodec.tuple(
                    BlockPos.PACKET_CODEC,      JukeboxSoundIdPayload::pos,
                    Identifier.PACKET_CODEC,    JukeboxSoundIdPayload::soundId,
                    JukeboxSoundIdPayload::new
            );

    @Override
    public Id<JukeboxSoundIdPayload> getId() { return ID; }
}