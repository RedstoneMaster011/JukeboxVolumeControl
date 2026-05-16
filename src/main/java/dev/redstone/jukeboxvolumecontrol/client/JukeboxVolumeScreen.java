package dev.redstone.jukeboxvolumecontrol.client;

import dev.redstone.jukeboxvolumecontrol.JukeboxVolumeManager;
import dev.redstone.jukeboxvolumecontrol.network.JukeboxSettingsPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
public final class JukeboxVolumeScreen extends Screen {

    private static final int BG            = 0xFF1A2535;
    private static final int BORDER        = 0xFF2A3A50;
    private static final int TEXT_PRIMARY  = 0xFFE8EEF4;
    private static final int TEXT_MUTED    = 0xFF7A9AB8;
    private static final int INPUT_BG      = 0xFF0F1E2E;
    private static final int INPUT_BORDER  = 0xFF2A4A6A;
    private static final int CHECKERBOARD1 = 0xFF888888;
    private static final int CHECKERBOARD2 = 0xFF555555;
    private static final int ACCENT        = 0xFF4A90D9;

    private static final int POP_W = 330;
    private static final int POP_H = 285;

    private static final int SV_X_OFF = 10;
    private static final int SV_Y_OFF = 22;
    private static final int SV_W     = 170;
    private static final int SV_H     = 120;

    private static final int HUE_W     = 12;
    private static final int HUE_X_OFF = SV_X_OFF + SV_W + 6;

    private static final int VOL_W     = 18;
    private static final int VOL_X_OFF = HUE_X_OFF + HUE_W + 8;

    private static final int FIELDS_Y_OFF = SV_Y_OFF + SV_H + 10;
    private static final int SWATCH       = 18;
    private static final int FIELD_H      = 12;
    private static final int FIELD_W_HEX  = 64;
    private static final int FIELD_W_RGB  = 32;

    private static final int PITCH_Y_OFF  = FIELDS_Y_OFF + FIELD_H + 6 + FIELD_H + 18;
    private static final int PITCH_H      = 5;

    private static final int BTN_Y_OFF = POP_H - 26;

    private final BlockPos jukeboxPos;
    private final Screen   parent;

    // Snapshot of settings when the screen was opened, for cancel rollback
    private final float originalVolume;
    private final float originalPitch;
    private final int   originalColor;

    private int   r, g, b;
    private float hue, sat, val;
    private float volume;
    private float pitch;

    private TextFieldWidget hexField;
    private TextFieldWidget rField, gField, bField;
    private boolean syncing = false;

    private boolean draggingSV     = false;
    private boolean draggingHue    = false;
    private boolean draggingVolume = false;
    private boolean draggingPitch  = false;

    private int px, py;

    public JukeboxVolumeScreen(BlockPos jukeboxPos) {
        super(Text.literal("Jukebox Settings"));
        this.jukeboxPos = jukeboxPos;
        this.parent     = MinecraftClient.getInstance().currentScreen;

        JukeboxVolumeManager.JukeboxSettings s = JukeboxVolumeManager.getSettings(jukeboxPos);
        this.volume = s.volume();
        this.pitch  = s.pitch();

        int rgb = s.color();
        this.r = (rgb >> 16) & 0xFF;
        this.g = (rgb >>  8) & 0xFF;
        this.b =  rgb        & 0xFF;
        float[] hsv = rgbToHsv(r, g, b);
        this.hue = hsv[0];
        this.sat = hsv[1];
        this.val = hsv[2];

        // Save originals for cancel
        this.originalVolume = this.volume;
        this.originalPitch  = this.pitch;
        this.originalColor  = rgb;
    }

    /** Send current settings to the server, which broadcasts to all clients. */
    private void sendUpdate() {
        ClientPlayNetworking.send(new JukeboxSettingsPayload(
                jukeboxPos, volume, pitch, (r << 16) | (g << 8) | b));
    }

    @Override
    protected void init() {
        px = (width  - POP_W) / 2;
        py = (height - POP_H) / 2;

        int fieldsY = py + FIELDS_Y_OFF;

        int hexX = px + SV_X_OFF + SWATCH + 14;
        hexField = new TextFieldWidget(textRenderer, hexX, fieldsY, FIELD_W_HEX, FIELD_H,
                Text.literal("Hex"));
        hexField.setMaxLength(7);
        hexField.setDrawsBackground(false);
        hexField.setText(toHexNoHash(r, g, b));
        hexField.setChangedListener(v -> {
            if (syncing) return;
            String s = v.startsWith("#") ? v.substring(1) : v;
            if (s.length() == 6) {
                try {
                    int rgb = (int) Long.parseLong(s, 16);
                    r = (rgb >> 16) & 0xFF;
                    g = (rgb >>  8) & 0xFF;
                    b =  rgb        & 0xFF;
                    float[] hsv = rgbToHsv(r, g, b);
                    hue = hsv[0]; sat = hsv[1]; val = hsv[2];
                    syncFromRgb(false);
                    sendUpdate();
                } catch (NumberFormatException ignored) {}
            }
        });
        addDrawableChild(hexField);

        int rgbY = fieldsY + FIELD_H + 6;
        int rX   = px + SV_X_OFF + 12;
        int gX   = rX + FIELD_W_RGB + 22;
        int bX   = gX + FIELD_W_RGB + 22;

        rField = makeRgbField(rX, rgbY, r, v -> { r = v; recalcHsv(); syncFromRgb(true); sendUpdate(); });
        gField = makeRgbField(gX, rgbY, g, v -> { g = v; recalcHsv(); syncFromRgb(true); sendUpdate(); });
        bField = makeRgbField(bX, rgbY, b, v -> { b = v; recalcHsv(); syncFromRgb(true); sendUpdate(); });
        addDrawableChild(rField);
        addDrawableChild(gField);
        addDrawableChild(bField);

        int btnY = py + BTN_Y_OFF;
        addDrawableChild(ButtonWidget.builder(Text.literal("Confirm"), btn -> confirm())
                .dimensions(px + POP_W / 2 - 82, btnY, 76, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), btn -> cancel())
                .dimensions(px + POP_W / 2 + 6,  btnY, 76, 20).build());
    }

    private TextFieldWidget makeRgbField(int x, int y, int initial,
                                         java.util.function.IntConsumer onChange) {
        TextFieldWidget tw = new TextFieldWidget(textRenderer, x, y, FIELD_W_RGB, FIELD_H,
                Text.literal(""));
        tw.setMaxLength(3);
        tw.setDrawsBackground(false);
        tw.setText(String.valueOf(initial));
        tw.setChangedListener(v -> {
            if (syncing) return;
            try { onChange.accept(Math.max(0, Math.min(255, Integer.parseInt(v)))); }
            catch (NumberFormatException ignored) {}
        });
        return tw;
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fillGradient(0, 0, width, height, 0x66000000, 0x66000000);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        drawModal(ctx);

        ctx.drawTextWithShadow(textRenderer, Text.literal("Jukebox Settings"),
                px + 10, py + 8, TEXT_PRIMARY);

        drawColorPicker(ctx);
        drawVolumeBar(ctx, mouseX, mouseY);
        drawPitchSlider(ctx, mouseX, mouseY);

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawColorPicker(DrawContext ctx) {
        int svX = px + SV_X_OFF;
        int svY = py + SV_Y_OFF;

        for (int col = 0; col < SV_W; col++) {
            float s = (float) col / (SV_W - 1);
            ctx.fillGradient(svX + col, svY, svX + col + 1, svY + SV_H,
                    hsvToArgb(hue, s, 1.0f), hsvToArgb(hue, s, 0.0f));
        }
        drawBorder(ctx, svX - 1, svY - 1, SV_W + 2, SV_H + 2);

        int cx = svX + Math.round(sat * (SV_W - 1));
        int cy = svY + Math.round((1f - val) * (SV_H - 1));
        ctx.fillGradient(cx - 4, cy - 1, cx + 4, cy + 1, 0xFFFFFFFF, 0xFFFFFFFF);
        ctx.fillGradient(cx - 1, cy - 4, cx + 1, cy + 4, 0xFFFFFFFF, 0xFFFFFFFF);

        int hueX = px + HUE_X_OFF;
        for (int row = 0; row < SV_H; row++) {
            float h = (float) row / (SV_H - 1) * 360f;
            int c = hsvToArgb(h, 1f, 1f);
            ctx.fillGradient(hueX, svY + row, hueX + HUE_W, svY + row + 1, c, c);
        }
        drawBorder(ctx, hueX - 1, svY - 1, HUE_W + 2, SV_H + 2);

        int hueCY = svY + Math.round(hue / 360f * (SV_H - 1));
        ctx.fillGradient(hueX - 2, hueCY - 1, hueX + HUE_W + 2, hueCY + 1, 0xFFFFFFFF, 0xFFFFFFFF);

        int fieldsY = py + FIELDS_Y_OFF;

        int swX = px + SV_X_OFF;
        for (int ty = 0; ty < SWATCH; ty += 4)
            for (int tx = 0; tx < SWATCH; tx += 4) {
                boolean odd = ((tx / 4 + ty / 4) % 2 == 1);
                int col = odd ? CHECKERBOARD1 : CHECKERBOARD2;
                ctx.fillGradient(swX + tx, fieldsY + ty, swX + tx + 4, fieldsY + ty + 4, col, col);
            }
        int argb = 0xFF000000 | (r << 16) | (g << 8) | b;
        ctx.fillGradient(swX, fieldsY, swX + SWATCH, fieldsY + SWATCH, argb, argb);

        int hexLabelX = swX + SWATCH + 4;
        ctx.drawTextWithShadow(textRenderer, Text.literal("#"), hexLabelX, fieldsY + 2, TEXT_MUTED);

        int rgbY = fieldsY + FIELD_H + 6;
        ctx.drawTextWithShadow(textRenderer, Text.literal("R"), rField.getX() - 8, rgbY + 2, 0xFFFF6B6B);
        ctx.drawTextWithShadow(textRenderer, Text.literal("G"), gField.getX() - 8, rgbY + 2, 0xFF6BFF6B);
        ctx.drawTextWithShadow(textRenderer, Text.literal("B"), bField.getX() - 8, rgbY + 2, 0xFF6B9FFF);

        ctx.fill(rField.getX() - 1, rgbY - 1, rField.getX() + FIELD_W_RGB + 1, rgbY + FIELD_H + 1, INPUT_BORDER);
        ctx.fill(rField.getX(), rgbY, rField.getX() + FIELD_W_RGB, rgbY + FIELD_H, INPUT_BG);
        ctx.fill(gField.getX() - 1, rgbY - 1, gField.getX() + FIELD_W_RGB + 1, rgbY + FIELD_H + 1, INPUT_BORDER);
        ctx.fill(gField.getX(), rgbY, gField.getX() + FIELD_W_RGB, rgbY + FIELD_H, INPUT_BG);
        ctx.fill(bField.getX() - 1, rgbY - 1, bField.getX() + FIELD_W_RGB + 1, rgbY + FIELD_H + 1, INPUT_BORDER);
        ctx.fill(bField.getX(), rgbY, bField.getX() + FIELD_W_RGB, rgbY + FIELD_H, INPUT_BG);

        int hexFieldX = hexField.getX();
        ctx.fill(hexFieldX - 1, fieldsY - 1, hexFieldX + FIELD_W_HEX + 1, fieldsY + FIELD_H + 1, INPUT_BORDER);
        ctx.fill(hexFieldX, fieldsY, hexFieldX + FIELD_W_HEX, fieldsY + FIELD_H, INPUT_BG);
    }

    private void drawVolumeBar(DrawContext ctx, int mouseX, int mouseY) {
        int volX = px + VOL_X_OFF;
        int svY  = py + SV_Y_OFF;

        int filled = Math.round(volume * SV_H);
        ctx.fillGradient(volX, svY, volX + VOL_W, svY + SV_H, INPUT_BG, INPUT_BG);
        if (filled > 0) {
            ctx.fillGradient(volX, svY + (SV_H - filled), volX + VOL_W, svY + SV_H, ACCENT, adjustBrightness(ACCENT, 0.7f));
        }
        drawBorder(ctx, volX - 1, svY - 1, VOL_W + 2, SV_H + 2);

        int volLabelY = svY + SV_H + 3;
        ctx.drawTextWithShadow(textRenderer, Text.literal("VOL"), volX, volLabelY, TEXT_MUTED);

        int pct = Math.round(volume * 100);
        String pctStr = pct + "%";
        int pctW = textRenderer.getWidth(pctStr);
        ctx.drawTextWithShadow(textRenderer, Text.literal(pctStr),
                volX + (VOL_W - pctW) / 2, volLabelY + 10, TEXT_PRIMARY);
    }

    private void drawPitchSlider(DrawContext ctx, int mouseX, int mouseY) {
        int pitchX = px + SV_X_OFF;
        int pitchY = py + PITCH_Y_OFF;
        int pitchW = VOL_X_OFF + VOL_W;

        ctx.fillGradient(pitchX, pitchY, pitchX + pitchW, pitchY + PITCH_H, INPUT_BG, INPUT_BG);
        drawBorder(ctx, pitchX - 1, pitchY - 1, pitchW + 2, PITCH_H + 2);

        float t = (pitch - 0.5f) / 1.5f;
        int handleX = pitchX + Math.round(t * (pitchW - 1));
        ctx.fillGradient(handleX - 3, pitchY - 3, handleX + 3, pitchY + PITCH_H + 3, ACCENT, ACCENT);

        int labelY = pitchY - 14;
        ctx.drawTextWithShadow(textRenderer, Text.literal("PITCH"), pitchX, labelY, TEXT_MUTED);
        String pitchStr = String.format("%.2fx", pitch);
        int pitchStrW = textRenderer.getWidth(pitchStr);
        ctx.drawTextWithShadow(textRenderer, Text.literal(pitchStr),
                pitchX + pitchW - pitchStrW, labelY, TEXT_PRIMARY);
    }

    private void drawModal(DrawContext ctx) {
        ctx.fill(px, py, px + POP_W, py + POP_H, BORDER);
        ctx.fill(px + 1, py + 1, px + POP_W - 1, py + POP_H - 1, BG);
    }

    private void drawBorder(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + 1, BORDER);
        ctx.fill(x, y + h - 1, x + w, y + h, BORDER);
        ctx.fill(x, y, x + 1, y + h, BORDER);
        ctx.fill(x + w - 1, y, x + w, y + h, BORDER);
    }

    @Override
    public boolean mouseClicked(Click click, boolean primary) {
        double mx = click.x(), my = click.y();

        int svX    = px + SV_X_OFF, svY = py + SV_Y_OFF;
        int hueX   = px + HUE_X_OFF;
        int volX   = px + VOL_X_OFF;
        int pitchX = px + SV_X_OFF;
        int pitchY = py + PITCH_Y_OFF;
        int pitchW = VOL_X_OFF + VOL_W;

        if (inRect(mx, my, svX, svY, SV_W, SV_H)) {
            draggingSV = true;
            applySVPick(clamp(mx, svX, svX + SV_W), clamp(my, svY, svY + SV_H));
            return true;
        }
        if (inRect(mx, my, hueX, svY, HUE_W, SV_H)) {
            draggingHue = true;
            applyHuePick(clamp(my, svY, svY + SV_H));
            return true;
        }
        if (inRect(mx, my, volX, svY, VOL_W, SV_H)) {
            draggingVolume = true;
            applyVolumePick(clamp(my, svY, svY + SV_H));
            return true;
        }
        if (inRect(mx, my, pitchX, pitchY - 4, pitchW, PITCH_H + 8)) {
            draggingPitch = true;
            applyPitchPick(clamp(mx, pitchX, pitchX + pitchW));
            return true;
        }

        return super.mouseClicked(click, primary);
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        int svX    = px + SV_X_OFF, svY = py + SV_Y_OFF;
        int hueX   = px + HUE_X_OFF;
        int volX   = px + VOL_X_OFF;
        int pitchX = px + SV_X_OFF;
        int pitchW = VOL_X_OFF + VOL_W;

        if (draggingSV) {
            applySVPick(clamp(click.x(), svX, svX + SV_W), clamp(click.y(), svY, svY + SV_H));
            return true;
        }
        if (draggingHue) {
            applyHuePick(clamp(click.y(), svY, svY + SV_H));
            return true;
        }
        if (draggingVolume) {
            applyVolumePick(clamp(click.y(), svY, svY + SV_H));
            return true;
        }
        if (draggingPitch) {
            applyPitchPick(clamp(click.x(), pitchX, pitchX + pitchW));
            return true;
        }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (draggingSV || draggingHue || draggingVolume || draggingPitch) {
            draggingSV = draggingHue = draggingVolume = draggingPitch = false;
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hScroll, double vScroll) {
        int volX = px + VOL_X_OFF;
        int svY  = py + SV_Y_OFF;
        if (inRect(mx, my, volX, svY, VOL_W, SV_H)) {
            volume = clampF(volume + (float) vScroll * 0.05f, 0f, 1f);
            sendUpdate();
            return true;
        }
        return super.mouseScrolled(mx, my, hScroll, vScroll);
    }

    private void applySVPick(double mx, double my) {
        int svX = px + SV_X_OFF, svY = py + SV_Y_OFF;
        sat = (float) ((mx - svX) / (SV_W - 1));
        val = (float) (1.0 - (my - svY) / (SV_H - 1));
        sat = clampF(sat, 0f, 1f);
        val = clampF(val, 0f, 1f);
        updateRgbFromHsv();
        syncFromRgb(true);
        sendUpdate();
    }

    private void applyHuePick(double my) {
        int svY = py + SV_Y_OFF;
        hue = (float) ((my - svY) / (SV_H - 1) * 360f);
        hue = clampF(hue, 0f, 360f);
        updateRgbFromHsv();
        syncFromRgb(true);
        sendUpdate();
    }

    private void applyVolumePick(double my) {
        int svY = py + SV_Y_OFF;
        volume = 1f - (float) ((my - svY) / (SV_H - 1));
        volume = clampF(volume, 0f, 1f);
        sendUpdate();
    }

    private void applyPitchPick(double mx) {
        int pitchX = px + SV_X_OFF;
        int pitchW = VOL_X_OFF + VOL_W;
        float t = (float) ((mx - pitchX) / (pitchW - 1));
        t = clampF(t, 0f, 1f);
        pitch = 0.5f + t * 1.5f;
        sendUpdate();
    }

    private void recalcHsv() {
        float[] hsv = rgbToHsv(r, g, b);
        hue = hsv[0]; sat = hsv[1]; val = hsv[2];
    }

    private void updateRgbFromHsv() {
        int argb = hsvToArgb(hue, sat, val);
        r = (argb >> 16) & 0xFF;
        g = (argb >>  8) & 0xFF;
        b =  argb        & 0xFF;
    }

    private void syncFromRgb(boolean syncHex) {
        syncing = true;
        if (syncHex) hexField.setText(toHexNoHash(r, g, b));
        rField.setText(String.valueOf(r));
        gField.setText(String.valueOf(g));
        bField.setText(String.valueOf(b));
        syncing = false;
    }

    private void confirm() {
        // Settings already live on the server from real-time updates, just close
        close();
    }

    private void cancel() {
        // Roll back to the original settings by sending them back to the server
        ClientPlayNetworking.send(new JukeboxSettingsPayload(jukeboxPos, originalVolume, originalPitch, originalColor));
        close();
    }

    @Override public boolean shouldPause()      { return false; }
    @Override public boolean shouldCloseOnEsc() { return true; }
    @Override public void close()               { if (client != null) client.setScreen(parent); }

    private static boolean inRect(double x, double y, int rx, int ry, int rw, int rh) {
        return x >= rx && x <= rx + rw && y >= ry && y <= ry + rh;
    }

    private static double clamp(double v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static float clampF(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static int adjustBrightness(int argb, float factor) {
        int a = (argb >> 24) & 0xFF;
        int r = Math.round(((argb >> 16) & 0xFF) * factor);
        int g = Math.round(((argb >>  8) & 0xFF) * factor);
        int b = Math.round(( argb        & 0xFF) * factor);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static String toHexNoHash(int r, int g, int b) {
        return String.format("%02X%02X%02X", r, g, b);
    }

    private static float[] rgbToHsv(int r, int g, int b) {
        float rf = r / 255f, gf = g / 255f, bf = b / 255f;
        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float delta = max - min;
        float h = 0f, s = (max == 0f) ? 0f : delta / max, v = max;
        if (delta != 0f) {
            if      (max == rf) h = ((gf - bf) / delta) % 6f;
            else if (max == gf) h = (bf - rf) / delta + 2f;
            else                h = (rf - gf) / delta + 4f;
            h *= 60f;
            if (h < 0) h += 360f;
        }
        return new float[]{h, s, v};
    }

    private static int hsvToArgb(float h, float s, float v) {
        float c  = v * s;
        float x  = c * (1f - Math.abs((h / 60f) % 2f - 1f));
        float m  = v - c;
        float rp, gp, bp;
        if      (h < 60f)  { rp = c; gp = x; bp = 0; }
        else if (h < 120f) { rp = x; gp = c; bp = 0; }
        else if (h < 180f) { rp = 0; gp = c; bp = x; }
        else if (h < 240f) { rp = 0; gp = x; bp = c; }
        else if (h < 300f) { rp = x; gp = 0; bp = c; }
        else               { rp = c; gp = 0; bp = x; }
        return 0xFF000000
                | (Math.round((rp + m) * 255) << 16)
                | (Math.round((gp + m) * 255) <<  8)
                |  Math.round((bp + m) * 255);
    }
}