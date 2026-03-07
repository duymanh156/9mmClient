package dev.ninemmteam.mod.commands.impl;

import dev.ninemmteam.fentanyl;
import dev.ninemmteam.core.impl.PlayerManager;
import dev.ninemmteam.mod.commands.Command;
import dev.ninemmteam.mod.gui.windows.WindowsScreen;
import dev.ninemmteam.mod.gui.windows.impl.ItemSelectWindow;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.Items;

public class TradeCommand extends Command {
   public TradeCommand() {
      super("trade", "[\"\"/name/reset/clear/list] | [add/remove] [name]");
   }

   @Override
   public void runCommand(String[] parameters) {
      if (parameters.length == 0) {
         PlayerManager.screenToOpen = new WindowsScreen(new ItemSelectWindow(fentanyl.TRADE));
      } else {
         String var2 = parameters[0];
         switch (var2) {
            case "reset":
               fentanyl.TRADE.clear();
               fentanyl.TRADE.add(Items.ENCHANTED_BOOK.getTranslationKey());
               fentanyl.TRADE.add(Items.DIAMOND_BLOCK.getTranslationKey());
               this.sendChatMessage("§fItems list got reset");
               return;
            case "clear":
               fentanyl.TRADE.clear();
               this.sendChatMessage("§fItems list got clear");
               return;
            case "list":
               if (fentanyl.TRADE.getList().isEmpty()) {
                  this.sendChatMessage("§fItems list is empty");
                  return;
               }

               for (String name : fentanyl.TRADE.getList()) {
                  this.sendChatMessage("§a" + name);
               }

               return;
            case "add":
               if (parameters.length == 2) {
                  fentanyl.TRADE.add(parameters[1]);
                  this.sendChatMessage("§f" + parameters[1] + (fentanyl.TRADE.inWhitelist(parameters[1]) ? " §ahas been added" : " §chas been removed"));
                  return;
               }

               this.sendUsage();
               return;
            case "remove":
               if (parameters.length == 2) {
                  fentanyl.TRADE.remove(parameters[1]);
                  this.sendChatMessage("§f" + parameters[1] + (fentanyl.TRADE.inWhitelist(parameters[1]) ? " §ahas been added" : " §chas been removed"));
                  return;
               }

               this.sendUsage();
               return;
            default:
               if (parameters.length == 1) {
                  this.sendChatMessage("§f" + parameters[0] + (fentanyl.TRADE.inWhitelist(parameters[0]) ? " §ais in whitelist" : " §cisn't in whitelist"));
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
            if (input.equalsIgnoreCase(fentanyl.getPrefix() + "trade") || x.toLowerCase().startsWith(input)) {
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
