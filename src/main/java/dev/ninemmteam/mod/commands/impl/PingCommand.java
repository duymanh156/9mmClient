package dev.ninemmteam.mod.commands.impl;

import dev.ninemmteam.fentanyl;
import dev.ninemmteam.api.events.eventbus.EventListener;
import dev.ninemmteam.api.events.impl.PacketEvent;
import dev.ninemmteam.mod.commands.Command;
import java.util.List;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

public class PingCommand extends Command {
   private long sendTime;

   public PingCommand() {
      super("ping", "");
   }

   @Override
   public void runCommand(String[] parameters) {
      this.sendTime = System.currentTimeMillis();
      mc.getNetworkHandler().sendChatCommand("chat ");
      fentanyl.EVENT_BUS.subscribe(this);
   }

   @Override
   public String[] getAutocorrect(int count, List<String> seperated) {
      return null;
   }

   @EventListener
   public void onPacketReceive(PacketEvent.Receive e) {
      if (e.getPacket() instanceof GameMessageS2CPacket packet
         && (
            packet.content().getString().contains("chat.use")
               || packet.content().getString().contains("命令")
               || packet.content().getString().contains("Bad command")
               || packet.content().getString().contains("No such command")
               || packet.content().getString().contains("<--[HERE]")
               || packet.content().getString().contains("Unknown")
               || packet.content().getString().contains("帮助")
               || packet.content().getString().contains("执行错误")
         )) {
         this.sendChatMessage("ping: " + (System.currentTimeMillis() - this.sendTime) + "ms");
         fentanyl.EVENT_BUS.unsubscribe(this);
      }
   }
}
