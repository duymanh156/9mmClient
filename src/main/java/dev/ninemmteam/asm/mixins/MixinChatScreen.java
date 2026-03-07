package dev.ninemmteam.asm.mixins;

import dev.ninemmteam.api.utils.render.Render2DUtil;
import dev.ninemmteam.mod.modules.impl.client.ClientSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.Color;

import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChatScreen.class)
public abstract class MixinChatScreen extends Screen {
    @Shadow protected TextFieldWidget chatField;

    protected MixinChatScreen(Text title) {
        super(title);
    }

    @Unique
    private float animationProgress = 0f;
    @Unique
    private long lastTime = 0;

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        animationProgress = 0f;
        lastTime = System.currentTimeMillis();
        
        if (ClientSetting.INSTANCE.isOn() && ClientSetting.INSTANCE.customChatBackground.getValue()) {
            if (this.chatField != null) {
                // Sakura 样式：输入框宽度为 340
                // 原版 x=4，所以宽度设为 340 - 8 = 332 左右，保留 padding
                this.chatField.setWidth(332);
            }
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderStart(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (ClientSetting.INSTANCE.isOn() && ClientSetting.INSTANCE.customChatBackground.getValue()) {
            long currentTime = System.currentTimeMillis();
            if (lastTime == 0) lastTime = currentTime;
            float deltaTime = (currentTime - lastTime) / 1000f;
            lastTime = currentTime;

            // 基础速度乘数，让动画更灵敏
            float speed = (float) ClientSetting.INSTANCE.chatAnimationSpeed.getValue() * 10.0f;
            
            if (animationProgress < 1.0f) {
                animationProgress += deltaTime * speed;
                if (animationProgress > 1.0f) animationProgress = 1.0f;
            }

            // 缓动函数 (EaseOutCubic): 1 - pow(1 - x, 3)
            float eased = 1.0f - (float) Math.pow(1.0f - animationProgress, 3);
            
            // 从底部下移 20 像素的位置滑入
            float offset = (1.0f - eased) * 20.0f;
            
            context.getMatrices().push();
            context.getMatrices().translate(0, offset, 0);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderEnd(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (ClientSetting.INSTANCE.isOn() && ClientSetting.INSTANCE.customChatBackground.getValue()) {
            context.getMatrices().pop();
        }
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"))
    private void redirectFill(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        if (ClientSetting.INSTANCE.isOn() && ClientSetting.INSTANCE.customChatBackground.getValue()) {
            float radius = (float) ClientSetting.INSTANCE.chatBackgroundRound.getValue();
            int alpha = (int) ClientSetting.INSTANCE.chatBackgroundAlpha.getValue();
            
            // Sakura 样式：固定宽度 340
            int width = 340;
            int height = y2 - y1;
            
            // 绘制圆角背景
            Render2DUtil.drawRoundedRect(context.getMatrices(), x1, y1, width, height, radius, new Color(18, 18, 18, alpha));
            
            // 绘制描边 (Sakura 颜色: 255, 183, 197)
            // 描边不透明度跟随动画可能比较复杂，这里先跟随背景设置或固定
            Render2DUtil.drawRoundedStroke(context.getMatrices(), x1, y1, width, height, radius, new Color(255, 183, 197, 255), 1);
        } else {
            context.fill(x1, y1, x2, y2, color);
        }
    }
}
