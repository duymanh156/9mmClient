package dev.ninemmteam.mod.gui.items.buttons;

import dev.ninemmteam.mod.gui.ClickGuiScreen;
import dev.ninemmteam.mod.modules.settings.impl.BindSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Formatting;

public class BindButton extends Button {
   private final BindSetting setting;
   public boolean isListening;

   public BindButton(BindSetting setting) {
      super(setting.getName());
      this.setting = setting;
   }

   @Override
   public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
      boolean hovering = this.isHovering(mouseX, mouseY);
      if (this.isListening) {
         this.drawString("Press a Key...", this.x + 2.3F, this.y - 1.7F - ClickGuiScreen.getInstance().getTextOffset(hovering), enableTextColor);
      } else {
         String str = this.setting.getKeyString();
         if (this.isListening
            || !hovering
            || !InputUtil.isKeyPressed(mc.getWindow().getHandle(), 340)
            || !this.setting.getName().equals("Key")) {
            this.drawString(
               this.setting.getName() + " " + Formatting.GRAY + str,
               this.x + 2.3F,
               this.y - 1.7F - ClickGuiScreen.getInstance().getTextOffset(hovering),
               this.getState() ? enableTextColor : textColor
            );
         } else if (this.setting.isHoldEnable()) {
            this.drawString("§7Toggle/§fHold", this.x + 2.3F, this.y - 1.7F - ClickGuiScreen.getInstance().getTextOffset(hovering), enableTextColor);
         } else {
            this.drawString("§fToggle§7/Hold", this.x + 2.3F, this.y - 1.7F - ClickGuiScreen.getInstance().getTextOffset(hovering), enableTextColor);
         }
      }
   }

   @Override
   public void update() {
      this.setHidden(!this.setting.isVisible());
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      if (mouseButton == 0 && this.isHovering(mouseX, mouseY)) {
         if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), 340) && this.setting.getName().equals("Key")) {
            this.setting.setHoldEnable(!this.setting.isHoldEnable());
            sound();
         } else {
            this.onMouseClick();
         }
      } else if (this.isListening) {
         this.setting.setValue(-mouseButton - 2);
         this.onMouseClick();
      }
   }

   @Override
   public void onKeyPressed(int key) {
      if (this.isListening) {
         this.setting.setValue(key);
         if (this.setting.getKeyString().equalsIgnoreCase("DELETE")) {
            this.setting.setValue(-1);
         }

         this.onMouseClick();
      }
   }

   @Override
   public void toggle() {
      this.isListening = !this.isListening;
   }

   @Override
   public boolean getState() {
      return !this.isListening;
   }
}