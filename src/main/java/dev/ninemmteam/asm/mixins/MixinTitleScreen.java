package dev.ninemmteam.asm.mixins;

import dev.ninemmteam.fentanyl;
import dev.ninemmteam.api.utils.render.Render2DUtil;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class MixinTitleScreen extends Screen {

    private static final int BUTTON_X = 20;
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 4;

   public MixinTitleScreen() {
      super(null);
   }

   @Inject(method = "renderPanoramaBackground", at = @At("HEAD"), cancellable = true)
   public void onRenderPanoramaBackground(DrawContext context, float delta, CallbackInfo ci) {
      ci.cancel();
      float w = this.width;
      float h = this.height;
      MatrixStack m = context.getMatrices();
      drawAuroraBackground(m, w, h);
   }
    
    @Inject(method = "init", at = @At("RETURN"))
    private void onInitReturn(CallbackInfo ci) {
        int currentY = this.height / 4 + 48;
        
        for (var child : this.children()) {
            if (child instanceof ClickableWidget widget) {
                widget.setX(BUTTON_X);
                widget.setY(currentY);
                widget.setWidth(BUTTON_WIDTH);
                currentY += BUTTON_HEIGHT + BUTTON_SPACING;
            }
        }
    }
    
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRenderHead(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ci.cancel();
        
        renderPanoramaBackground(context, delta);
        
        drawCustomTitle(context);
        
        for (var child : this.children()) {
            if (child instanceof ClickableWidget widget) {
                widget.render(context, mouseX, mouseY, delta);
            }
        }
    }
    
    private void drawCustomTitle(DrawContext context) {
        context.drawTextWithShadow(this.textRenderer, fentanyl.NAME, BUTTON_X, 30, 0xFFFFFFFF);
        context.drawTextWithShadow(this.textRenderer, "v" + fentanyl.VERSION, BUTTON_X, 42, 0xFFAAAAAA);
    }
    
    private void drawAuroraBackground(MatrixStack m, float w, float h) {
        Render2DUtil.rect(m, 0, 0, w, h, new Color(5, 10, 35, 255).getRGB());
        
        long now = System.currentTimeMillis();
        float time = now / 1000.0F;
        
        for (int layer = 0; layer < 6; layer++) {
            float layerTime = time * (0.15F + layer * 0.05F);
            float baseY = h * 0.05F + layer * h * 0.08F;
            
            for (int band = 0; band < 3; band++) {
                float bandOffset = band * 0.5F;
                float hue = (time * 0.03F + layer * 0.08F + band * 0.15F) % 1.0F;
                Color bandColor = Color.getHSBColor(hue, 0.7F, 0.9F);
                
                float prevX = 0;
                float prevY = baseY + (float)Math.sin(layerTime + bandOffset) * h * 0.05F;
                
                for (int x = 0; x <= w; x += 8) {
                    float wave1 = (float)Math.sin(x * 0.01F + layerTime * 2.0F) * h * 0.04F;
                    float wave2 = (float)Math.sin(x * 0.02F + layerTime * 1.5F + bandOffset) * h * 0.02F;
                    float wave3 = (float)Math.cos(x * 0.015F + layerTime) * h * 0.03F;
                    
                    float y = baseY + wave1 + wave2 + wave3;
                    
                    int alpha = (int)(80 - layer * 10);
                    int color = new Color(bandColor.getRed(), bandColor.getGreen(), bandColor.getBlue(), alpha).getRGB();
                    
                    float thickness = 30 + (float)Math.sin(x * 0.005F + time) * 15;
                    Render2DUtil.rect(m, prevX, prevY, x + 8, y + thickness, color);
                    
                    prevX = x;
                    prevY = y;
                }
            }
        }
        
        drawStars(m, w, h, time);
    }
    
    private void drawStars(MatrixStack m, float w, float h, float time) {
        java.util.Random random = new java.util.Random(12345L);
        
        for (int i = 0; i < 150; i++) {
            float x = random.nextFloat() * w;
            float y = random.nextFloat() * h * 0.6F;
            float size = random.nextFloat() * 2.5F + 0.5F;
            float twinkle = (float)Math.sin(time * 3.0F + i * 0.5F) * 0.4F + 0.6F;
            
            int alpha = (int)(twinkle * 255);
            int color = new Color(255, 255, 255, alpha).getRGB();
            
            Render2DUtil.rect(m, x, y, x + size, y + size, color);
        }
    }
}
