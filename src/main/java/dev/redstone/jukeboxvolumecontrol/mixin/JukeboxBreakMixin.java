package dev.redstone.jukeboxvolumecontrol.mixin;

import dev.redstone.jukeboxvolumecontrol.JukeboxVolumeManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.JukeboxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JukeboxBlock.class)
public class JukeboxBreakMixin {

    @Inject(method = "onStateReplaced", at = @At("HEAD"))
    private void onJukeboxRemoved(BlockState state, ServerWorld world, BlockPos pos, boolean moved, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.getSoundManager() != null) {
            client.getSoundManager().stopSounds(null, SoundCategory.RECORDS);
            JukeboxVolumeManager.remove(pos);
        }
    }
}
