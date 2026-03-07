package dev.ninemmteam.core.impl;


import dev.ninemmteam.fentanyl;
import dev.ninemmteam.api.interfaces.IChatHudHook;
import dev.ninemmteam.api.utils.Wrapper;
import dev.ninemmteam.mod.commands.Command;
import dev.ninemmteam.mod.commands.impl.AimCommand;
import dev.ninemmteam.mod.commands.impl.BindCommand;
import dev.ninemmteam.mod.commands.impl.BindsCommand;
import dev.ninemmteam.mod.commands.impl.CleanerCommand;
import dev.ninemmteam.mod.commands.impl.ClipCommand;
import dev.ninemmteam.mod.commands.impl.FakePlayerCommand;
import dev.ninemmteam.mod.commands.impl.FriendCommand;
import dev.ninemmteam.mod.commands.impl.GamemodeCommand;
import dev.ninemmteam.mod.commands.impl.GcCommand;
import dev.ninemmteam.mod.commands.impl.KitCommand;
import dev.ninemmteam.mod.commands.impl.LoadCommand;
import dev.ninemmteam.mod.commands.impl.PeekCommand;
import dev.ninemmteam.mod.commands.impl.PingCommand;
import dev.ninemmteam.mod.commands.impl.PrefixCommand;
import dev.ninemmteam.mod.commands.impl.RejoinCommand;
import dev.ninemmteam.mod.commands.impl.ReloadCommand;
import dev.ninemmteam.mod.commands.impl.SaveCommand;
import dev.ninemmteam.mod.commands.impl.TCommand;
import dev.ninemmteam.mod.commands.impl.TeleportCommand;
import dev.ninemmteam.mod.commands.impl.ToggleCommand;
import dev.ninemmteam.mod.commands.impl.TradeCommand;
import dev.ninemmteam.mod.commands.impl.WatermarkCommand;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.impl.client.ClientSetting;

import java.util.HashMap;
import net.minecraft.text.Text;

public class CommandManager implements Wrapper {
   private final HashMap<String, Command> commands = new HashMap();

   public CommandManager() {
      this.init();
   }

   
   public void init() {
      this.registerCommand(new AimCommand());
      this.registerCommand(new BindCommand());
      this.registerCommand(new BindsCommand());
      this.registerCommand(new CleanerCommand());
      this.registerCommand(new ClipCommand());
      this.registerCommand(new FakePlayerCommand());
      this.registerCommand(new FriendCommand());
      this.registerCommand(new GamemodeCommand());
      this.registerCommand(new KitCommand());
      this.registerCommand(new LoadCommand());
      this.registerCommand(new PingCommand());
      this.registerCommand(new PrefixCommand());
      this.registerCommand(new RejoinCommand());
      this.registerCommand(new ReloadCommand());
      this.registerCommand(new SaveCommand());
      this.registerCommand(new TeleportCommand());
      this.registerCommand(new TCommand());
      this.registerCommand(new ToggleCommand());
      this.registerCommand(new TradeCommand());
      this.registerCommand(new WatermarkCommand());
      this.registerCommand(new PeekCommand());
      this.registerCommand(new GcCommand());
   }

   public static void sendMessage(String message) {
      mc.execute(() -> {
         if (!Module.nullCheck()) {
            if (ClientSetting.INSTANCE.messageStyle.getValue() == ClientSetting.Style.Earth) {
               mc.inGameHud.getChatHud().addMessage(Text.of(message));
            } else if (ClientSetting.INSTANCE.messageStyle.getValue() == ClientSetting.Style.Moon) {
               mc.inGameHud.getChatHud().addMessage(Text.of("§b" + ClientSetting.INSTANCE.hackName.getValue() + "§f " + message));
            } else {
               ((IChatHudHook)mc.inGameHud.getChatHud()).fentany1Client$addMessage(Text.of(ClientSetting.INSTANCE.hackName.getValue() + "§f " + message));
            }
         }
      });
   }

   public static void sendMessageId(String message, int id) {
      mc.execute(
         () -> {
            if (!Module.nullCheck()) {
               if (ClientSetting.INSTANCE.messageStyle.getValue() == ClientSetting.Style.Earth) {
                  ((IChatHudHook)mc.inGameHud.getChatHud()).fentany1Client$addMessage(Text.of(message), id);
               } else if (ClientSetting.INSTANCE.messageStyle.getValue() == ClientSetting.Style.Moon) {
                  ((IChatHudHook)mc.inGameHud.getChatHud())
                     .fentany1Client$addMessage(Text.of("§b" + ClientSetting.INSTANCE.hackName.getValue() + "§f " + message), id);
               } else {
                  ((IChatHudHook)mc.inGameHud.getChatHud()).fentany1Client$addMessage(Text.of(ClientSetting.INSTANCE.hackName.getValue() + "§f " + message), id);
               }
            }
         }
      );
   }

   public static void sendChatMessageWidthIdNoSync(String message, int id) {
      mc.execute(() -> {
         if (!Module.nullCheck()) {
            ((IChatHudHook)mc.inGameHud.getChatHud()).fentany1Client$addMessageOutSync(Text.of("§f" + message), id);
         }
      });
   }

   private void registerCommand(Command command) {
      this.commands.put(command.getName(), command);
   }

   public Command getCommandBySyntax(String string) {
      return (Command)this.commands.get(string);
   }

   public HashMap<String, Command> getCommands() {
      return this.commands;
   }

   public void command(String[] commandIn) {
      Command command = (Command)this.commands.get(commandIn[0].substring(fentanyl.getPrefix().length()).toLowerCase());
      if (command == null) {
         sendMessage("§4Invalid Command!");
      } else {
         String[] parameterList = new String[commandIn.length - 1];
         System.arraycopy(commandIn, 1, parameterList, 0, commandIn.length - 1);
         if (parameterList.length == 1 && parameterList[0].equals("help")) {
            command.sendUsage();
            return;
         }

         command.runCommand(parameterList);
      }
   }
}
