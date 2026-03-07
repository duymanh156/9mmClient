package dev.ninemmteam.mod.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.ninemmteam.api.events.eventbus.EventListener;
import dev.ninemmteam.api.utils.math.Timer;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.settings.impl.BooleanSetting;
import dev.ninemmteam.mod.modules.settings.impl.ColorSetting;
import dev.ninemmteam.mod.modules.settings.impl.EnumSetting;
import dev.ninemmteam.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import org.joml.Matrix4f;

public class CrystalPlaceESP extends Module {
   public static CrystalPlaceESP INSTANCE;
   private final ConcurrentHashMap<EndCrystalEntity, CrystalInfo> crystalList = new ConcurrentHashMap<>();
   private final Timer timer = new Timer();
   
   private final BooleanSetting range = this.add(new BooleanSetting("CheckRange", true).setParent());
   private final SliderSetting rangeValue = this.add(new SliderSetting("Range", 12.0, 0.0, 256.0, 1.0, () -> this.range.getValue()));
   private final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 255, 255, 150)));
   private final SliderSetting animationTime = this.add(new SliderSetting("AnimationTime", 500.0, 0.0, 1500.0, 1.0));
   private final SliderSetting fadeSpeed = this.add(new SliderSetting("FadeSpeed", 500.0, 0.0, 1500.0, 0.1));
   private final SliderSetting upSpeed = this.add(new SliderSetting("UpSpeed", 1500.0, 0.0, 3000.0, 0.1));
   private final EnumSetting<Mode> mode = this.add(new EnumSetting<>("Mode", Mode.Normal));
   private final SliderSetting points = this.add(new SliderSetting("Points", 3.0, 1.0, 10.0, 1.0, () -> this.mode.getValue() == Mode.Normal));
   private final SliderSetting interval = this.add(new SliderSetting("Interval", 2.0, 1.0, 100.0, 1.0, () -> this.mode.getValue() == Mode.New));

   public CrystalPlaceESP() {
      super("CrystalPlaceESP", Module.Category.Render);
      this.setChinese("水晶放置ESP");
      INSTANCE = this;
   }

   public static void drawCircle3D(MatrixStack stack, Entity entity, float radius, float height, float up, Color color) {
      RenderSystem.enableBlend();
      RenderSystem.disableDepthTest();
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder bufferBuilder = tessellator.begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      
      double x = entity.prevX + (entity.getX() - entity.prevX) * mc.getRenderTickCounter().getTickDelta(true) 
         - mc.getEntityRenderDispatcher().camera.getPos().getX();
      double y = entity.prevY + height + (entity.getY() - entity.prevY) * mc.getRenderTickCounter().getTickDelta(true) 
         - mc.getEntityRenderDispatcher().camera.getPos().getY();
      double z = entity.prevZ + (entity.getZ() - entity.prevZ) * mc.getRenderTickCounter().getTickDelta(true) 
         - mc.getEntityRenderDispatcher().camera.getPos().getZ();
      
      stack.push();
      stack.translate(x, y, z);
      Matrix4f matrix = stack.peek().getPositionMatrix();
      
      float r = color.getRed() / 255.0f;
      float g = color.getGreen() / 255.0f;
      float b = color.getBlue() / 255.0f;
      float a = color.getAlpha() / 255.0f;
      
      for (int i = 0; i <= 180; i++) {
         float cos = (float) (radius * Math.cos(i * 6.28 / 45.0));
         float sin = (float) (radius * Math.sin(i * 6.28 / 45.0));
         bufferBuilder.vertex(matrix, cos, up, sin).color(r, g, b, a);
      }
      
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.endNullable());
      stack.pop();
      RenderSystem.enableDepthTest();
      RenderSystem.disableBlend();
   }

   @Override
   public void onRender3D(MatrixStack matrixStack) {
      if (nullCheck()) return;
      
      for (Entity entity : mc.world.getEntities()) {
         if (!(entity instanceof EndCrystalEntity)) continue;
         if (this.range.getValue() && mc.player.distanceTo(entity) > this.rangeValue.getValue()) continue;
         if (this.crystalList.containsKey(entity)) continue;
         
         this.crystalList.put((EndCrystalEntity) entity, new CrystalInfo((EndCrystalEntity) entity, System.currentTimeMillis()));
      }
      
      if (this.mode.getValue() == Mode.Normal) {
         this.crystalList.forEach((entity, info) -> this.draw(matrixStack, info.entity, info.time, info.time));
      } else if (this.mode.getValue() == Mode.New) {
         int time = 0;
         for (int i = 0; i < this.points.getValueInt(); i++) {
            if (this.timer.passedMs(500)) {
               int finalTime = time;
               this.crystalList.forEach((entity, info) -> this.draw(matrixStack, info.entity, info.time - finalTime, info.time - finalTime));
            }
            time = (int) (time + this.interval.getValue());
         }
      }
      
      this.crystalList.forEach((entity, info) -> {
         if (System.currentTimeMillis() - info.time > this.animationTime.getValue() && !entity.isAlive()) {
            this.crystalList.remove(entity);
         }
         if (System.currentTimeMillis() - info.time > this.animationTime.getValue() && mc.player.distanceTo(entity) > this.rangeValue.getValue()) {
            this.crystalList.remove(entity);
         }
      });
   }

   private void draw(MatrixStack matrixStack, EndCrystalEntity entity, long radTime, long heightTime) {
      long rad = System.currentTimeMillis() - radTime;
      long height = System.currentTimeMillis() - heightTime;
      
      if (rad <= this.animationTime.getValue()) {
         float radius = (float) rad / this.fadeSpeed.getValueFloat();
         float h = (float) height / 1000.0f;
         float up = (float) rad / this.upSpeed.getValueFloat();
         drawCircle3D(matrixStack, entity, radius, h, up, this.color.getValue());
      }
   }

   @Override
   public void onDisable() {
      this.crystalList.clear();
   }

   public enum Mode {
      Normal,
      New
   }

   private record CrystalInfo(EndCrystalEntity entity, long time) {}
}
