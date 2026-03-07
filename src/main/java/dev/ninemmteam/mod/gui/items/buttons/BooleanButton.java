package dev.ninemmteam.mod.gui.items.buttons;

import dev.ninemmteam.api.utils.render.ColorUtil;
import dev.ninemmteam.api.utils.render.Render2DUtil;
import dev.ninemmteam.mod.gui.ClickGuiScreen;
import dev.ninemmteam.mod.modules.impl.client.ClickGui;
import dev.ninemmteam.mod.modules.settings.impl.BooleanSetting;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;

public class BooleanButton extends Button {
   private final BooleanSetting setting;

   public BooleanButton(BooleanSetting setting) {
      super(setting.getName());
      this.setting = setting;
   }

   @Override
   public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
      Color color = ClickGui.getInstance().color.getValue();
      Render2DUtil.rect(
              context.getMatrices(),
              this.x,
              this.y,
              this.x + this.width + 7.0F,
              this.y + this.height - 0.5F,
              this.getState()
                      ? (
                      ColorUtil.injectAlpha(color, ClickGui.getInstance().color.getValue().getAlpha()).getRGB()
              )
                      : (bgButton)
      );
      boolean hovering = this.isHovering(mouseX, mouseY);
      this.drawString(
         this.getName(), this.x + 2.3F, this.y - 1.7F - ClickGuiScreen.getInstance().getTextOffset(hovering), textColor
      );
      if (this.setting.hasParent()) {
         this.drawString(
            this.setting.isOpen() ? "-" : "+",
            this.x + this.width - 1.0F,
            this.y - 1.7F - ClickGuiScreen.getInstance().getTextOffset(hovering),
            textColor
         );
      }
   }

   @Override
   public void update() {
      this.setHidden(!this.setting.isVisible());
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      super.mouseClicked(mouseX, mouseY, mouseButton);
      if (mouseButton == 1 && this.isHovering(mouseX, mouseY)) {
         sound();
         this.setting.setOpen(!this.setting.isOpen());
      }
   }

   @Override
   public void toggle() {
      this.setting.setValue(!this.setting.getValue());
   }

   @Override
   public boolean getState() {
      return this.setting.getValue();
   }
}