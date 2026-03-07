package dev.ninemmteam.mod.commands.impl;

import dev.ninemmteam.mod.commands.Command;
import java.util.List;

public class GcCommand extends Command {
   public GcCommand() {
      super("gc", "");
   }

   @Override
   public void runCommand(String[] parameters) {
      System.gc();
   }

   @Override
   public String[] getAutocorrect(int count, List<String> seperated) {
      return null;
   }
}
