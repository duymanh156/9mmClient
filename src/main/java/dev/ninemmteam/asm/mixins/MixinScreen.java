package dev.ninemmteam.asm.mixins;

import dev.ninemmteam.fentanyl;
import dev.ninemmteam.api.utils.render.Render2DUtil;
import dev.ninemmteam.mod.modules.impl.client.ClickGui;
import dev.ninemmteam.mod.modules.impl.client.ClientSetting;
import java.awt.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.gui.screen.ProgressScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class MixinScreen {
   @Shadow
   public int width;
   @Shadow
   public int height;
   @Shadow
   protected MinecraftClient client;

   @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
   public void renderInGameBackgroundHook(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      ci.cancel();
      if (this.client.world == null) {
         boolean isMainMenu = (Screen)((Object)this) instanceof TitleScreen;
         boolean isLoading = (Screen)((Object)this) instanceof ProgressScreen || (Screen)((Object)this) instanceof DownloadingTerrainScreen;
         if (isMainMenu || isLoading) {
            drawAuroraBackground(context);
            return;
         }

         this.renderPanoramaBackground(context, delta);
      }

      if (ClientSetting.INSTANCE.darkening.getValue() && !((Screen)((Object)this) instanceof TitleScreen)) {
         this.renderDarkening(context);
      }

      if (this.client.world != null && ClickGui.getInstance().tint.booleanValue) {
         context.fillGradient(
            0, 0, this.width, this.height, ClickGui.getInstance().tint.getValue().getRGB(), ClickGui.getInstance().endColor.getValue().getRGB()
         );
      }
   }
   
   private void drawAuroraBackground(DrawContext context) {
      float w = this.width;
      float h = this.height;
      MatrixStack m = context.getMatrices();
      
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

   @Shadow
   protected void renderPanoramaBackground(DrawContext context, float delta) {
   }

   @Shadow
   protected void renderDarkening(DrawContext context) {
   }

   @Shadow
   public void close() {
   }

   @Shadow
   public ScreenRect getNavigationFocus() {
      return null;
   }

   @Shadow
   protected <T extends Element & Drawable & Selectable> T addDrawableChild(T drawableElement) {
      return null;
   }
}
