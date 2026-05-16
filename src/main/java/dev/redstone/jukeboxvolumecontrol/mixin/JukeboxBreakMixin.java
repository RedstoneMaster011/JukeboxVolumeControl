package dev.redstone.jukeboxvolumecontrol.mixin;

import dev.redstone.jukeboxvolumecontrol.JukeboxVolumeManager;
import dev.redstone.jukeboxvolumecontrol.network.JukeboxRemovePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.JukeboxBlock;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JukeboxBlock.class)
public class JukeboxBreakMixin {

    @Inject(method = "onStateReplaced", at = @At("HEAD"))
    private void onJukeboxRemoved(BlockState state, ServerWorld world, BlockPos pos, boolean moved, CallbackInfo ci) {
        JukeboxVolumeManager.remove(pos);

        JukeboxRemovePayload payload = new JukeboxRemovePayload(pos.toImmutable());
        for (ServerPlayerEntity player : world.getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}