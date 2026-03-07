package dev.ninemmteam.asm.mixins;

import dev.ninemmteam.fentanyl;
import dev.ninemmteam.api.events.impl.CollisionBoxEvent;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.impl.player.SpeedMine;
import dev.ninemmteam.mod.modules.impl.render.Ambience;
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

@Mixin(AbstractBlock.AbstractBlockState.class)
public abstract class MixinAbstractBlockState {
   @Inject(method = "getLuminance", at = @At("HEAD"), cancellable = true)
   public void getLuminanceHook(CallbackInfoReturnable<Integer> cir) {
      if (!Module.nullCheck()) {
         if (Ambience.INSTANCE.customLuminance.getValue()) {
            cir.setReturnValue(Ambience.INSTANCE.luminance.getValueInt());
         }
      }
   }

   @Inject(method = "getCollisionShape(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/ShapeContext;)Lnet/minecraft/util/shape/VoxelShape;", at = @At("RETURN"), cancellable = true, require = 0)
   public void onGetCollisionShape(BlockView world, BlockPos pos, ShapeContext context, CallbackInfoReturnable<VoxelShape> cir) {
      BlockState state = (BlockState) (Object) this;
      
      if (SpeedMine.INSTANCE != null && pos.equals(SpeedMine.getBreakPos()) && SpeedMine.INSTANCE.noCollide.getValue() && SpeedMine.ghost) {
         cir.setReturnValue(VoxelShapes.empty());
         return;
      }

      VoxelShape originalShape = cir.getReturnValue();
      CollisionBoxEvent event = fentanyl.EVENT_BUS.post(CollisionBoxEvent.get(originalShape, pos, state));
      if (event.isCancelled()) {
         cir.setReturnValue(VoxelShapes.empty());
      } else if (event.getVoxelShape() != originalShape) {
         cir.setReturnValue(event.getVoxelShape());
      }
   }
}
