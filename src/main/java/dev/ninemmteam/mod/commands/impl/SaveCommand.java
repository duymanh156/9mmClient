package dev.ninemmteam.mod.commands.impl;

import dev.ninemmteam.fentanyl;
import dev.ninemmteam.core.Manager;
import dev.ninemmteam.core.impl.ConfigManager;
import dev.ninemmteam.mod.commands.Command;
import java.io.File;
import java.util.List;

public class SaveCommand extends Command {
   public SaveCommand() {
      super("save", "");
   }

   @Override
   public void runCommand(String[] parameters) {
      if (parameters.length == 1) {
         this.sendChatMessage("§fSaving config named " + parameters[0]);
         File folder = new File(mc.runDirectory.getPath() + File.separator + "fent@nyl".toLowerCase() + File.separator + "cfg");
         if (!folder.exists()) {
            folder.mkdirs();
         }

         ConfigManager.options = Manager.getFile("cfg" + File.separator + parameters[0] + ".cfg");
         fentanyl.save();
         ConfigManager.options = Manager.getFile("options.txt");
      } else {
         this.sendChatMessage("§fSaving..");
      }

      fentanyl.save();
   }

   @Override
   public String[] getAutocorrect(int count, List<String> seperated) {
      return null;
   }
}
