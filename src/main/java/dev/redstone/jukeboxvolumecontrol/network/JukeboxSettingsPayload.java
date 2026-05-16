package dev.redstone.jukeboxvolumecontrol.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Sent in BOTH directions:
 *  - Client → Server: player confirmed new settings in the GUI
 *  - Server → Client: broadcast updated settings to all players
 */
public record JukeboxSettingsPayload(BlockPos pos, float volume, float pitch, int color)
        implements CustomPayload {

    public static final Id<JukeboxSettingsPayload> ID =
            new Id<>(Identifier.of("jukeboxvolumecontrol", "settings"));

    public static final PacketCodec<RegistryByteBuf, JukeboxSettingsPayload> CODEC =
            PacketCodec.tuple(
                    BlockPos.PACKET_CODEC,              JukeboxSettingsPayload::pos,
                    PacketCodecs.FLOAT,                 JukeboxSettingsPayload::volume,
                    PacketCodecs.FLOAT,                 JukeboxSettingsPayload::pitch,
                    PacketCodecs.INTEGER,               JukeboxSettingsPayload::color,
                    JukeboxSettingsPayload::new
            );

    @Override
    public Id<JukeboxSettingsPayload> getId() { return ID; }
}
