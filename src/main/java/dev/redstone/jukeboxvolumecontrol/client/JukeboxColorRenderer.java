package dev.redstone.jukeboxvolumecontrol.client;

import dev.redstone.jukeboxvolumecontrol.JukeboxVolumeManager;
import net.minecraft.block.entity.JukeboxBlockEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public class JukeboxColorRenderer
        implements BlockEntityRenderer<JukeboxBlockEntity, JukeboxColorRenderer.ColorState> {

    private static final RenderLayer TINT_LAYER = RenderLayers.debugFilledBox();

    public JukeboxColorRenderer(BlockEntityRendererFactory.Context ctx) {}

    public static class ColorState extends BlockEntityRenderState {
        public int rgb = 0xFFFFFF;
    }

    @Override
    public ColorState createRenderState() {
        return new ColorState();
    }

    @Override
    public void updateRenderState(JukeboxBlockEntity entity, ColorState state, float tickProgress,
                                  Vec3d cameraPos,
                                  ModelCommandRenderer.@Nullable CrumblingOverlayCommand crumblingOverlay) {
        BlockEntityRenderState.updateBlockEntityRenderState(entity, state, crumblingOverlay);
        state.rgb = JukeboxVolumeManager.getColor(entity.getPos());
    }

    @Override
    public void render(ColorState state, MatrixStack matrices,
                       OrderedRenderCommandQueue queue, CameraRenderState cameraState) {

        if (state.rgb == 0xFFFFFF) return;

        final float r = ((state.rgb >> 16) & 0xFF) / 255f;
        final float g = ((state.rgb >>  8) & 0xFF) / 255f;
        final float b = ( state.rgb        & 0xFF) / 255f;
        final float a = 0.45f;

        queue.submitCustom(matrices, TINT_LAYER, (entry, vc) -> {
            Matrix4f pose = entry.getPositionMatrix();
            quad(vc, pose, r, g, b, a,  0,0,0,  1,0,0,  1,0,1,  0,0,1);
            quad(vc, pose, r, g, b, a,  0,1,1,  1,1,1,  1,1,0,  0,1,0);
            quad(vc, pose, r, g, b, a,  1,0,0,  0,0,0,  0,1,0,  1,1,0);
            quad(vc, pose, r, g, b, a,  0,0,1,  1,0,1,  1,1,1,  0,1,1);
            quad(vc, pose, r, g, b, a,  0,0,0,  0,0,1,  0,1,1,  0,1,0);
            quad(vc, pose, r, g, b, a,  1,0,1,  1,0,0,  1,1,0,  1,1,1);
        });
    }

    private static void quad(VertexConsumer vc, Matrix4f pose,
                             float r, float g, float b, float a,
                             float x0, float y0, float z0,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3) {
        vtx(vc, pose, x0, y0, z0, r, g, b, a);
        vtx(vc, pose, x1, y1, z1, r, g, b, a);
        vtx(vc, pose, x2, y2, z2, r, g, b, a);
        vtx(vc, pose, x3, y3, z3, r, g, b, a);
    }

    private static void vtx(VertexConsumer vc, Matrix4f pose,
                            float x, float y, float z,
                            float r, float g, float b, float a) {
        vc.vertex(pose, x, y, z).color(r, g, b, a);
    }
}
