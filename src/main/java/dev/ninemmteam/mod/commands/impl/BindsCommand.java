package dev.ninemmteam.mod.commands.impl;

import dev.ninemmteam.fentanyl;
import dev.ninemmteam.mod.commands.Command;
import dev.ninemmteam.mod.modules.Module;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BindsCommand extends Command {
   public BindsCommand() {
      super("binds", "");
   }

   @Override
   public void runCommand(String[] parameters) {
      List<String> list = new ArrayList();

      for (Module x : fentanyl.MODULE.getModules()) {
         if (x.getBindSetting().getValue() != -1) {
            list.add("§f" + x.getDisplayName() + " §7- §r" + x.getBindSetting().getKeyString());
         }
      }

      Iterator<String> temp = list.iterator();
      int i = 0;
      StringBuilder string = new StringBuilder();

      while (temp.hasNext()) {
         if (i == 0) {
            string = new StringBuilder((String)temp.next());
         } else {
            string.append("§7, ").append((String)temp.next());
         }

         if (++i >= 3 || !temp.hasNext()) {
            this.sendChatMessage(string.toString());
            i = 0;
         }
      }
   }

   @Override
   public String[] getAutocorrect(int count, List<String> seperated) {
      return null;
   }
}
