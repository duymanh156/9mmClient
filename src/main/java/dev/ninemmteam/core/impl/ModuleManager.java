package dev.ninemmteam.core.impl;


import dev.ninemmteam.fentanyl;
import dev.ninemmteam.api.events.impl.Render2DEvent;
import dev.ninemmteam.api.events.impl.Render3DEvent;
import dev.ninemmteam.api.utils.Wrapper;
import dev.ninemmteam.api.utils.render.Render2DUtil;
import dev.ninemmteam.api.utils.render.TextUtil;
import dev.ninemmteam.mod.Mod;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.impl.client.*;
import dev.ninemmteam.mod.modules.impl.combat.*;
import dev.ninemmteam.mod.modules.impl.exploit.*;
import dev.ninemmteam.mod.modules.impl.misc.ArmorPreserve;
import dev.ninemmteam.mod.modules.impl.hud.ItemsCounter;
import dev.ninemmteam.mod.modules.impl.hud.TextRadar;
import dev.ninemmteam.mod.modules.impl.misc.*;
import dev.ninemmteam.mod.modules.impl.movement.*;
import dev.ninemmteam.mod.modules.impl.player.*;
import dev.ninemmteam.mod.modules.impl.render.*;
import dev.ninemmteam.mod.modules.settings.Setting;
import dev.ninemmteam.mod.modules.settings.impl.BindSetting;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import org.lwjgl.opengl.GL11;

public class ModuleManager implements Wrapper {
   private final ArrayList<Module> modules = new ArrayList();

   public ArrayList<Module> getModules() {
      return this.modules;
   }

   public ModuleManager() {
      this.init();
   }


   public void init() {
      if (fentanyl.nightly) {
         addModule(new Speed());
      }

      this.addModule(new AutoEnderChest());
      this.addModule(new Hat());
      this.addModule(new CapeModule());
      this.addModule(new Fonts());
      this.addModule(new NoTerrainScreen());
      this.addModule(new AutoCrystal());
      this.addModule(new CatAura());
      this.addModule(new AutoPlacer());
      this.addModule(new Ambience());
      this.addModule(new AntiHunger());
      this.addModule(new AntiVoid());
      this.addModule(new AutoWalk());
      this.addModule(new VClip());
      this.addModule(new LadderStep());
      this.addModule(new ExtraTab());
      this.addModule(new Aura());
      this.addModule(new AntiPush());
      this.addModule(new AntiCrystalBomb());
      this.addModule(new PistonCrystal());
      this.addModule(new AutoAnchor());
      this.addModule(new AutoArmor());
      this.addModule(new LowArmorAlert());
      this.addModule(new AutoMine());
      this.addModule(new AutoLog());
      this.addModule(new AutoEZ());
      this.addModule(new SelfTrap());
      this.addModule(new Sorter());
      this.addModule(new AutoXP());
      this.addModule(new AutoPot());
      this.addModule(new Offhand());
      this.addModule(new AutoTrap());
      this.addModule(new AutoDoor2());
      this.addModule(new AutoWeb());
      this.addModule(new AutoPlate());
      this.addModule(new Blink());
      this.addModule(new ChorusControl());
      this.addModule(new BlockStrafe());
      this.addModule(new FastSwim());
      this.addModule(new Blocker());
      this.addModule(new BowBomb());
      this.addModule(new BreakESP());
      this.addModule(new CrystalPlaceESP());
      this.addModule(new Burrow());
      this.addModule(new Burrow320());
      this.addModule(new CameraClip());
      this.addModule(new ClickGui());
      this.addModule(new UISettings());
      this.addModule(new InfiniteTrident());
      this.addModule(new ColorsModule());
      this.addModule(new AutoRegear());
      this.addModule(new LavaFiller());
      this.addModule(new AntiCheat());
      this.addModule(new ItemsCounter());
      this.addModule(new Fov());
      this.addModule(new FakeSquat());
      this.addModule(new FakePPS());
      this.addModule(new Criticals());
      this.addModule(new Crosshair());
      this.addModule(new Chams());
      this.addModule(new AntiPacket());
      this.addModule(new AutoReconnect());
      this.addModule(new ESP());
      this.addModule(new Tracers());
      this.addModule(new ElytraFly());
      this.addModule(new TeleportLogger());
      this.addModule(new SkinFlicker());
      this.addModule(new EntityControl());
      this.addModule(new NameTags());
      this.addModule(new ShulkerViewer());
      this.addModule(new PingSpoof());
      this.addModule(new FakePlayer());
      this.addModule(new AutoCC());
      this.addModule(new MotionCamera());
      this.addModule(new HighLight());
      this.addModule(new RotationGhost());
      this.addModule(new DoubleVision());
      this.addModule(new AntiShulker());
      this.addModule(new AntiRegear());
      this.addModule(new FastFall());
      this.addModule(new FastWeb());
      this.addModule(new Flatten());
      this.addModule(new Fly());
      this.addModule(new Freecam());
      this.addModule(new FreeLook());
      this.addModule(new TimerModule());
      this.addModule(new Tips());
      this.addModule(new ClientSetting());
      this.addModule(new TextRadar());
      this.addModule(new HUD());
      this.addModule(new NoResourcePack());
      this.addModule(new RocketExtend());
      this.addModule(new HoleFiller());
      this.addModule(new HighJumpFollow());
      this.addModule(new HoleSnap());
      this.addModule(new LogoutSpots());
      this.addModule(new Trajectories());
      this.addModule(new KillEffect());
      this.addModule(new AutoPearl());
      this.addModule(new AntiEffects());
      this.addModule(new NoFall());
      this.addModule(new NoRender());
      this.addModule(new NoSlow());
      this.addModule(new NoSound());
      this.addModule(new PacketEat());
      this.addModule(new PacketFly());
      this.addModule(new SpeedMine());
      this.addModule(new PacketControl());
      this.addModule(new Phase());
      this.addModule(new PlaceRender());
      this.addModule(new InteractTweaks());
      this.addModule(new PopChams());
      this.addModule(new Skeleton());
      this.addModule(new Replenish());
      this.addModule(new ServerLagger());
      this.addModule(new Scaffold());
      this.addModule(new ShaderModule());
      this.addModule(new SafeWalk());
      this.addModule(new NoJumpDelay());
      this.addModule(new HighJump());
      this.addModule(new LongJump());
      this.addModule(new LongJumpPro());
      this.addModule(new Sprint());
      this.addModule(new Strafe());
      this.addModule(new Step());
      this.addModule(new Surround());
      this.addModule(new TotemParticle());
      this.addModule(new Velocity());
      this.addModule(new ViewModel());
      this.addModule(new XCarry());
      this.addModule(new ChestStealer());
      this.addModule(new DropAntiRegear());
      this.addModule(new DropShulkerBox());
      this.addModule(new PopEz());
      this.addModule(new WindowTitle());
      this.addModule(new EsuESP());
      this.addModule(new TargetFollow());
      this.addModule(new ArmorPreserve());
      this.addModule(new AutoCommand());
      this.addModule(new AutoPush());
      this.addModule(new AutoSquare());
      this.addModule(new Disabler());
      this.addModule(new PopLag());
      this.addModule(new DeathRespawn());
      this.addModule(new DynamicPingSpoof());
      this.addModule(new AutoTK());
      this.addModule(new EatMyDick());
      this.addModule(new Auto250());
      this.addModule(new AspectRatio());
      this.addModule(new Weather());

      
      HoleSnap.init();
      this.modules.sort(Comparator.comparing(Mod::getName));
   }

   public void onKeyReleased(int eventKey) {
      if (eventKey != -1 && eventKey != 0) {
         this.handleKeyEvent(eventKey, false);
      }
   }

   public void onKeyPressed(int eventKey) {
      if (eventKey != -1 && eventKey != 0) {
         this.handleKeyEvent(eventKey, true);
      }
   }

   private void handleKeyEvent(int key, boolean isPressed) {
      for (Module module : this.modules) {
         BindSetting bindSetting = module.getBindSetting();
         if (bindSetting.getValue() == key) {
            if (isPressed && mc.currentScreen == null) {
               module.toggle();
               bindSetting.holding = true;
            } else if (!isPressed && bindSetting.isHoldEnable() && bindSetting.holding) {
               module.toggle();
               bindSetting.holding = false;
            }
         }

         for (Setting setting : module.getSettings()) {
            if (setting instanceof BindSetting bind && bind.getValue() == key) {
               bind.setPressed(isPressed);
            }
         }
      }
   }

   public void onLogin() {
      for (Module module : this.modules) {
         if (module.isOn()) {
            module.onLogin();
         }
      }
   }

   public void onLogout() {
      for (Module module : this.modules) {
         if (module.isOn()) {
            module.onLogout();
         }
      }
   }

   public void onRender2D(DrawContext drawContext) {
      for (Module module : this.modules) {
         if (module.isOn()) {
            try {
               module.onRender2D(drawContext, mc.getRenderTickCounter().getTickDelta(true));
            } catch (Exception var6) {
               var6.printStackTrace();
               if (ClientSetting.INSTANCE.debug.getValue()) {
                  CommandManager.sendMessage("§4An error has occurred (" + module.getName() + " [onRender2D]) Message: [" + var6.getMessage() + "]");
               }
            }
         }
      }

      try {
         fentanyl.EVENT_BUS.post(Render2DEvent.get(drawContext, mc.getRenderTickCounter().getTickDelta(true)));
      } catch (Exception var5) {
         var5.printStackTrace();
         if (ClientSetting.INSTANCE.debug.getValue()) {
            CommandManager.sendMessage("§4An error has occurred (Render3DEvent) Message: [" + var5.getMessage() + "]");
         }
      }
   }

   public void render3D(MatrixStack matrices) {
      GL11.glEnable(2848);

      for (Module module : this.modules) {
         if (module.isOn()) {
            try {
               module.onRender3D(matrices);
            } catch (Exception var6) {
               var6.printStackTrace();
               if (ClientSetting.INSTANCE.debug.getValue()) {
                  CommandManager.sendMessage("§4An error has occurred (" + module.getName() + " [onRender3D]) Message: [" + var6.getMessage() + "]");
               }
            }
         }
      }

      try {
         fentanyl.EVENT_BUS.post(Render3DEvent.get(matrices, mc.getRenderTickCounter().getTickDelta(true)));
      } catch (Exception var5) {
         var5.printStackTrace();
         if (ClientSetting.INSTANCE.debug.getValue()) {
            CommandManager.sendMessage("§4An error has occurred (Render3DEvent) Message: [" + var5.getMessage() + "]");
         }
      }

      GL11.glDisable(2848);
   }



   private void renderPotionListLegacy(DrawContext ctx) {
      if (mc.player != null) {
         int margin = 14;
         int startX = margin + 2;
         int startY = margin + 92;
         int pillH = 14;
         int pillPad = 6;
         int idx = 0;

         for (StatusEffectInstance se : mc.player.getStatusEffects()) {
            String name = ((StatusEffect)se.getEffectType().value()).getName().getString();
            int ticks = se.getDuration();
            int totalSec = Math.max(0, ticks / 20);
            int mm = totalSec / 60;
            int ss = totalSec % 60;
            String time = String.format("%d:%02d", mm, ss);
            String text = name + " " + time;
            int tw = ClickGui.getInstance().font.getValue() ? (int)FontManager.ui.getWidth(text) : (int)TextUtil.getWidth(text);
            int pillW = tw + pillPad * 2;
            int y = startY + idx * (pillH + 4);
            int a = 180;
            Render2DUtil.drawRoundedRect(ctx.getMatrices(), startX, y, pillW, pillH, 4.0F, new Color(255, 255, 255, a));
            Render2DUtil.drawRoundedStroke(ctx.getMatrices(), startX, y, pillW, pillH, 4.0F, new Color(220, 224, 230, 160), 48);
            int tx = startX + pillPad;
            double ty = y + (pillH - (ClickGui.getInstance().font.getValue() ? FontManager.ui.getFontHeight() : TextUtil.getHeight())) / 2.0;
            TextUtil.drawString(ctx, text, tx, ty, new Color(30, 30, 30).getRGB(), ClickGui.getInstance().font.getValue());
            if (++idx >= 5) {
               break;
            }
         }
      }
   }

   private void renderPotionBanners(DrawContext ctx) {
   }

   private String formatDuration(int ticks) {
      int totalSec = Math.max(0, ticks / 20);
      int mm = totalSec / 60;
      int ss = totalSec % 60;
      return String.format("%d:%02d", mm, ss);
   }

   private Module getModuleByDisplayName(String name) {
      for (Module m : this.modules) {
         if (m.getDisplayName().equalsIgnoreCase(name) || m.getName().equalsIgnoreCase(name)) {
            return m;
         }
      }

      return null;
   }

   public void addModule(Module module) {
      this.modules.add(module);
   }

   public Module getModuleByName(String string) {
      for (Module module : this.modules) {
         if (module.getName().equalsIgnoreCase(string)) {
            return module;
         }
      }

      return null;
   }

   public ArrayList<Module> getModulesByCategory(Module.Category category) {
      ArrayList<Module> categoryModules = new ArrayList<>();
      for (Module module : this.modules) {
         if (module.getCategory() == category) {
            categoryModules.add(module);
         }
      }
      return categoryModules;
   }

   public <T> T getModule(Class<T> clazz) {
      for (Module module : this.modules) {
         if (module.getClass() == clazz) {
            return (T)module;
         }
      }
      return null;
   }
}
