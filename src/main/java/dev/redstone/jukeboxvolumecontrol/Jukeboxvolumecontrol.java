package dev.redstone.jukeboxvolumecontrol;

import dev.redstone.jukeboxvolumecontrol.network.JukeboxRemovePayload;
import dev.redstone.jukeboxvolumecontrol.network.JukeboxSettingsPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public class Jukeboxvolumecontrol implements ModInitializer {

    @Override
    public void onInitialize() {
        // Register both payload types for the play phase (bidirectional)
        PayloadTypeRegistry.playC2S().register(JukeboxSettingsPayload.ID, JukeboxSettingsPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(JukeboxSettingsPayload.ID, JukeboxSettingsPayload.CODEC);

        PayloadTypeRegistry.playS2C().register(JukeboxRemovePayload.ID, JukeboxRemovePayload.CODEC);

        // Server receives settings from one client → saves them → broadcasts to ALL clients
        ServerPlayNetworking.registerGlobalReceiver(JukeboxSettingsPayload.ID,
                (payload, context) -> {
                    BlockPos pos    = payload.pos();
                    float volume    = payload.volume();
                    float pitch     = payload.pitch();
                    int color       = payload.color();

                    // Run on the server thread
                    context.server().execute(() -> {
                        // Store on the server so newly joining players could be synced
                        JukeboxVolumeManager.setSettings(pos, volume, pitch, color);

                        // Broadcast to every connected player (including the sender)
                        JukeboxSettingsPayload broadcast = new JukeboxSettingsPayload(pos, volume, pitch, color);
                        for (ServerPlayerEntity player : context.server().getPlayerManager().getPlayerList()) {
                            ServerPlayNetworking.send(player, broadcast);
                        }
                    });
                });
    }
}
