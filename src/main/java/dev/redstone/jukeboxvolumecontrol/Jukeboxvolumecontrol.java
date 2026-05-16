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
        PayloadTypeRegistry.playC2S().register(JukeboxSettingsPayload.ID, JukeboxSettingsPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(JukeboxSettingsPayload.ID, JukeboxSettingsPayload.CODEC);

        PayloadTypeRegistry.playS2C().register(JukeboxRemovePayload.ID, JukeboxRemovePayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(JukeboxSettingsPayload.ID,
                (payload, context) -> {
                    BlockPos pos    = payload.pos();
                    float volume    = payload.volume();
                    float pitch     = payload.pitch();
                    int color       = payload.color();

                    context.server().execute(() -> {
                        JukeboxVolumeManager.setSettings(pos, volume, pitch, color);

                        JukeboxSettingsPayload broadcast = new JukeboxSettingsPayload(pos, volume, pitch, color);
                        for (ServerPlayerEntity player : context.server().getPlayerManager().getPlayerList()) {
                            ServerPlayNetworking.send(player, broadcast);
                        }
                    });
                });
    }
}
