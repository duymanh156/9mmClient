package dev.ninemmteam.asm.mixins;

import dev.ninemmteam.mod.modules.impl.client.CapeModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListEntry.class)
public class MixinPlayerListEntry {

    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void onGetSkinTextures(CallbackInfoReturnable<SkinTextures> cir) {
        if (MinecraftClient.getInstance().player == null) return;

        PlayerListEntry self = (PlayerListEntry) (Object) this;

        if (!self.getProfile().getId().equals(MinecraftClient.getInstance().player.getUuid())) {
            return;
        }

        if (CapeModule.INSTANCE == null || !CapeModule.INSTANCE.isOn()) {
            return;
        }

        SkinTextures original = cir.getReturnValue();

        try {
            SkinTextures newTextures = new SkinTextures(
                    original.texture(),
                    original.textureUrl(),
                    CapeModule.INSTANCE.getSelectedTexture(),
                    CapeModule.INSTANCE.getSelectedTexture(),
                    original.model(),
                    original.secure()
            );
            cir.setReturnValue(newTextures);
        } catch (Exception e) {
            System.err.println("CapeModule: Failed to create SkinTextures: " + e.getMessage());
        }
    }
}
