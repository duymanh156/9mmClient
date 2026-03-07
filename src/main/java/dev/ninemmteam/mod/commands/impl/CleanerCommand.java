package dev.ninemmteam.mod.commands.impl;

import dev.ninemmteam.fentanyl;
import dev.ninemmteam.core.impl.PlayerManager;
import dev.ninemmteam.mod.commands.Command;
import dev.ninemmteam.mod.gui.windows.WindowsScreen;
import dev.ninemmteam.mod.gui.windows.impl.ItemSelectWindow;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.Items;

public class CleanerCommand extends Command {
   public CleanerCommand() {
      super("cleaner", "[\"\"/name/reset/clear/list] | [add/remove] [name]");
   }

   @Override
   public void runCommand(String[] parameters) {
      if (parameters.length == 0) {
         PlayerManager.screenToOpen = new WindowsScreen(new ItemSelectWindow(fentanyl.CLEANER));
      } else {
         String var2 = parameters[0];
         switch (var2) {
            case "reset":
               fentanyl.CLEANER.clear();
               fentanyl.CLEANER.add(Items.NETHERITE_SWORD.getTranslationKey());
               fentanyl.CLEANER.add(Items.NETHERITE_PICKAXE.getTranslationKey());
               fentanyl.CLEANER.add(Items.NETHERITE_HELMET.getTranslationKey());
               fentanyl.CLEANER.add(Items.NETHERITE_CHESTPLATE.getTranslationKey());
               fentanyl.CLEANER.add(Items.NETHERITE_LEGGINGS.getTranslationKey());
               fentanyl.CLEANER.add(Items.NETHERITE_BOOTS.getTranslationKey());
               fentanyl.CLEANER.add(Items.OBSIDIAN.getTranslationKey());
               fentanyl.CLEANER.add(Items.ENDER_CHEST.getTranslationKey());
               fentanyl.CLEANER.add(Items.ENDER_PEARL.getTranslationKey());
               fentanyl.CLEANER.add(Items.ENCHANTED_GOLDEN_APPLE.getTranslationKey());
               fentanyl.CLEANER.add(Items.EXPERIENCE_BOTTLE.getTranslationKey());
               fentanyl.CLEANER.add(Items.COBWEB.getTranslationKey());
               fentanyl.CLEANER.add(Items.POTION.getTranslationKey());
               fentanyl.CLEANER.add(Items.SPLASH_POTION.getTranslationKey());
               fentanyl.CLEANER.add(Items.TOTEM_OF_UNDYING.getTranslationKey());
               fentanyl.CLEANER.add(Items.END_CRYSTAL.getTranslationKey());
               fentanyl.CLEANER.add(Items.ELYTRA.getTranslationKey());
               fentanyl.CLEANER.add(Items.FLINT_AND_STEEL.getTranslationKey());
               fentanyl.CLEANER.add(Items.PISTON.getTranslationKey());
               fentanyl.CLEANER.add(Items.STICKY_PISTON.getTranslationKey());
               fentanyl.CLEANER.add(Items.REDSTONE_BLOCK.getTranslationKey());
               fentanyl.CLEANER.add(Items.GLOWSTONE.getTranslationKey());
               fentanyl.CLEANER.add(Items.RESPAWN_ANCHOR.getTranslationKey());
               fentanyl.CLEANER.add(Items.ANVIL.getTranslationKey());
               this.sendChatMessage("§fItems list got reset");
               return;
            case "clear":
               fentanyl.CLEANER.getList().clear();
               this.sendChatMessage("§fItems list got clear");
               return;
            case "list":
               if (fentanyl.CLEANER.getList().isEmpty()) {
                  this.sendChatMessage("§fItems list is empty");
                  return;
               }

               for (String name : fentanyl.CLEANER.getList()) {
                  this.sendChatMessage("§a" + name);
               }

               return;
            case "add":
               if (parameters.length == 2) {
                  fentanyl.CLEANER.add(parameters[1]);
                  this.sendChatMessage("§f" + parameters[1] + (fentanyl.CLEANER.inList(parameters[1]) ? " §ahas been added" : " §chas been removed"));
                  return;
               }

               this.sendUsage();
               return;
            case "remove":
               if (parameters.length == 2) {
                  fentanyl.CLEANER.remove(parameters[1]);
                  this.sendChatMessage("§f" + parameters[1] + (fentanyl.CLEANER.inList(parameters[1]) ? " §ahas been added" : " §chas been removed"));
                  return;
               }

               this.sendUsage();
               return;
            default:
               if (parameters.length == 1) {
                  this.sendChatMessage("§f" + parameters[0] + (fentanyl.CLEANER.inList(parameters[0]) ? " §ais in whitelist" : " §cisn't in whitelist"));
               } else {
                  this.sendUsage();
               }
         }
      }
   }

   @Override
   public String[] getAutocorrect(int count, List<String> seperated) {
      if (count != 1) {
         return null;
      } else {
         String input = ((String)seperated.getLast()).toLowerCase();
         List<String> correct = new ArrayList();

         for (String x : List.of("add", "remove", "list", "reset", "clear")) {
            if (input.equalsIgnoreCase(fentanyl.getPrefix() + "cleaner") || x.toLowerCase().startsWith(input)) {
               correct.add(x);
            }
         }

         int numCmds = correct.size();
         String[] commands = new String[numCmds];
         int i = 0;

         for (String xx : correct) {
            commands[i++] = xx;
         }

         return commands;
      }
   }
}
