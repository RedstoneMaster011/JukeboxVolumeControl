package dev.redstone.jukeboxvolumecontrol.mixin;

import dev.redstone.jukeboxvolumecontrol.network.JukeboxStopPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.JukeboxBlock;
import net.minecraft.block.entity.JukeboxBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(JukeboxBlock.class)
public class JukeboxMixin {

    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private void onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
                       BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        if (player.isSneaking()) {
            cir.setReturnValue(ActionResult.SUCCESS);
            return;
        }

        if (world instanceof ServerWorld serverWorld) {
            if (world.getBlockEntity(pos) instanceof JukeboxBlockEntity jukebox) {
                if (jukebox.getManager().isPlaying()) {
                    JukeboxStopPayload stopPayload = new JukeboxStopPayload(pos.toImmutable());
                    for (ServerPlayerEntity p : serverWorld.getServer().getPlayerManager().getPlayerList()) {
                        ServerPlayNetworking.send(p, stopPayload);
                    }
                }
            }
        }
    }
}