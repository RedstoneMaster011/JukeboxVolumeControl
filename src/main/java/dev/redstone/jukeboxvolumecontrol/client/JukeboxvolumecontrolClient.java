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
        BlockEntityRendererFactories.register(
                BlockEntityType.JUKEBOX,
                JukeboxColorRenderer::new
        );

        ClientPlayNetworking.registerGlobalReceiver(JukeboxSettingsPayload.ID,
                (payload, context) -> {
                    context.client().execute(() -> {
                        JukeboxVolumeManager.setSettings(
                                payload.pos(), payload.volume(), payload.pitch(), payload.color());

                        MinecraftClient client = MinecraftClient.getInstance();
                        if (client.getSoundManager() != null) {
                            client.getSoundManager().refreshSoundVolumes(SoundCategory.RECORDS);
                        }
                    });
                });

        ClientPlayNetworking.registerGlobalReceiver(JukeboxRemovePayload.ID,
                (payload, context) -> {
                    context.client().execute(() -> {
                        JukeboxVolumeManager.remove(payload.pos());
                    });
                });
    }
}
