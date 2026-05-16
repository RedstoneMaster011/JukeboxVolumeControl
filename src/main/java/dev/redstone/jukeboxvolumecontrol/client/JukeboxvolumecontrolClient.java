package dev.redstone.jukeboxvolumecontrol.client;

import dev.redstone.jukeboxvolumecontrol.JukeboxVolumeManager;
import dev.redstone.jukeboxvolumecontrol.network.JukeboxRemovePayload;
import dev.redstone.jukeboxvolumecontrol.network.JukeboxSettingsPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.sound.SoundCategory;

public class JukeboxvolumecontrolClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Register the color renderer
        BlockEntityRendererFactories.register(
                BlockEntityType.JUKEBOX,
                JukeboxColorRenderer::new
        );

        // Receive settings broadcast from the server → apply locally
        ClientPlayNetworking.registerGlobalReceiver(JukeboxSettingsPayload.ID,
                (payload, context) -> {
                    context.client().execute(() -> {
                        JukeboxVolumeManager.setSettings(
                                payload.pos(), payload.volume(), payload.pitch(), payload.color());

                        // Refresh audio so changes take effect immediately
                        MinecraftClient client = MinecraftClient.getInstance();
                        if (client.getSoundManager() != null) {
                            client.getSoundManager().refreshSoundVolumes(SoundCategory.RECORDS);
                        }
                    });
                });

        // Receive remove notification from the server → clear locally
        ClientPlayNetworking.registerGlobalReceiver(JukeboxRemovePayload.ID,
                (payload, context) -> {
                    context.client().execute(() -> {
                        JukeboxVolumeManager.remove(payload.pos());
                    });
                });
    }
}
