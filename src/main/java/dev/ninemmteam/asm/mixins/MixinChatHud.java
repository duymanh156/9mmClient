package dev.ninemmteam.asm.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import dev.ninemmteam.fentanyl;
import dev.ninemmteam.api.interfaces.IChatHudHook;
import dev.ninemmteam.api.interfaces.IChatHudLineHook;
import dev.ninemmteam.api.utils.math.AnimateUtil;
import dev.ninemmteam.api.utils.math.Easing;
import dev.ninemmteam.api.utils.math.FadeUtils;
import dev.ninemmteam.api.utils.render.ColorUtil;
import dev.ninemmteam.api.utils.render.Render2DUtil;
import java.awt.Color;
import dev.ninemmteam.asm.accessors.IChatHud;
import dev.ninemmteam.mod.modules.impl.client.ClientSetting;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.client.gui.hud.ChatHudLine.Visible;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.ninemmteam.api.events.impl.ReceiveMessageEvent;

@Mixin(ChatHud.class)
public abstract class MixinChatHud implements IChatHudHook {
   @Unique private int minX = Integer.MAX_VALUE;
   @Unique private int minY = Integer.MAX_VALUE;
   @Unique private int maxX = Integer.MIN_VALUE;
   @Unique private int maxY = Integer.MIN_VALUE;
   @Unique private int prevMinX = 0;
   @Unique private int prevMinY = 0;
   @Unique private int prevMaxX = 0;
   @Unique private int prevMaxY = 0;
   @Unique private boolean hasBounds = false;
   @Unique private double animationMinX = 0;
   @Unique private double animationMinY = 0;
   @Unique private double animationMaxX = 0;
   @Unique private double animationMaxY = 0;

   @Unique
   private int nextMessageId = 0;
   @Unique
   private boolean nextSync;
   @Unique
   private int chatLineIndex;
   @Final
   @Shadow
   private List<Visible> visibleMessages;
   @Shadow
   @Final
   private List<ChatHudLine> messages;

   @Inject(method = "render", at = @At("HEAD"))
   private void onRenderStart(DrawContext context, int currentTick, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
      if (ClientSetting.INSTANCE.isOn() && ClientSetting.INSTANCE.customChatBackground.getValue() && hasBounds) {
          float radius = (float) ClientSetting.INSTANCE.chatBackgroundRound.getValue();
          double speed = ClientSetting.INSTANCE.chatAnimationSpeed.getValue();
          
          if (animationMinX == 0 || Math.abs(animationMinX - prevMinX) > 1000) animationMinX = prevMinX;
          if (animationMinY == 0 || Math.abs(animationMinY - prevMinY) > 1000) animationMinY = prevMinY;
          if (animationMaxX == 0 || Math.abs(animationMaxX - prevMaxX) > 1000) animationMaxX = prevMaxX;
          if (animationMaxY == 0 || Math.abs(animationMaxY - prevMaxY) > 1000) animationMaxY = prevMaxY;

          animationMinX = AnimateUtil.animate(animationMinX, prevMinX, speed);
          animationMinY = AnimateUtil.animate(animationMinY, prevMinY, speed);
          animationMaxX = AnimateUtil.animate(animationMaxX, prevMaxX, speed);
          animationMaxY = AnimateUtil.animate(animationMaxY, prevMaxY, speed);

          float currentX = (float)animationMinX - 4f + 6F;
          float currentY = (float)animationMinY - 4f;
          float finalWidth = (float)(animationMaxX - animationMinX) + 8f;
          float finalHeight = (float)(animationMaxY - animationMinY) + 8f;
          
          int alpha = (int)ClientSetting.INSTANCE.chatBackgroundAlpha.getValue();
          Render2DUtil.drawRoundedRect(context.getMatrices(), currentX, currentY, finalWidth, finalHeight, radius, new Color(18, 18, 18, alpha));
      }
      
      minX = Integer.MAX_VALUE;
      minY = Integer.MAX_VALUE;
      maxX = Integer.MIN_VALUE;
      maxY = Integer.MIN_VALUE;
   }

   @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"))
   private void redirectChatBackground(DrawContext context, int x1, int y1, int x2, int y2, int color) {
      if (ClientSetting.INSTANCE.isOn() && ClientSetting.INSTANCE.customChatBackground.getValue()) {
          int CHAT_MARGIN_LEFT = 4;
          minX = Math.min(minX, x1 + CHAT_MARGIN_LEFT);
          minY = Math.min(minY, y1);
          maxX = Math.max(maxX, x2 + CHAT_MARGIN_LEFT);
          maxY = Math.max(maxY, y2);
      } else {
          context.fill(x1, y1, x2, y2, color);
      }
   }

   @Inject(method = "render", at = @At("TAIL"))
   private void onRenderEnd(DrawContext context, int currentTick, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
       if (minX != Integer.MAX_VALUE) {
           prevMinX = minX;
           prevMinY = minY;
           prevMaxX = maxX;
           prevMaxY = maxY;
           hasBounds = true;
       } else {
           hasBounds = false;
       }
   }

   @Inject(method = "<init>", at = @At("TAIL"))
   public void onInit(MinecraftClient client, CallbackInfo ci) {
      ((IChatHud)this).setMessages(new CopyOnWriteArrayList());
      ((IChatHud)this).setVisibleMessages(new CopyOnWriteArrayList());
   }

   @Override
   public void fentany1Client$addMessage(Text message, int id) {
      this.nextMessageId = id;
      this.nextSync = true;
      this.addMessage(message);
      this.nextSync = false;
      this.nextMessageId = 0;
   }

   @Override
   public void fentany1Client$addMessage(Text message) {
      this.nextSync = true;
      this.addMessage(message);
      this.nextSync = false;
   }

   @Override
   public void fentany1Client$addMessageOutSync(Text message, int id) {
      this.nextMessageId = id;
      this.addMessage(message);
      this.nextMessageId = 0;
   }

   @Inject(method = "addVisibleMessage", at = @At(value = "INVOKE", target = "Ljava/util/List;add(ILjava/lang/Object;)V", ordinal = 0, shift = Shift.AFTER))
   private void onAddMessageAfterNewChatHudLineVisible(ChatHudLine message, CallbackInfo ci) {
      IChatHudLineHook line = (IChatHudLineHook)(Object)this.visibleMessages.getFirst();
      if (line != null) {
         line.fentany1Client$setMessageId(this.nextMessageId);
         line.fentany1Client$setSync(this.nextSync);
         line.fentany1Client$setFade(new FadeUtils(ClientSetting.INSTANCE.animationTime.getValueInt()));
      }
   }

   @Inject(
      method = "addMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V",
      at = @At(value = "INVOKE", target = "Ljava/util/List;add(ILjava/lang/Object;)V", ordinal = 0, shift = Shift.AFTER)
   )
   private void onAddMessageAfterNewChatHudLine(ChatHudLine message, CallbackInfo ci) {
      IChatHudLineHook line = (IChatHudLineHook)(Object)this.messages.getFirst();
      if (line != null) {
         line.fentany1Client$setMessageId(this.nextMessageId);
         line.fentany1Client$setSync(this.nextSync);
         line.fentany1Client$setFade(new FadeUtils(ClientSetting.INSTANCE.animationTime.getValueInt()));
      }
   }

   @Inject(
      at = @At("HEAD"),
      method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V"
   )
   private void onAddMessage(Text message, MessageSignatureData signatureData, MessageIndicator indicator, CallbackInfo ci) {
      ReceiveMessageEvent event = ReceiveMessageEvent.get(message);
      fentanyl.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         ci.cancel();
         return;
      }
      if (this.nextMessageId != 0) {
         this.visibleMessages.removeIf(msg -> ((IChatHudLineHook)(Object)msg).fentany1Client$getMessageId() == this.nextMessageId);
         this.messages.removeIf(msg -> ((IChatHudLineHook)(Object)msg).fentany1Client$getMessageId() == this.nextMessageId);
      }
   }

   @Redirect(method = "addVisibleMessage", at = @At(value = "INVOKE", target = "Ljava/util/List;size()I", ordinal = 2, remap = false), require = 0)
   public int chatLinesSize(List<Visible> list) {
      return ClientSetting.INSTANCE.isOn() && ClientSetting.INSTANCE.infiniteChat.getValue() ? -2147483647 : list.size();
   }

   @Redirect(
      method = "render",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;III)I"
      ),
      require = 0
   )
   private int drawStringWithShadow(DrawContext drawContext, TextRenderer textRenderer, OrderedText text, int x, int y, int color) {
      IChatHudLineHook line = (IChatHudLineHook)(Object)this.visibleMessages.get(this.chatLineIndex);
      if (line != null) {
         FadeUtils fadeUtils = line.fentany1Client$getFade();
         double ease = fadeUtils == null ? 0.0 : fadeUtils.ease((Easing)ClientSetting.INSTANCE.ease.getValue());
         double fade = 1.0 - ease;
         x += (int)(fade * ClientSetting.INSTANCE.animateOffset.getValue());
         double c = Math.max(10.0, (color >> 24 & 0xFF) * ease);
         return line.fentany1Client$getSync()
            ? drawContext.drawTextWithShadow(
               textRenderer,
               text,
               x,
               y,
               ColorUtil.injectAlpha(ClientSetting.INSTANCE.color.getValue(), ClientSetting.INSTANCE.fade.getValue() ? (int)c : color >> 24 & 0xFF).getRGB()
            )
            : drawContext.drawTextWithShadow(
               textRenderer, text, x, y, ColorUtil.injectAlpha(color, ClientSetting.INSTANCE.fade.getValue() ? (int)c : color >> 24 & 0xFF)
            );
      } else {
         return drawContext.drawTextWithShadow(textRenderer, text, x, y, color);
      }
   }

   @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHudLine$Visible;addedTime()I"))
   public void getChatLineIndex(CallbackInfo ci, @Local(ordinal = 13) int chatLineIndex) {
      this.chatLineIndex = chatLineIndex;
   }

   @ModifyVariable(method = "render", at = @At("STORE"))
   private MessageIndicator removeMessageIndicator(MessageIndicator messageIndicator) {
      return ClientSetting.INSTANCE.hideIndicator.getValue() ? null : messageIndicator;
   }

   @Shadow
   public abstract void addMessage(Text var1);

   @Shadow
   public int getWidth() {
      return 0;
   }
}
