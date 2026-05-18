package dev.redstone.jukeboxvolumecontrol.client;

import dev.redstone.jukeboxvolumecontrol.JukeboxVolumeManager;
import dev.redstone.jukeboxvolumecontrol.network.JukeboxRemovePayload;
import dev.redstone.jukeboxvolumecontrol.network.JukeboxSettingsPayload;
import dev.redstone.jukeboxvolumecontrol.network.JukeboxSoundIdPayload;
import dev.redstone.jukeboxvolumecontrol.network.JukeboxStopPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

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

        ClientPlayNetworking.registerGlobalReceiver(JukeboxSoundIdPayload.ID,
                (payload, context) -> context.client().execute(() ->
                        JukeboxVolumeManager.trackActiveSound(payload.pos(), payload.soundId())));

        ClientPlayNetworking.registerGlobalReceiver(JukeboxStopPayload.ID,
                (payload, context) -> context.client().execute(() ->
                        stopSoundAt(payload.pos())));

        ClientPlayNetworking.registerGlobalReceiver(JukeboxRemovePayload.ID,
                (payload, context) -> context.client().execute(() -> {
                    stopSoundAt(payload.pos());
                    JukeboxVolumeManager.remove(payload.pos());
                }));
    }

    private static void stopSoundAt(BlockPos pos) {
        Identifier soundId = JukeboxVolumeManager.popActiveSound(pos);
        MinecraftClient client = MinecraftClient.getInstance();
        if (soundId != null && client.getSoundManager() != null) {
            client.getSoundManager().stopSounds(soundId, SoundCategory.RECORDS);
        }
    }
}