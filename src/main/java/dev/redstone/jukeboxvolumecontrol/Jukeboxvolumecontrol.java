package dev.redstone.jukeboxvolumecontrol;

import dev.redstone.jukeboxvolumecontrol.network.JukeboxRemovePayload;
import dev.redstone.jukeboxvolumecontrol.network.JukeboxSettingsPayload;
import dev.redstone.jukeboxvolumecontrol.network.JukeboxSoundIdPayload;
import dev.redstone.jukeboxvolumecontrol.network.JukeboxStopPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.JukeboxBlock;
import net.minecraft.block.entity.JukeboxBlockEntity;
import net.minecraft.block.jukebox.JukeboxSong;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.Map;

public class Jukeboxvolumecontrol implements ModInitializer {

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(JukeboxSettingsPayload.ID, JukeboxSettingsPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(JukeboxSettingsPayload.ID, JukeboxSettingsPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(JukeboxRemovePayload.ID, JukeboxRemovePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(JukeboxSoundIdPayload.ID, JukeboxSoundIdPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(JukeboxStopPayload.ID, JukeboxStopPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(JukeboxSettingsPayload.ID,
                (payload, context) -> {
                    BlockPos pos  = payload.pos();
                    float volume  = payload.volume();
                    float pitch   = payload.pitch();
                    int color     = payload.color();

                    context.server().execute(() -> {
                        JukeboxVolumeManager.setSettings(pos, volume, pitch, color);

                        JukeboxSettingsPayload broadcast = new JukeboxSettingsPayload(pos, volume, pitch, color);
                        for (ServerPlayerEntity player : context.server().getPlayerManager().getPlayerList()) {
                            ServerPlayNetworking.send(player, broadcast);
                        }
                    });
                });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            server.execute(() -> {
                for (Map.Entry<BlockPos, JukeboxVolumeManager.JukeboxSettings> entry
                        : JukeboxVolumeManager.getAllSettings().entrySet()) {
                    JukeboxVolumeManager.JukeboxSettings s = entry.getValue();
                    ServerPlayNetworking.send(player,
                            new JukeboxSettingsPayload(
                                    entry.getKey(), s.volume(), s.pitch(), s.color()));
                }

                for (ServerWorld world : server.getWorlds()) {
                    for (BlockPos pos : JukeboxVolumeManager.getAllSettings().keySet()) {
                        if (!world.isChunkLoaded(ChunkPos.toLong(
                                pos.getX() >> 4, pos.getZ() >> 4))) continue;

                        if (!(world.getBlockState(pos).getBlock() instanceof JukeboxBlock)) continue;
                        if (!(world.getBlockEntity(pos) instanceof JukeboxBlockEntity jukebox)) continue;

                        if (!jukebox.getManager().isPlaying()) continue;

                        var songEntry = JukeboxSong.getSongEntryFromStack(
                                world.getRegistryManager(), jukebox.getStack());
                        if (songEntry.isEmpty()) continue;

                        RegistryEntry<SoundEvent> soundEntry = songEntry.get().value().soundEvent();
                        JukeboxVolumeManager.JukeboxSettings settings =
                                JukeboxVolumeManager.getSettings(pos);

                        player.networkHandler.sendPacket(new PlaySoundS2CPacket(
                                soundEntry,
                                SoundCategory.RECORDS,
                                pos.getX() + 0.5,
                                pos.getY() + 0.5,
                                pos.getZ() + 0.5,
                                settings.volume(),
                                settings.pitch(),
                                0L
                        ));

                        ServerPlayNetworking.send(player,
                                new JukeboxSoundIdPayload(pos.toImmutable(),
                                        soundEntry.value().id()));
                    }
                }
            });
        });
    }
}