package dev.ninemmteam.core.impl;

import dev.ninemmteam.mod.gui.fonts.FontRenderer;
import dev.ninemmteam.mod.modules.impl.client.Fonts;
import java.awt.Font;
import java.awt.FontFormatException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import net.minecraft.client.util.math.MatrixStack;

public class FontManager {
   public static FontRenderer ui;
   public static FontRenderer small;
   public static FontRenderer icon;

   public static void init() {
      // Load UI font
      try {
         ui = assets(8.0F, "font", 0);
      } catch (Exception var1) {
         var1.printStackTrace();
         try {
            ui = new FontRenderer(new Font("Arial", 0, 8), 8.0F);
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
      
      // Load small font
      try {
         small = assets(6.0F, "font", 0);
      } catch (Exception var1) {
         var1.printStackTrace();
         try {
            small = new FontRenderer(new Font("Arial", 0, 6), 6.0F);
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
      
      // Load icon font
      try {
         icon = assetsWithoutOffset(8.0F, "icon", 0);
      } catch (Exception var1) {
         var1.printStackTrace();
         try {
            if (ui != null) {
               icon = ui;
            } else {
               icon = new FontRenderer(new Font("Arial", 0, 8), 8.0F);
            }
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
   }

   public static FontRenderer assets(float size, String font, int style, String alternate) throws IOException, FontFormatException {
      return new FontRenderer(
         loadFontFromAssets(font, style, size),
         getFont(alternate, style, (int)size),
         size
      ) {
         @Override
         public void drawString(MatrixStack stack, String s, float x, float y, float r, float g, float b, float a, boolean shadow) {
            super.drawString(stack, s, x + Fonts.INSTANCE.translate.getValueInt(), y + Fonts.INSTANCE.shift.getValueInt(), r, g, b, a, shadow);
         }
      };
   }

   public static FontRenderer assetsWithoutOffset(float size, String name, int style) throws IOException, FontFormatException {
      return new FontRenderer(
         loadFontFromAssets(name, style, size),
         size
      );
   }

   public static FontRenderer assets(float size, String name, int style) throws IOException, FontFormatException {
      return new FontRenderer(
         loadFontFromAssets(name, style, size),
         size
      ) {
         @Override
         public void drawString(MatrixStack stack, String s, float x, float y, float r, float g, float b, float a, boolean shadow) {
            super.drawString(stack, s, x + Fonts.INSTANCE.translate.getValueInt(), y + Fonts.INSTANCE.shift.getValueInt(), r, g, b, a, shadow);
         }
      };
   }

   public static FontRenderer create(int size, String font, int style, String alternate) {
      return new FontRenderer(getFont(font, style, size), getFont(alternate, style, size), size) {
         @Override
         public void drawString(MatrixStack stack, String s, float x, float y, float r, float g, float b, float a, boolean shadow) {
            super.drawString(stack, s, x + Fonts.INSTANCE.translate.getValueInt(), y + Fonts.INSTANCE.shift.getValueInt(), r, g, b, a, shadow);
         }
      };
   }

   public static FontRenderer create(int size, String font, int style) {
      return new FontRenderer(getFont(font, style, size), size) {
         @Override
         public void drawString(MatrixStack stack, String s, float x, float y, float r, float g, float b, float a, boolean shadow) {
            super.drawString(stack, s, x + Fonts.INSTANCE.translate.getValueInt(), y + Fonts.INSTANCE.shift.getValueInt(), r, g, b, a, shadow);
         }
      };
   }

   private static Font getFont(String font, int style, int size) {
      File fontDir = new File("C:\\Windows\\Fonts");

      try {
         for (File file : fontDir.listFiles()) {
            if (file.getName().replace(".ttf", "").replace(".ttc", "").replace(".otf", "").equalsIgnoreCase(font)) {
               try {
                  return Font.createFont(0, file).deriveFont(style, size);
               } catch (Exception var9) {
                  var9.printStackTrace();
               }
            }
         }

         for (File filex : fontDir.listFiles()) {
            if (filex.getName().startsWith(font)) {
               try {
                  return Font.createFont(0, filex).deriveFont(style, size);
               } catch (Exception var10) {
                  var10.printStackTrace();
               }
            }
         }
      } catch (Exception var11) {
      }

      return new Font(null, style, size);
   }

   private static Font loadFontFromAssets(String fontName, int style, float size) throws IOException, FontFormatException {
      String actualFontName = fontName;
      
      if ("default".equalsIgnoreCase(fontName)) {
         actualFontName = "font";
      }
      
      InputStream ttfStream = FontManager.class.getClassLoader().getResourceAsStream("assets/fentanyl/textures/font/" + actualFontName + ".ttf");
      if (ttfStream != null) {
         return Font.createFont(0, ttfStream).deriveFont(style, size);
      }
      
      InputStream otfStream = FontManager.class.getClassLoader().getResourceAsStream("assets/fentanyl/textures/font/" + actualFontName + ".otf");
      if (otfStream != null) {
         return Font.createFont(0, otfStream).deriveFont(style, size);
      }
      
      if (!actualFontName.equals("font")) {
         InputStream fallbackTtf = FontManager.class.getClassLoader().getResourceAsStream("assets/fentanyl/textures/font/font.ttf");
         if (fallbackTtf != null) {
            return Font.createFont(0, fallbackTtf).deriveFont(style, size);
         }
         
         InputStream fallbackOtf = FontManager.class.getClassLoader().getResourceAsStream("assets/fentanyl/textures/font/font.otf");
         if (fallbackOtf != null) {
            return Font.createFont(0, fallbackOtf).deriveFont(style, size);
         }
      }
      
      throw new IOException("Font file not found: " + fontName + " (.ttf or .otf)");
   }
}