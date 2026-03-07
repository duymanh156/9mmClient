package dev.ninemmteam.api.utils.path;

import dev.ninemmteam.api.utils.Wrapper;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;

public class BaritoneUtil implements Wrapper {
   public static boolean loaded;

   public static void gotoPos(BlockPos pos) {
      // Baritone functionality disabled - dependency removed
   }

   public static void forward() {
      // Baritone functionality disabled - dependency removed
   }

   public static void mine(Block block) {
      // Baritone functionality disabled - dependency removed
   }

   public static boolean isPathing() {
      return false; // Baritone functionality disabled - dependency removed
   }

   public static void cancelEverything() {
      // Baritone functionality disabled - dependency removed
   }

   public static boolean isActive() {
      return false; // Baritone functionality disabled - dependency removed
   }

   static {
      // Check if baritone is available (will be false since we removed the dependency)
      Package[] packages = Package.getPackages();
      loaded = false;
      for (Package pkg : packages) {
         if (pkg.getName().contains("baritone.api")) {
            loaded = true;
            break;
         }
      }
   }
}
