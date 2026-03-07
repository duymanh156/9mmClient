package dev.ninemmteam.mod.modules.impl.player;

import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ArmorItem;

public class LowArmorAlert extends Module {
    public static LowArmorAlert INSTANCE;
    
    private final SliderSetting threshold = this.add(new SliderSetting("Threshold", 35.0, 0.0, 100.0, 1.0));
    private final SliderSetting yOffset = this.add(new SliderSetting("YOffset", -60, 0.0, 200.0, 1.0));
    private long lastFlashTime;
    
    public LowArmorAlert() {
        super("LowArmorAlert", Module.Category.Player);
        this.setChinese("低盔甲耐久警报");
        INSTANCE = this;
    }
    
    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) {
        if (nullCheck()) return;
        
        double thresholdValue = threshold.getValue();
        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();
        int yOffset = (int) this.yOffset.getValue();
        
        boolean hasLowDurability = false;
        
        ItemStack helmet = mc.player.getInventory().armor.get(3);
        if (!helmet.isEmpty() && helmet.getItem() instanceof ArmorItem) {
            float durabilityPercent = getDurabilityPercent(helmet);
            if (durabilityPercent < thresholdValue) {
                hasLowDurability = true;
            }
        }
        
        ItemStack chestplate = mc.player.getInventory().armor.get(2);
        if (!chestplate.isEmpty() && chestplate.getItem() instanceof ArmorItem) {
            float durabilityPercent = getDurabilityPercent(chestplate);
            if (durabilityPercent < thresholdValue) {
                hasLowDurability = true;
            }
        }
        
        ItemStack leggings = mc.player.getInventory().armor.get(1);
        if (!leggings.isEmpty() && leggings.getItem() instanceof ArmorItem) {
            float durabilityPercent = getDurabilityPercent(leggings);
            if (durabilityPercent < thresholdValue) {
                hasLowDurability = true;
            }
        }
        
        ItemStack boots = mc.player.getInventory().armor.get(0);
        if (!boots.isEmpty() && boots.getItem() instanceof ArmorItem) {
            float durabilityPercent = getDurabilityPercent(boots);
            if (durabilityPercent < thresholdValue) {
                hasLowDurability = true;
            }
        }
        
        if (hasLowDurability) {
            drawWarning(drawContext, "Your armor durability is low!", screenWidth, screenHeight, yOffset);
        }
    }
    
    private float getDurabilityPercent(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        float maxDamage = stack.getMaxDamage();
        float currentDamage = stack.getDamage();
        float durability = maxDamage - currentDamage;
        return (durability / maxDamage) * 100.0f;
    }
    
    private void drawWarning(DrawContext drawContext, String text, int screenWidth, int screenHeight, int yOffset) {
        int textWidth = mc.textRenderer.getWidth(text);
        int textHeight = mc.textRenderer.fontHeight;
        int x = (screenWidth - textWidth) / 2;
        int y = (screenHeight / 2) - 100 + yOffset;
        
        long currentTime = System.currentTimeMillis();
        boolean shouldFlash = (currentTime - lastFlashTime) < 250;
        if ((currentTime - lastFlashTime) >= 500) {
            lastFlashTime = currentTime;
        }
        
        if (shouldFlash) {
            int padding = 2;
            int bgColor = 0x80AA0000; 
            
            int left = x - padding;
            int top = y - padding;
            int right = x + textWidth + padding;
            int bottom = y + textHeight + padding;
            
            drawContext.fill(left + 1, top, right - 1, top + 1, bgColor);
            
            drawContext.fill(left + 1, bottom - 1, right - 1, bottom, bgColor);
            
            drawContext.fill(left, top + 1, left + 1, bottom - 1, bgColor);
            
            drawContext.fill(right - 1, top + 1, right, bottom - 1, bgColor);
            
            drawContext.fill(left + 1, top + 1, right - 1, bottom - 1, bgColor);
        }
        
        drawContext.drawText(mc.textRenderer, text, x, y, 0xFFFF0000, false);
    }
}