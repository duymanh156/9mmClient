package dev.ninemmteam.mod.commands.impl;

import dev.ninemmteam.fentanyl;
import dev.ninemmteam.core.Manager;
import dev.ninemmteam.core.impl.ConfigManager;
import dev.ninemmteam.mod.commands.Command;
import dev.ninemmteam.mod.modules.impl.client.Fonts;
import java.io.File;
import java.util.List;

public class LoadCommand extends Command {
   public LoadCommand() {
      super("load", "[config]");
   }

   @Override
   public void runCommand(String[] parameters) {
      if (parameters.length == 0) {
         this.sendUsage();
      } else {
         this.sendChatMessage("§fLoading..");
         ConfigManager.options = Manager.getFile("cfg" + File.separator + parameters[0] + ".cfg");
         fentanyl.CONFIG = new ConfigManager();
         fentanyl.CONFIG.load();
         ConfigManager.options = Manager.getFile("options.txt");
         fentanyl.save();
         Fonts.INSTANCE.refresh();
      }
   }

   @Override
   public String[] getAutocorrect(int count, List<String> seperated) {
      return null;
   }
}
