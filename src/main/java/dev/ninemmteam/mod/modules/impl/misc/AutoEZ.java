package dev.ninemmteam.mod.modules.impl.misc;

import dev.ninemmteam.fentanyl;
import dev.ninemmteam.api.events.eventbus.EventListener;
import dev.ninemmteam.api.events.impl.DeathEvent;
import dev.ninemmteam.core.Manager;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.settings.impl.EnumSetting;
import dev.ninemmteam.mod.modules.settings.impl.SliderSetting;
import dev.ninemmteam.mod.modules.settings.impl.StringSetting;
import dev.ninemmteam.mod.modules.settings.impl.BooleanSetting;
import java.util.List;
import java.util.Random;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import net.minecraft.entity.player.PlayerEntity;

public class AutoEZ extends Module {
   private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
   public final List<String> sex = List.of(
      "",
      "什麼時候剪辮子？",
      "東北狗",
      "731nb666",
      "廣東狗",
      "ch1ng9ch0ng",
      "秦腔窮",
      "天天種地嗎？鋤禾日當午？",
      "大陸人吃貓肉狗肉"
   );
   public final List<String> bot = List.of(
      "大陸人吃貓肉狗肉",
      "天天種地嗎？鋤禾日當午？",
      "秦腔窮",
      "廣東狗",
      "廣東狗",
      "東北狗",
      "731nb666",
      "731nb666",
      "什麼時候剪辮子？"
   );
   private final EnumSetting<AutoEZ.Type> type = this.add(new EnumSetting("Type", AutoEZ.Type.Bot));
   final StringSetting message = this.add(new StringSetting("Message", "EZ %player%", () -> this.type.getValue() == AutoEZ.Type.Custom));
   final StringSetting fileName = this.add(new StringSetting("FileName", "autoez", () -> this.type.getValue() == AutoEZ.Type.File));
   final BooleanSetting randomFromFile = this.add(new BooleanSetting("RandomFromFile", true, () -> this.type.getValue() == AutoEZ.Type.File));
   final Random random = new Random();
   private final SliderSetting range = this.add(new SliderSetting("Range", 10.0, 0.0, 20.0, 0.1));
   private final SliderSetting randoms = this.add(new SliderSetting("Random", 3.0, 0.0, 20.0, 1.0));
   private int currentTauntIndex = 0;

   public AutoEZ() {
      super("AutoEZ", Module.Category.Misc);
      this.setChinese("自动嘲讽");
   }

   @EventListener
   public void onDeath(DeathEvent event) {
      PlayerEntity player = event.getPlayer();
      if (player != mc.player && !fentanyl.FRIEND.isFriend(player)) {
         if (this.range.getValue() > 0.0 && mc.player.distanceTo(player) > this.range.getValue()) {
            return;
         }

         String randomString = this.generateRandomString(this.randoms.getValueInt());
         if (!randomString.isEmpty()) {
            randomString = " " + randomString;
         }

         switch ((AutoEZ.Type)this.type.getValue()) {
            case Bot:
               mc.getNetworkHandler()
                  .sendChatMessage((String)this.bot.get(this.random.nextInt(this.bot.size() - 1)) + " " + player.getName().getString() + randomString);
               break;
            case Custom:
               mc.getNetworkHandler().sendChatMessage(this.message.getValue().replaceAll("%player%", player.getName().getString()) + randomString);
               break;
            case AutoSex:
               mc.getNetworkHandler()
                  .sendChatMessage((String)this.sex.get(this.random.nextInt(this.sex.size() - 1)) + " " + player.getName().getString() + randomString);
               break;
            case File:
               List<String> fileTaunts = this.readTauntsFromFile();
               String taunt;
               if (this.randomFromFile.getValue()) {
                  taunt = fileTaunts.get(this.random.nextInt(fileTaunts.size()));
               } else {
                  taunt = fileTaunts.get(this.currentTauntIndex);
                  this.currentTauntIndex = (this.currentTauntIndex + 1) % fileTaunts.size();
               }
               mc.getNetworkHandler().sendChatMessage(taunt.replaceAll("%player%", player.getName().getString()) + randomString);
         }
      }
   }

   private List<String> readTauntsFromFile() {
      List<String> taunts = new ArrayList<>();
      
      try {
         File autoezDir = new File(Manager.getFolder(), "autoez");
         
         if (!autoezDir.exists()) {
            autoezDir.mkdirs();
         }
         
         File tauntFile = new File(autoezDir, this.fileName.getValue() + ".txt");
         
         if (tauntFile.exists()) {
            Path filePath = Paths.get(tauntFile.getAbsolutePath());
            List<String> lines = Files.readAllLines(filePath);
            
            for (String line : lines) {
               if (!line.trim().isEmpty()) {
                  taunts.add(line.trim());
               }
            }
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
      
      if (taunts.isEmpty()) {
         taunts.add("EZ %player%");
      }
      
      return taunts;
   }

   private String generateRandomString(int LENGTH) {
      StringBuilder sb = new StringBuilder(LENGTH);

      for (int i = 0; i < LENGTH; i++) {
         int index = this.random.nextInt("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".length());
         sb.append("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".charAt(index));
      }

      return sb.toString();
   }

   public static enum Type {
      Bot,
      Custom,
      AutoSex,
      File;
   }
}
