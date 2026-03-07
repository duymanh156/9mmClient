package dev.ninemmteam.asm.mixins;

import dev.ninemmteam.fentanyl;
import dev.ninemmteam.api.events.impl.AmbientOcclusionEvent;
import dev.ninemmteam.api.events.impl.CollisionBoxEvent;
import dev.ninemmteam.mod.modules.impl.player.SpeedMine;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlock.class)
public abstract class MixinAbstractBlock {
   @Inject(method = "getAmbientOcclusionLightLevel", at = @At("HEAD"), cancellable = true)
   private void onGetAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos, CallbackInfoReturnable<Float> info) {
      AmbientOcclusionEvent event = fentanyl.EVENT_BUS.post(AmbientOcclusionEvent.get());
      if (event.lightLevel != -1.0F) {
         info.setReturnValue(event.lightLevel);
      }
   }
}
