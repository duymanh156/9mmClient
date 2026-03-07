package dev.ninemmteam.mod.gui.items.buttons;

import dev.ninemmteam.api.utils.render.ColorUtil;
import dev.ninemmteam.api.utils.render.Render2DUtil;
import dev.ninemmteam.mod.gui.ClickGuiScreen;
import dev.ninemmteam.mod.gui.items.Component;
import dev.ninemmteam.mod.gui.items.Item;
import dev.ninemmteam.mod.modules.impl.client.ClickGui;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;

public class Button extends Item {
   private boolean state;
   public static int bgButton = 290805077;
   public static int textColor = -5592406;
   public static int enableTextColor = -1;

   public Button(String name) {
      super(name);
      this.setHeight(15);
   }

   @Override
   public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
      Color color = ClickGui.getInstance().bgColor.getValue();
      Render2DUtil.rect(
              context.getMatrices(),
              this.x,
              this.y,
              this.x + this.width,
              this.y + this.height - 0.5F,
              this.getState()
                      ? (
                      !this.isHovering(mouseX, mouseY)
                              ? ColorUtil.injectAlpha(color, ClickGui.getInstance().color.getValue().getAlpha()).getRGB()
                              : ColorUtil.injectAlpha(color, ClickGui.getInstance().bgButton.getValue().getAlpha()).getRGB()
              )
                      : (!this.isHovering(mouseX, mouseY) ? color.getRGB() : bgButton)
      );
      this.drawString(
         this.getName(), this.x + 2.3F, this.y - 2.0F - ClickGuiScreen.getInstance().getTextOffset(this.isHovering(mouseX, mouseY)), this.getState() ? enableTextColor : textColor
      );
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      if (mouseButton == 0 && this.isHovering(mouseX, mouseY)) {
         this.onMouseClick();
      }
   }

   public void onMouseClick() {
      this.state = !this.state;
      this.toggle();
      sound();
   }

   public void toggle() {
   }

   public boolean getState() {
      return this.state;
   }

   @Override
   public int getHeight() {
      return this.height - 1;
   }

   public boolean isHovering(int mouseX, int mouseY) {
      for (Component component : ClickGuiScreen.getInstance().getComponents()) {
         if (component.drag) {
            return false;
         }
      }

      return mouseX >= this.getX() && mouseX <= this.getX() + this.getWidth() && mouseY >= this.getY() && mouseY <= this.getY() + this.height - 1.0F;
   }
}