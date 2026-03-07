package dev.ninemmteam.mod.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.ninemmteam.api.events.eventbus.EventListener;
import dev.ninemmteam.api.events.impl.ClientTickEvent;
import dev.ninemmteam.api.events.impl.DeathEvent;
import dev.ninemmteam.api.events.impl.Render3DEvent;
import dev.ninemmteam.api.utils.math.MathUtil;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.settings.impl.BooleanSetting;
import dev.ninemmteam.mod.modules.settings.impl.EnumSetting;
import dev.ninemmteam.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class KillEffect extends Module {
   public enum KillEffectMode {
      DEFAULT, HereHasALowiqDie
   }

   private final EnumSetting<KillEffectMode> mode = this.add(new EnumSetting<>("Mode", KillEffectMode.DEFAULT));
   
   // Default mode settings
   private final BooleanSetting lightning = this.add(new BooleanSetting("Lightning", true, () -> mode.getValue() == KillEffectMode.DEFAULT));
   private final BooleanSetting levelUp = this.add(new BooleanSetting("LevelUp", true, () -> mode.getValue() == KillEffectMode.DEFAULT).setParent());
   private final SliderSetting lMaxPitch = this.add(new SliderSetting("LMaxPitch", 1.0, 0.0, 2.0, 0.1, () -> mode.getValue() == KillEffectMode.DEFAULT && this.levelUp.isOpen()));
   private final SliderSetting lMinPitch = this.add(new SliderSetting("LMinPitch", 1.0, 0.0, 2.0, 0.1, () -> mode.getValue() == KillEffectMode.DEFAULT && this.levelUp.isOpen()));
   private final BooleanSetting trident = this.add(new BooleanSetting("Trident", false, () -> mode.getValue() == KillEffectMode.DEFAULT).setParent());
   private final SliderSetting tMaxPitch = this.add(new SliderSetting("TMaxPitch", 1.0, 0.0, 2.0, 0.1, () -> mode.getValue() == KillEffectMode.DEFAULT && this.trident.isOpen()));
   private final SliderSetting tMinPitch = this.add(new SliderSetting("TMinPitch", 1.0, 0.0, 2.0, 0.1, () -> mode.getValue() == KillEffectMode.DEFAULT && this.trident.isOpen()));
   private final SliderSetting factor = this.add(new SliderSetting("Factor", 1.0, 1.0, 10.0, 1.0, () -> mode.getValue() == KillEffectMode.DEFAULT));
   
   // Kill mode settings
   private final SliderSetting duration = this.add(new SliderSetting("Duration", 3, 1, 10, 0.1, () -> mode.getValue() == KillEffectMode.HereHasALowiqDie));
   private final SliderSetting scale = this.add(new SliderSetting("Scale", 1, 0.1, 3, 0.1, () -> mode.getValue() == KillEffectMode.HereHasALowiqDie));
   private final BooleanSetting flash = this.add(new BooleanSetting("Flash", true, () -> mode.getValue() == KillEffectMode.HereHasALowiqDie));
   
   private final List<DeathEffect> deathEffects = new ArrayList<>();

   public KillEffect() {
      super("KillEffect", Module.Category.Render);
      this.setChinese("击杀效果");
   }

   @EventListener
   public void onPlayerDeath(DeathEvent event) {
      if (!nullCheck()) {
         PlayerEntity player = event.getPlayer();
         if (player != null) {
            if (mode.getValue() == KillEffectMode.DEFAULT) {
               for (int i = 0; i < this.factor.getValue(); i++) {
                  this.doEffect(player);
               }
            } else if (mode.getValue() == KillEffectMode.HereHasALowiqDie) {
               boolean exists = false;
               for (DeathEffect effect : deathEffects) {
                  if (Math.abs(effect.x - player.getX()) < 0.5 && 
                      Math.abs(effect.y - player.getY()) < 0.5 && 
                      Math.abs(effect.z - player.getZ()) < 0.5) {
                     exists = true;
                     break;
                  }
               }
               if (!exists) {
                  deathEffects.add(new DeathEffect(player.getX(), player.getY() + 1, player.getZ(), System.currentTimeMillis()));
               }
            }
         }
      }
   }

   @EventListener
   public void onTick(ClientTickEvent event) {
      if (mc.world != null && mode.getValue() == KillEffectMode.HereHasALowiqDie) {
         // 清理过期的效果
         deathEffects.removeIf(effect -> System.currentTimeMillis() - effect.timestamp > duration.getValue() * 1000);
      }
   }

   @EventListener
   public void onRender3D(Render3DEvent event) {
      if (mc.world != null && mode.getValue() == KillEffectMode.HereHasALowiqDie) {
         for (DeathEffect effect : deathEffects) {
            if (flash.getValue()) {
               // 计算闪烁效果（每200ms切换一次是否渲染）
               if (((System.currentTimeMillis() / 200) % 2) == 0) {
                  drawEffect(event.matrixStack, effect.x, effect.y, effect.z, 1f);
               }
            } else {
               drawEffect(event.matrixStack, effect.x, effect.y, effect.z, 1f);
            }
         }
      }
   }

   private void doEffect(PlayerEntity player) {
      double x = player.getX();
      double y = player.getY();
      double z = player.getZ();
      if (this.lightning.getValue()) {
         LightningEntity lightningEntity = new LightningEntity(EntityType.LIGHTNING_BOLT, mc.world);
         lightningEntity.updatePosition(x, y, z);
         lightningEntity.refreshPositionAfterTeleport(x, y, z);
         mc.world.addEntity(lightningEntity);
      }

      if (this.levelUp.getValue()) {
         mc.world
            .playSound(
               mc.player,
               x,
               y,
               z,
               SoundEvents.ENTITY_PLAYER_LEVELUP,
               SoundCategory.PLAYERS,
               100.0F,
               MathUtil.random(this.lMinPitch.getValueFloat(), this.lMaxPitch.getValueFloat())
            );
      }

      if (this.trident.getValue()) {
         mc.world
            .playSound(
               mc.player,
               x,
               y,
               z,
               SoundEvents.ITEM_TRIDENT_THUNDER,
               SoundCategory.MASTER,
               999.0F,
               MathUtil.random(this.tMinPitch.getValueFloat(), this.tMaxPitch.getValueFloat())
            );
      }
   }

   private void drawEffect(MatrixStack matrixStack, double x, double y, double z, float alpha) {
      matrixStack.push();

      double cameraX = mc.gameRenderer.getCamera().getPos().x;
      double cameraY = mc.gameRenderer.getCamera().getPos().y;
      double cameraZ = mc.gameRenderer.getCamera().getPos().z;

      matrixStack.translate(x - cameraX, y - cameraY, z - cameraZ);
      matrixStack.multiply(mc.gameRenderer.getCamera().getRotation());

      double scaleValue = 0.0245 * scale.getValue();
      matrixStack.scale((float) scaleValue, (float) scaleValue, (float) scaleValue);

      // 渲染yuanquan.png
      Identifier OMGTexture = Identifier.of("fentanyl", "textures/killeffect/omg.png");
      RenderSystem.setShaderTexture(0, OMGTexture);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      RenderSystem.disableDepthTest();
      RenderSystem.setShaderColor(1f, 1f, 1f, alpha);

      drawTexture(matrixStack, -32, -32, 64, 64);

      // 渲染jiantou.png
      Identifier FKUTexture = Identifier.of("fentanyl", "textures/killeffect/fku.png");
      RenderSystem.setShaderTexture(0, FKUTexture);
      drawTexture(matrixStack, -5, -5, 64, 64);

      RenderSystem.enableCull();
      RenderSystem.enableDepthTest();

      matrixStack.pop();
   }

   private void drawTexture(MatrixStack matrices, float x, float y, float width, float height) {
      RenderSystem.setShader(GameRenderer::getPositionTexProgram);
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
      Matrix4f matrix = matrices.peek().getPositionMatrix();

      bufferBuilder.vertex(matrix, x, y + height, 0).texture(0, 0);
      bufferBuilder.vertex(matrix, x + width, y + height, 0).texture(1, 0);
      bufferBuilder.vertex(matrix, x + width, y, 0).texture(1, 1);
      bufferBuilder.vertex(matrix, x, y, 0).texture(0, 1);

      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
   }

   private static class DeathEffect {
      public final double x;
      public final double y;
      public final double z;
      public final long timestamp;

      public DeathEffect(double x, double y, double z, long timestamp) {
         this.x = x;
         this.y = y;
         this.z = z;
         this.timestamp = timestamp;
      }
   }
}
