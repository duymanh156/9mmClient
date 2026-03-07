package dev.ninemmteam.mod.commands.impl;

import dev.ninemmteam.fentanyl;
import dev.ninemmteam.mod.commands.Command;
import dev.ninemmteam.mod.modules.Module;
import java.util.ArrayList;
import java.util.List;

public class TCommand extends Command {
   public TCommand() {
      super("t", "[module]");
   }

   @Override
   public void runCommand(String[] parameters) {
      if (parameters.length == 0) {
         this.sendUsage();
      } else {
         String moduleName = parameters[0];
         Module module = fentanyl.MODULE.getModuleByName(moduleName);
         if (module == null) {
            this.sendChatMessage("§fUnknown module!");
         } else {
            module.toggle();
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

         for (Module x : fentanyl.MODULE.getModules()) {
            if (input.equalsIgnoreCase(fentanyl.getPrefix() + "t") || x.getName().toLowerCase().startsWith(input)) {
               correct.add(x.getName());
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
