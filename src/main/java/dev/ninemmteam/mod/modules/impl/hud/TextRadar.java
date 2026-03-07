package dev.ninemmteam.mod.modules.impl.hud;

import dev.ninemmteam.fentanyl;
import dev.ninemmteam.core.impl.FontManager;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.settings.impl.BooleanSetting;
import dev.ninemmteam.mod.modules.settings.impl.ColorSetting;
import dev.ninemmteam.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import dev.ninemmteam.api.events.eventbus.EventListener;
import dev.ninemmteam.api.events.impl.PacketEvent;
import dev.ninemmteam.api.events.impl.TotemEvent;
import net.minecraft.client.texture.Sprite;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.WorldEventS2CPacket;
import net.minecraft.potion.Potions;
import net.minecraft.util.math.Vec3d;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Formatting;

public class TextRadar extends Module {
   public static TextRadar INSTANCE;
   private final DecimalFormat df = new DecimalFormat("0.0");
   private final BooleanSetting font = this.add(new BooleanSetting("Font", true));
   private final BooleanSetting shadow = this.add(new BooleanSetting("Shadow", true));
   private final SliderSetting x = this.add(new SliderSetting("X", 0, 0, 1500));
   private final SliderSetting y = this.add(new SliderSetting("Y", 100, 0, 1000));
   private final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 255, 255)));
   private final ColorSetting friend = this.add(new ColorSetting("Friend").injectBoolean(true));
   private final BooleanSetting doubleBlank = this.add(new BooleanSetting("Double", false));
   private final BooleanSetting health = this.add(new BooleanSetting("Health", true));
   private final BooleanSetting pops = this.add(new BooleanSetting("Pops", true));
   public final BooleanSetting red = this.add(new BooleanSetting("Red", false));
   private final BooleanSetting distance = this.add(new BooleanSetting("Distance", true));
   private final BooleanSetting effects = this.add(new BooleanSetting("Effects", true));
   private final BooleanSetting turtle = this.add(new BooleanSetting("Turtle", true));
   
   private final Map<Integer, Long> turtlePlayers = new HashMap<>();

   public TextRadar() {
      super("TextRadar", Category.Client);
      this.setChinese("文字雷达");
      INSTANCE = this;
   }

   @EventListener
   public void onPacketReceive(PacketEvent.Receive event) {
      if (event.getPacket() instanceof WorldEventS2CPacket packet && packet.getEventId() == 2002) {
         Vec3d splashPos = Vec3d.ofCenter(packet.getPos());
         PotionEntity potionEntity = null;
         double minDst = Double.MAX_VALUE;

         for (Entity e : mc.world.getEntities()) {
            if (e instanceof PotionEntity pe) {
               double dst = e.getPos().squaredDistanceTo(splashPos);
               if (dst < 16.0 && dst < minDst) {
                  minDst = dst;
                  potionEntity = pe;
               }
            }
         }

         if (potionEntity != null) {
            ItemStack stack = potionEntity.getStack();
            PotionContentsComponent contents =
                  stack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);
            if (contents.matches(Potions.TURTLE_MASTER)
                  || contents.matches(Potions.STRONG_TURTLE_MASTER)
                  || contents.matches(Potions.LONG_TURTLE_MASTER)) {
               for (PlayerEntity player : mc.world.getPlayers()) {
                  double dist = player.squaredDistanceTo(splashPos);
                  if (dist < 16.0) {
                     int baseDuration = 400;
                     if (contents.matches(Potions.LONG_TURTLE_MASTER)) baseDuration = 800;

                     double impact = 1.0 - Math.sqrt(dist) / 4.0;
                     if (impact > 0) {
                        int duration = (int) (impact * baseDuration + 0.5);
                        if (duration > 20) {
                           turtlePlayers.put(
                                 player.getId(), System.currentTimeMillis() + duration * 50L);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @EventListener
   public void onTotemPop(TotemEvent event) {
      PlayerEntity player = event.getPlayer();
      if (player != null) {
         turtlePlayers.remove(player.getId());
      }
   }

   @Override
   public void onRender2D(DrawContext drawContext, float tickDelta) {
      int currentY = this.y.getValueInt();
      List<AbstractClientPlayerEntity> players = new ArrayList(mc.world.getPlayers());
      players.sort(Comparator.comparingDouble(playerx -> mc.player.distanceTo(playerx)));

      for (PlayerEntity player : players) {
         if (player != mc.player) {
            StringBuilder stringBuilder = new StringBuilder();
            String blank = this.doubleBlank.getValue() ? "  " : " ";
            if (this.health.getValue()) {
               stringBuilder.append(getHealthColor(player));
               stringBuilder.append(this.df.format(player.getHealth() + player.getAbsorptionAmount()));
               stringBuilder.append(blank);
            }

            stringBuilder.append(Formatting.RESET);
            stringBuilder.append(player.getName().getString());
            if (this.distance.getValue()) {
               stringBuilder.append(blank);
               stringBuilder.append(Formatting.WHITE);
               stringBuilder.append(this.df.format(mc.player.distanceTo(player)));
               stringBuilder.append("m");
            }

            if (this.effects.getValue()) {
               if (player.hasStatusEffect(StatusEffects.SLOWNESS)) {
                  stringBuilder.append(blank);
                  stringBuilder.append(Formatting.GRAY);
                  stringBuilder.append("Lv.");
                  stringBuilder.append(player.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier() + 1);
                  stringBuilder.append(blank);
                  stringBuilder.append(player.getStatusEffect(StatusEffects.SLOWNESS).getDuration() / 20 + 1);
                  stringBuilder.append("s");
               }

               if (player.hasStatusEffect(StatusEffects.SPEED)) {
                  stringBuilder.append(blank);
                  stringBuilder.append(Formatting.AQUA);
                  stringBuilder.append("Lv.");
                  stringBuilder.append(player.getStatusEffect(StatusEffects.SPEED).getAmplifier() + 1);
                  stringBuilder.append(blank);
                  stringBuilder.append(player.getStatusEffect(StatusEffects.SPEED).getDuration() / 20 + 1);
                  stringBuilder.append("s");
               }

               if (player.hasStatusEffect(StatusEffects.STRENGTH)) {
                  stringBuilder.append(blank);
                  stringBuilder.append(Formatting.DARK_RED);
                  stringBuilder.append("Lv.");
                  stringBuilder.append(player.getStatusEffect(StatusEffects.STRENGTH).getAmplifier() + 1);
                  stringBuilder.append(blank);
                  stringBuilder.append(player.getStatusEffect(StatusEffects.STRENGTH).getDuration() / 20 + 1);
                  stringBuilder.append("s");
               }

               if (player.hasStatusEffect(StatusEffects.RESISTANCE)) {
                  stringBuilder.append(blank);
                  stringBuilder.append(Formatting.BLUE);
                  stringBuilder.append("Lv.");
                  stringBuilder.append(player.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() + 1);
                  stringBuilder.append(blank);
                  stringBuilder.append(player.getStatusEffect(StatusEffects.RESISTANCE).getDuration() / 20 + 1);
                  stringBuilder.append("s");
               }
            }

            if (this.pops.getValue()) {
               int totemPopped = fentanyl.POP.getPop(player);
               if (totemPopped > 0) {
                  stringBuilder.append(blank);
                  stringBuilder.append(getPopColor(totemPopped));
                  stringBuilder.append("-");
                  stringBuilder.append(totemPopped);
               }
            }

            boolean isFriend = fentanyl.FRIEND.isFriend(player);
            if (!isFriend || this.friend.booleanValue) {
               int color = isFriend ? this.friend.getValue().getRGB() : this.color.getValue().getRGB();
               if (this.font.getValue()) {
                  FontManager.ui.drawString(drawContext.getMatrices(), stringBuilder.toString(), this.x.getValueInt(), currentY, color, this.shadow.getValue());
               } else {
                  drawContext.drawText(mc.textRenderer, stringBuilder.toString(), this.x.getValueInt(), currentY, color, this.shadow.getValue());
               }

               currentY += this.font.getValue() ? (int)FontManager.ui.getFontHeight() : 9;
            }
            
            if (this.turtle.getValue() && turtlePlayers.containsKey(player.getId())) {
               long expiry = turtlePlayers.get(player.getId());
               if (System.currentTimeMillis() < expiry) {
                  long remaining = expiry - System.currentTimeMillis();
                  int seconds = (int) (remaining / 1000);
                  String time = String.format("%d:%02d", seconds / 60, seconds % 60);
                  
                  int textWidth = this.font.getValue() ? 
                     (int)FontManager.ui.getWidth(stringBuilder.toString()) : 
                     mc.textRenderer.getWidth(stringBuilder.toString());
                  
                  int drawX = this.x.getValueInt() + textWidth + 2;
                  int drawY = currentY - (this.font.getValue() ? (int)FontManager.ui.getFontHeight() : 9);
                  
                  Sprite sprite = mc.getStatusEffectSpriteManager().getSprite(StatusEffects.RESISTANCE);
                  int iconSize = 8;
                  
                  drawContext.drawSprite(drawX, drawY, 0, iconSize, iconSize, sprite);
                  
                  if (this.font.getValue()) {
                     FontManager.ui.drawString(drawContext.getMatrices(), time, drawX + iconSize + 2, drawY, -1, this.shadow.getValue());
                  } else {
                     drawContext.drawText(mc.textRenderer, time, drawX + iconSize + 2, drawY, -1, this.shadow.getValue());
                  }
               } else {
                  turtlePlayers.remove(player.getId());
               }
            }
         }
      }
   }

   public static Formatting getHealthColor(PlayerEntity player) {
      double health = player.getHealth() + player.getAbsorptionAmount();
      if (health > 18.0) {
         return Formatting.GREEN;
      } else if (health > 16.0) {
         return Formatting.DARK_GREEN;
      } else if (health > 12.0) {
         return Formatting.YELLOW;
      } else if (health > 8.0) {
         return Formatting.GOLD;
      } else {
         return health > 4.0 ? Formatting.RED : Formatting.DARK_RED;
      }
   }

   public static Formatting getPopColor(int totems) {
      if (INSTANCE.red.getValue()) {
         return Formatting.RED;
      } else if (totems > 10) {
         return Formatting.DARK_RED;
      } else if (totems > 8) {
         return Formatting.RED;
      } else if (totems > 6) {
         return Formatting.GOLD;
      } else if (totems > 4) {
         return Formatting.YELLOW;
      } else {
         return totems > 2 ? Formatting.DARK_GREEN : Formatting.GREEN;
      }
   }
}