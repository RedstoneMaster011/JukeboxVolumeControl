package dev.redstone.jukeboxvolumecontrol;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class JukeboxVolumeManager {

    public record JukeboxSettings(float volume, float pitch, int color) {}

    private static final Map<BlockPos, JukeboxSettings> settingsMap = new HashMap<>();

    private static final Map<BlockPos, Identifier> activeSoundIds = new HashMap<>();

    public static void setVolume(BlockPos pos, float volume) {
        JukeboxSettings old = settingsMap.getOrDefault(pos.toImmutable(),
                new JukeboxSettings(1.0f, 1.0f, 0xFFFFFF));
        settingsMap.put(pos.toImmutable(),
                new JukeboxSettings(clampVolume(volume), old.pitch(), old.color()));
    }

    public static float getVolume(BlockPos pos) {
        JukeboxSettings s = settingsMap.get(pos.toImmutable());
        return s != null ? s.volume() : 1.0f;
    }

    public static boolean hasCustomVolume(BlockPos pos) {
        return settingsMap.containsKey(pos.toImmutable());
    }

    public static void setPitch(BlockPos pos, float pitch) {
        JukeboxSettings old = settingsMap.getOrDefault(pos.toImmutable(),
                new JukeboxSettings(1.0f, 1.0f, 0xFFFFFF));
        settingsMap.put(pos.toImmutable(),
                new JukeboxSettings(old.volume(), clampPitch(pitch), old.color()));
    }

    public static float getPitch(BlockPos pos) {
        JukeboxSettings s = settingsMap.get(pos.toImmutable());
        return s != null ? s.pitch() : 1.0f;
    }

    public static void setColor(BlockPos pos, int rgb) {
        JukeboxSettings old = settingsMap.getOrDefault(pos.toImmutable(),
                new JukeboxSettings(1.0f, 1.0f, 0xFFFFFF));
        settingsMap.put(pos.toImmutable(),
                new JukeboxSettings(old.volume(), old.pitch(), rgb & 0xFFFFFF));
    }

    public static int getColor(BlockPos pos) {
        JukeboxSettings s = settingsMap.get(pos.toImmutable());
        return s != null ? s.color() : 0xFFFFFF;
    }

    public static void setSettings(BlockPos pos, float volume, float pitch, int color) {
        settingsMap.put(pos.toImmutable(),
                new JukeboxSettings(clampVolume(volume), clampPitch(pitch), color & 0xFFFFFF));
    }

    public static JukeboxSettings getSettings(BlockPos pos) {
        return settingsMap.getOrDefault(pos.toImmutable(),
                new JukeboxSettings(1.0f, 1.0f, 0xFFFFFF));
    }

    public static Map<BlockPos, JukeboxSettings> getAllSettings() {
        return Collections.unmodifiableMap(settingsMap);
    }

    public static void remove(BlockPos pos) {
        settingsMap.remove(pos.toImmutable());
        activeSoundIds.remove(pos.toImmutable());
    }

    public static void trackActiveSound(BlockPos pos, Identifier soundId) {
        activeSoundIds.put(pos.toImmutable(), soundId);
    }

    public static Identifier popActiveSound(BlockPos pos) {
        return activeSoundIds.remove(pos.toImmutable());
    }

    private static float clampVolume(float v) { return Math.max(0.0f, Math.min(1.0f, v)); }
    private static float clampPitch(float p)  { return Math.max(0.5f, Math.min(2.0f, p)); }
}