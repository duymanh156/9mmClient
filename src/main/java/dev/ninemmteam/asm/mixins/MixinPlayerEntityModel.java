package dev.ninemmteam.asm.mixins;

import dev.ninemmteam.mod.modules.impl.render.FakeSquat;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityModel.class)
public abstract class MixinPlayerEntityModel<T extends LivingEntity> extends BipedEntityModel<T> {
    
    public MixinPlayerEntityModel(ModelPart root) {
        super(root);
    }
    
    @Inject(method = "setAngles*", at = @At("TAIL"))
    private void onSetAngles(T livingEntity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch, CallbackInfo ci) {
        if (livingEntity instanceof net.minecraft.entity.player.PlayerEntity player) {
            if (FakeSquat.shouldNoLimb(player)) {
                this.leftArm.pitch = 0.0F;
                this.rightArm.pitch = 0.0F;
                this.leftLeg.pitch = 0.0F;
                this.rightLeg.pitch = 0.0F;
            }
        }
    }
}
