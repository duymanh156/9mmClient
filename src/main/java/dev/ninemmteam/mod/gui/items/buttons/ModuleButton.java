package dev.ninemmteam.mod.gui.items.buttons;

import dev.ninemmteam.api.utils.math.Animation;
import dev.ninemmteam.api.utils.render.ColorUtil;
import dev.ninemmteam.api.utils.render.Render2DUtil;
import dev.ninemmteam.mod.gui.ClickGuiScreen;
import dev.ninemmteam.mod.gui.items.Item;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.impl.client.ClickGui;
import dev.ninemmteam.mod.modules.settings.Setting;
import dev.ninemmteam.mod.modules.settings.impl.BindSetting;
import dev.ninemmteam.mod.modules.settings.impl.BooleanSetting;
import dev.ninemmteam.mod.modules.settings.impl.ColorSetting;
import dev.ninemmteam.mod.modules.settings.impl.EnumSetting;
import dev.ninemmteam.mod.modules.settings.impl.SliderSetting;
import dev.ninemmteam.mod.modules.settings.impl.StringSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;

public class ModuleButton extends Button {
   private final Module module;
   private List<Item> items = new ArrayList();
   public boolean subOpen;
   public double itemHeight;
   public final Animation animation = new Animation();

   public ModuleButton(Module module) {
      super(module.getName());
      this.module = module;
      this.initSettings();
   }

   public void initSettings() {
      ArrayList<Item> newItems = new ArrayList();

      for (Setting setting : this.module.getSettings()) {
         if (setting instanceof BooleanSetting s) {
            newItems.add(new BooleanButton(s));
         }

         if (setting instanceof BindSetting s) {
            newItems.add(new BindButton(s));
         }

         if (setting instanceof StringSetting s) {
            newItems.add(new StringButton(s));
         }

         if (setting instanceof SliderSetting s) {
            newItems.add(new SliderButton(s));
         }

         if (setting instanceof EnumSetting<?> s) {
            newItems.add(new EnumButton(s));
         }

         if (setting instanceof ColorSetting s) {
            newItems.add(new PickerButton(s));
         }
      }

      this.items = newItems;
   }

   @Override
   public void update() {
      for (Item item : this.items) {
         item.update();
      }
   }

   @Override
   public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
      boolean hovered = this.isHovering(mouseX, mouseY);
      boolean searchMatch = dev.ninemmteam.mod.gui.ClickGuiScreen.getInstance().isSearchMatch(this.module.getName());
      Color accent = ClickGui.getInstance().bgColor.getValue();
       Color outlineColor = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), accent.getAlpha());
      Render2DUtil.drawRect(context.getMatrices(), this.x, this.y, this.width, this.height - 0.5F, outlineColor);

      this.drawString(
         this.module.getDisplayName(),
         this.x + 2.3F,
         this.y - 2.0F - ClickGuiScreen.getInstance().getTextOffset(hovered),
              this.getState()
                      ? (
                      ColorUtil.injectAlpha(ClickGui.getInstance().enableTextColor.getValue(), ClickGui.getInstance().enableTextColor.getValue().getAlpha()).getRGB()
              )
                      : (ClickGui.getInstance().textColor.getValue().getRGB())
      );
      
      int bgColor;
      if (searchMatch) {
         bgColor = 0xAAFF0000;
      } else if (this.getState()) {
         bgColor = ColorUtil.injectAlpha(ClickGui.getInstance().bgColor.getValue(), ClickGui.getInstance().bgEnable.getValue().getAlpha() - 100).getRGB();
      } else if (this.isHovering(mouseX, mouseY)) {
         bgColor = bgButton;
      } else {
         bgColor = ClickGui.getInstance().bgColor.getValue().getRGB();
      }
      
      Render2DUtil.rect(
              context.getMatrices(),
              this.x,
              this.y,
              this.x + this.width,
              this.y + this.height - 0.5F,
              bgColor
      );
      if (ClickGui.getInstance().gear.getValue()) {
         this.drawString(
            this.subOpen ? "-" : "+",
            this.x + this.width - 8.0F,
            this.y - 1.7F - ClickGuiScreen.getInstance().getTextOffset(hovered),
            -1
         );
      }

      if (this.subOpen || this.itemHeight > 0.0) {
         float height = this.height + 2;

         for (Item item : this.items) {
            if (!item.isHidden()) {
               item.setHeight(this.height);
               item.setLocation(this.x + 1.0F, this.y + height);
               item.setWidth(this.width - 9);
               item.drawScreen(context, mouseX, mouseY, partialTicks);
               height += item.getHeight() + 2;
            }
         }
      }
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      super.mouseClicked(mouseX, mouseY, mouseButton);
      if (!this.items.isEmpty()) {
         if (mouseButton == 1 && this.isHovering(mouseX, mouseY)) {
            this.subOpen = !this.subOpen;
            sound();
         }

         if (this.subOpen) {
            for (Item item : this.items) {
               if (!item.isHidden()) {
                  item.mouseClicked(mouseX, mouseY, mouseButton);
               }
            }
         }
      }
   }

   @Override
   public void onKeyTyped(char typedChar, int keyCode) {
      super.onKeyTyped(typedChar, keyCode);
      if (!this.items.isEmpty() && this.subOpen) {
         for (Item item : this.items) {
            if (!item.isHidden()) {
               item.onKeyTyped(typedChar, keyCode);
            }
         }
      }
   }

   @Override
   public void onKeyPressed(int key) {
      super.onKeyPressed(key);
      if (!this.items.isEmpty() && this.subOpen) {
         for (Item item : this.items) {
            if (!item.isHidden()) {
               item.onKeyPressed(key);
            }
         }
      }
   }

   public int getButtonHeight() {
      return super.getHeight();
   }

   public int getItemHeight() {
      int height = 3;

      for (Item item : this.items) {
         if (!item.isHidden()) {
            height += item.getHeight() + 2;
         }
      }

      return height;
   }

   @Override
   public int getHeight() {
      if (this.subOpen) {
         int height = super.getHeight();

         for (Item item : this.items) {
            if (!item.isHidden()) {
               height += item.getHeight() + 1;
            }
         }

         return height + 2;
      } else {
         return super.getHeight();
      }
   }

   public Module getModule() {
      return this.module;
   }

   @Override
   public void toggle() {
      this.module.toggle();
   }

   @Override
   public boolean getState() {
      return this.module.isOn();
   }
}