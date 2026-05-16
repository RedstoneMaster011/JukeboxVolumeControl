package dev.redstone.jukeboxvolumecontrol.mixin;

import dev.redstone.jukeboxvolumecontrol.JukeboxVolumeManager;
import net.minecraft.client.sound.Channel;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(SoundSystem.class)
public abstract class SoundSystemMixin {

    @Shadow private Map<SoundInstance, Channel.SourceManager> sources;
    @Shadow private float getAdjustedVolume(SoundInstance sound) { return 0; }

    @Inject(method = "refreshSoundVolumes", at = @At("TAIL"))
    private void onRefreshSoundVolumes(SoundCategory category, CallbackInfo ci) {
        applyJukeboxSettings(category);
    }

    @Inject(method = "tick()V", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        applyJukeboxSettings(SoundCategory.RECORDS);
    }

    private void applyJukeboxSettings(SoundCategory category) {
        if (sources == null) return;
        if (category != SoundCategory.RECORDS && category != SoundCategory.MASTER) return;

        for (Map.Entry<SoundInstance, Channel.SourceManager> entry : sources.entrySet()) {
            SoundInstance sound = entry.getKey();
            if (sound.getCategory() != SoundCategory.RECORDS) continue;

            BlockPos pos = BlockPos.ofFloored(sound.getX(), sound.getY(), sound.getZ());
            if (!JukeboxVolumeManager.hasCustomVolume(pos)) continue;

            JukeboxVolumeManager.JukeboxSettings settings = JukeboxVolumeManager.getSettings(pos);
            float baseVolume = getAdjustedVolume(sound);

            entry.getValue().run(source -> {
                source.setVolume(baseVolume * settings.volume());
                source.setPitch(sound.getPitch() * settings.pitch());
            });
        }
    }
}
