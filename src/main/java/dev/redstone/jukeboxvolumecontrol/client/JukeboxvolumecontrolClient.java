package dev.redstone.jukeboxvolumecontrol.client;

import dev.redstone.jukeboxvolumecontrol.client.JukeboxColorRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;

public class JukeboxvolumecontrolClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        BlockEntityRendererFactories.register(
                BlockEntityType.JUKEBOX,
                JukeboxColorRenderer::new
        );
    }
}
