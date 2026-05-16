package dev.redstone.jukeboxvolumecontrol.client;

import dev.redstone.jukeboxvolumecontrol.JukeboxVolumeManager;
import net.minecraft.client.sound.Sound;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.sound.WeightedSoundSet;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.Nullable;

public class ScaledSoundInstance implements SoundInstance {

    private final SoundInstance wrapped;
    private final BlockPos jukeboxPos;

    public ScaledSoundInstance(SoundInstance wrapped, BlockPos jukeboxPos) {
        this.wrapped    = wrapped;
        this.jukeboxPos = jukeboxPos;
    }

    @Override
    public float getVolume() {
        return wrapped.getVolume() * JukeboxVolumeManager.getVolume(jukeboxPos);
    }

    @Override
    public float getPitch() {
        return wrapped.getPitch() * JukeboxVolumeManager.getPitch(jukeboxPos);
    }

    @Override public Identifier getId()                                          { return wrapped.getId(); }
    @Override public @Nullable WeightedSoundSet getSoundSet(SoundManager sm)    { return wrapped.getSoundSet(sm); }
    @Override public Sound getSound()                                            { return wrapped.getSound(); }
    @Override public SoundCategory getCategory()                                 { return wrapped.getCategory(); }
    @Override public boolean isRepeatable()                                      { return wrapped.isRepeatable(); }
    @Override public boolean isRelative()                                        { return wrapped.isRelative(); }
    @Override public int getRepeatDelay()                                        { return wrapped.getRepeatDelay(); }
    @Override public double getX()                                               { return wrapped.getX(); }
    @Override public double getY()                                               { return wrapped.getY(); }
    @Override public double getZ()                                               { return wrapped.getZ(); }
    @Override public AttenuationType getAttenuationType()                        { return wrapped.getAttenuationType(); }
    @Override public boolean shouldAlwaysPlay()                                  { return wrapped.shouldAlwaysPlay(); }
}
