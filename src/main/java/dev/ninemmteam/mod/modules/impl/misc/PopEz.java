package dev.ninemmteam.mod.modules.impl.misc;

import dev.ninemmteam.fentanyl;
import dev.ninemmteam.api.events.eventbus.EventListener;
import dev.ninemmteam.api.events.impl.TotemEvent;
import dev.ninemmteam.api.events.impl.UpdateEvent;
import dev.ninemmteam.api.utils.math.Timer;
import dev.ninemmteam.mod.modules.Module;

import dev.ninemmteam.mod.modules.settings.impl.BooleanSetting;
import dev.ninemmteam.mod.modules.settings.impl.SliderSetting;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import net.minecraft.entity.player.PlayerEntity;

public class PopEz extends Module {

  public static PopEz INSTANCE;
  private final SliderSetting randomChars = add(new SliderSetting("RandomChars", 3, 0, 20, 1));
  public final BooleanSetting slowSend = add(new BooleanSetting("SlowSend", false));

  Random random = new Random();
  Timer timer = new Timer();
  private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
  private Map<Integer, Integer> popQueue = new HashMap<>(); // 存储玩家ID和需要发送的pop次数

  public PopEz() {
    super("PopEz", Category.Misc);
    setChinese("POP嘲讽");
    INSTANCE = this;
  }

  @EventListener
  public void onTotem(TotemEvent event) {
    PlayerEntity player = event.getPlayer();

    // 只处理敌人爆图腾的情况，忽略好友
    if (player != null && !player.equals(mc.player) && !fentanyl.FRIEND.isFriend(player)) {
      int l_Count = 1;
      if (fentanyl.POP.popContainer.containsKey(player.getName().getString())) {
        l_Count = fentanyl.POP.popContainer.get(player.getName().getString());
      }

      if (slowSend.getValue()) {
        // 慢发送模式：将所有pop添加到队列中
        popQueue.put(player.getId(), l_Count);
      } else {
        // 正常模式：立即发送消息（计数功能写死为开）
        String message;
        if (l_Count == 1) {
          message = player.getName().getString() + " has popped " + l_Count + " totem.";
        } else {
          message = player.getName().getString() + " has popped " + l_Count + " totems.";
        }

        sendMessage(message, player.getId());
      }
    }
  }

  @EventListener
  public void onUpdate(UpdateEvent event) {
    // 处理慢发送队列
    if (slowSend.getValue() && !popQueue.isEmpty() && timer.passedS(3.2)) {
      timer.reset();

      // 获取第一个玩家的pop信息
      Map.Entry<Integer, Integer> entry = popQueue.entrySet().iterator().next();
      int playerId = entry.getKey();
      int popCount = entry.getValue();

      // 查找对应的玩家
      PlayerEntity player = null;
      for (PlayerEntity p : mc.world.getPlayers()) {
        if (p.getId() == playerId) {
          player = p;
          break;
        }
      }

      if (player != null) {
        // 发送带有正确pop数量的消息（计数功能写死为开）
        String message;
        if (popCount == 1) {
          message = player.getName().getString() + " has popped " + popCount + " totem.";
        } else {
          message = player.getName().getString() + " has popped " + popCount + " totems.";
        }
        sendMessage(message, playerId);

        // 移除队列中的pop项，因为我们已经发送了完整的数量信息
        popQueue.remove(playerId);
      } else {
        // 如果找不到玩家，移除队列项
        popQueue.remove(playerId);
      }
    }
  }

  public void sendMessage(String message, int id) {
    if (!nullCheck() && mc.player != null && mc.player.networkHandler != null) {
      // 添加随机字符（如果启用）
      String randomString = generateRandomString(randomChars.getValueInt());
      if (!randomString.isEmpty()) {
        message = message + " " + randomString;
      }

      // 发送消息到聊天栏让所有人看到（使用纯文本避免非法字符）
      mc.player.networkHandler.sendChatMessage(message);
    }
  }

  private String generateRandomString(int length) {
    StringBuilder sb = new StringBuilder(length);

    for (int i = 0; i < length; i++) {
      int index = random.nextInt(CHARACTERS.length());
      sb.append(CHARACTERS.charAt(index));
    }

    return sb.toString();
  }
}