package dev.redstone.jukeboxvolumecontrol.mixin;

import dev.redstone.jukeboxvolumecontrol.JukeboxVolumeManager;
import dev.redstone.jukeboxvolumecontrol.network.JukeboxSoundIdPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.jukebox.JukeboxManager;
import net.minecraft.block.jukebox.JukeboxSong;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JukeboxManager.class)
public class JukeboxManagerMixin {

    @Shadow @Final private BlockPos pos;

    @Inject(method = "startPlaying", at = @At("TAIL"))
    private void onStartPlaying(WorldAccess world, RegistryEntry<JukeboxSong> song, CallbackInfo ci) {
        if (!(world instanceof ServerWorld serverWorld)) return;

        RegistryEntry<SoundEvent> soundEntry = song.value().soundEvent();
        JukeboxVolumeManager.JukeboxSettings settings = JukeboxVolumeManager.getSettings(pos);

        JukeboxSoundIdPayload idPayload = new JukeboxSoundIdPayload(
                pos.toImmutable(), soundEntry.value().id());

        for (ServerPlayerEntity player : serverWorld.getServer().getPlayerManager().getPlayerList()) {
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
            ServerPlayNetworking.send(player, idPayload);
        }
    }
}