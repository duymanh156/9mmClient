package dev.ninemmteam.mod.commands.impl;

import dev.ninemmteam.fentanyl;
import dev.ninemmteam.core.impl.ConfigManager;
import dev.ninemmteam.mod.commands.Command;
import java.util.List;

public class ReloadCommand extends Command {
   public ReloadCommand() {
      super("reload", "");
   }

   @Override
   public void runCommand(String[] parameters) {
      this.sendChatMessage("§fReloading..");
      fentanyl.CONFIG = new ConfigManager();
      fentanyl.CONFIG.load();
      fentanyl.CLEANER.read();
      fentanyl.TRADE.read();
      fentanyl.FRIEND.read();
   }

   @Override
   public String[] getAutocorrect(int count, List<String> seperated) {
      return null;
   }
}
