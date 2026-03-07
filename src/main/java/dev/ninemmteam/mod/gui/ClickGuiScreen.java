package dev.ninemmteam.mod.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.ninemmteam.fentanyl;
import dev.ninemmteam.api.utils.Wrapper;
import dev.ninemmteam.mod.Mod;
import dev.ninemmteam.mod.gui.items.Component;
import dev.ninemmteam.mod.gui.items.Item;
import dev.ninemmteam.mod.gui.items.buttons.Button;
import dev.ninemmteam.mod.gui.items.buttons.ModuleButton;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.impl.client.ClickGui;

import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

public class ClickGuiScreen extends Screen {
   private static ClickGuiScreen INSTANCE = new ClickGuiScreen();
   private final ArrayList<Component> components = new ArrayList();
   
   private String searchText = "";
   private boolean searchFocused = false;
   private int searchBoxX;
   private int searchBoxY;
   private int searchBoxWidth = 150;
   private int searchBoxHeight = 16;

   public ClickGuiScreen() {
      super(Text.literal("fent@nyl"));
      this.setInstance();
      this.load();
   }
   
   public static ClickGuiScreen getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new ClickGuiScreen();
      }

      return INSTANCE;
   }

   private void setInstance() {
      INSTANCE = this;
   }

   
   private void load() {
      this.components.clear();
      int x = -84;

      for (final Module.Category category : Module.Category.values()) {
          String var10004 = category.toString();
         x += 94;
         this.components.add(new Component(var10004, category, x, 4, true) {
            @Override
            public void setupItems() {
               for (Module module : fentanyl.MODULE.getModules()) {
                  if (module.getCategory().equals(category)) {
                     this.addButton(new ModuleButton(module));
                  }
               }
            }
         });
      }

      this.components.forEach(components -> components.getItems().sort(Comparator.comparing(Mod::getName)));
   }
   
   public void refresh() {
      this.load();
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      float a = (float)ClickGui.getInstance().alphaValue;
      float scale = 0.92F + 0.08F * a;
      float slideY = (1.0F - a) * 20.0F;
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, a);
      Item.context = context;
      this.renderBackground(context, mouseX, mouseY, delta);
      int minX = Integer.MAX_VALUE;
      int minY = Integer.MAX_VALUE;
      int maxX = Integer.MIN_VALUE;
      int maxY = Integer.MIN_VALUE;

      for (Component c : this.components) {
         minX = Math.min(minX, c.getX());
         minY = Math.min(minY, c.getY());
         maxX = Math.max(maxX, c.getX() + c.getWidth());
         maxY = Math.max(maxY, c.getY() + c.getHeight());
      }

      int margin = 16;
      int panelX = Math.max(8, minX - margin);
      int panelY = Math.max(6, minY - margin);
      int panelW = Math.min(context.getScaledWindowWidth() - panelX - 8, maxX - minX + margin * 2);
      int panelH = Math.min(context.getScaledWindowHeight() - panelY - 6, maxY - minY + margin * 2 + 24);
      boolean focused = mouseX >= panelX && mouseX <= panelX + panelW && mouseY >= panelY && mouseY <= panelY + panelH;
      context.getMatrices().push();
      context.getMatrices().translate(0.0F, slideY, 0.0F);
      context.getMatrices().scale(scale, scale, 1.0F);
      this.components.forEach(components -> components.drawScreen(context, mouseX, mouseY, delta));
      context.getMatrices().pop();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      
      renderSearchBox(context, mouseX, mouseY);
   }
   
   private void renderSearchBox(DrawContext context, int mouseX, int mouseY) {
      searchBoxX = context.getScaledWindowWidth() - searchBoxWidth - 10;
      searchBoxY = context.getScaledWindowHeight() - searchBoxHeight - 10;
      
      int bgColor = searchFocused ? 0xDD333333 : 0xAA222222;
      int borderColor = searchFocused ? 0xFF0050FF : 0xFF444444;
      
      context.fill(searchBoxX, searchBoxY, searchBoxX + searchBoxWidth, searchBoxY + searchBoxHeight, bgColor);
      context.drawBorder(searchBoxX, searchBoxY, searchBoxWidth, searchBoxHeight, borderColor);
      
      String displayText = searchText;
      if (searchText.isEmpty() && !searchFocused) {
         displayText = "Search...";
      }
      
      int textColor = searchText.isEmpty() && !searchFocused ? 0x88888888 : 0xFFFFFFFF;
      context.drawText(this.client.textRenderer, displayText, searchBoxX + 4, searchBoxY + 4, textColor, false);
      
      if (searchFocused && System.currentTimeMillis() % 1000 < 500) {
         int cursorX = searchBoxX + 4 + this.client.textRenderer.getWidth(searchText);
         context.fill(cursorX, searchBoxY + 3, cursorX + 1, searchBoxY + searchBoxHeight - 3, 0xFFFFFFFF);
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int clickedButton) {
      int mx = (int) mouseX;
      int my = (int) mouseY;
      
      if (mx >= searchBoxX && mx <= searchBoxX + searchBoxWidth && my >= searchBoxY && my <= searchBoxY + searchBoxHeight) {
         searchFocused = true;
         return true;
      } else {
         searchFocused = false;
      }
      
      this.components.forEach(components -> components.mouseClicked((int)mouseX, (int)mouseY, clickedButton));
      return super.mouseClicked(mouseX, mouseY, clickedButton);
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int releaseButton) {
      this.components.forEach(components -> components.mouseReleased((int)mouseX, (int)mouseY, releaseButton));
      return super.mouseReleased(mouseX, mouseY, releaseButton);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (InputUtil.isKeyPressed(Wrapper.mc.getWindow().getHandle(), 340)) {
         if (verticalAmount < 0.0) {
            this.components.forEach(component -> component.setX(component.getX() - 15));
         } else if (verticalAmount > 0.0) {
            this.components.forEach(component -> component.setX(component.getX() + 15));
         }
      } else if (verticalAmount < 0.0) {
         this.components.forEach(component -> component.setY(component.getY() - 15));
      } else if (verticalAmount > 0.0) {
         this.components.forEach(component -> component.setY(component.getY() + 15));
      }

      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (searchFocused) {
         if (keyCode == 256) {
            searchFocused = false;
            return true;
         }
         if (keyCode == 259 && !searchText.isEmpty()) {
            searchText = searchText.substring(0, searchText.length() - 1);
            return true;
         }
         return true;
      }
      this.components.forEach(component -> component.onKeyPressed(keyCode));
      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      if (searchFocused) {
         if (chr >= 32 && chr <= 126) {
            searchText += chr;
         }
         return true;
      }
      this.components.forEach(component -> component.onKeyTyped(chr, modifiers));
      return super.charTyped(chr, modifiers);
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   public final ArrayList<Component> getComponents() {
      return this.components;
   }

   public int getTextOffset() {
      return -ClickGui.getInstance().textOffset.getValueInt() - 6;
   }
   
   public int getTextOffset(boolean isHovering) {
      if (isHovering) {
         return -ClickGui.getInstance().textOffset.getValueInt() - 5;
      } else {
         return -ClickGui.getInstance().textOffset.getValueInt() - 6;
      }
   }
   
   public String getSearchText() {
      return searchText;
   }
   
   public boolean isSearchMatch(String moduleName) {
      if (searchText.isEmpty()) {
         return false;
      }
      String search = searchText.toLowerCase();
      String name = moduleName.toLowerCase();
      return name.contains(search);
   }
}
