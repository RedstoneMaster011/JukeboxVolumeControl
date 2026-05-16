package dev.redstone.jukeboxvolumecontrol.mixin;

import dev.redstone.jukeboxvolumecontrol.client.JukeboxVolumeScreen;
import net.minecraft.block.JukeboxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientInteractionMixin {

    @Inject(method = "interactBlockInternal", at = @At("HEAD"), cancellable = true)
    private void onInteractBlockInternal(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult,
                                         CallbackInfoReturnable<ActionResult> cir) {
        if (!player.isSneaking()) return;
        if (hand != Hand.MAIN_HAND) return;

        var state = MinecraftClient.getInstance().world.getBlockState(hitResult.getBlockPos());
        if (!(state.getBlock() instanceof JukeboxBlock)) return;

        MinecraftClient.getInstance().setScreen(new JukeboxVolumeScreen(hitResult.getBlockPos()));
        cir.setReturnValue(ActionResult.SUCCESS);
    }
}
