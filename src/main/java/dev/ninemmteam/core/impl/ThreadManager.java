package dev.ninemmteam.core.impl;


import com.google.common.collect.Lists;
import dev.ninemmteam.fentanyl;
import dev.ninemmteam.api.events.eventbus.EventListener;
import dev.ninemmteam.api.events.impl.InitEvent;
import dev.ninemmteam.api.events.impl.ClientTickEvent;
import dev.ninemmteam.api.events.impl.UpdateEvent;
import dev.ninemmteam.api.utils.Wrapper;
import dev.ninemmteam.api.utils.render.JelloUtil;
import dev.ninemmteam.api.utils.world.BlockUtil;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.impl.client.ClientSetting;
import dev.ninemmteam.mod.modules.impl.combat.AutoAnchor;
import dev.ninemmteam.mod.modules.impl.combat.AutoCrystal;
import dev.ninemmteam.mod.modules.impl.combat.CatAura;
import dev.ninemmteam.mod.modules.impl.render.ESP;
import dev.ninemmteam.mod.modules.impl.render.PlaceRender;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;

public class ThreadManager implements Wrapper {
   public static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
   public static ThreadManager.ClientService clientService;
   public volatile Iterable<Entity> threadSafeEntityList = Collections.emptyList();
   public volatile List<AbstractClientPlayerEntity> threadSafePlayersList = Collections.emptyList();
   public volatile boolean tickRunning = false;

   public ThreadManager() {
      this.init();
      fentanyl.EVENT_BUS.subscribe(new ThreadManager.HwidInitHandler());
   }

   
   public void init() {
      fentanyl.EVENT_BUS.subscribe(this);
      clientService = new ThreadManager.ClientService();
      clientService.setName("fentany1ClientService");
      clientService.setDaemon(true);
      clientService.start();
   }

   public Iterable<Entity> getEntities() {
      return this.threadSafeEntityList;
   }

   public List<AbstractClientPlayerEntity> getPlayers() {
      return this.threadSafePlayersList;
   }

   public void execute(Runnable runnable) {
      EXECUTOR.execute(runnable);
   }

   public static class HwidInitHandler {
      @EventListener
      public void onInit(InitEvent event) {
         fentanyl.EVENT_BUS.unsubscribe(this);
      }
   }

   @EventListener(priority = 200)
   public void onEvent(ClientTickEvent event) {
      fentanyl.POP.onUpdate();
      fentanyl.SERVER.onUpdate();
      if (event.isPre()) {
         JelloUtil.updateJello();
         this.tickRunning = true;
         BlockUtil.placedPos.forEach(pos -> PlaceRender.INSTANCE.create(pos));
         BlockUtil.placedPos.clear();
         fentanyl.PLAYER.onUpdate();
         if (!Module.nullCheck()) {
            fentanyl.EVENT_BUS.post(UpdateEvent.INSTANCE);
         }
      } else {
         this.tickRunning = false;
         if (mc.world == null || mc.player == null) {
            return;
         }

         this.threadSafeEntityList = Lists.newArrayList(mc.world.getEntities());
         this.threadSafePlayersList = Lists.newArrayList(mc.world.getPlayers());
      }

      if (!clientService.isAlive() || clientService.isInterrupted()) {
         clientService = new ThreadManager.ClientService();
         clientService.setName("fentany1Service");
         clientService.setDaemon(true);
         clientService.start();
      }
   }

   public class ClientService extends Thread {
      public void run() {
         while (true) {
            try {
               while (ThreadManager.this.tickRunning) {
                  Thread.onSpinWait();
               }

               AutoCrystal.INSTANCE.onThread();
               ESP.INSTANCE.onThread();
               AutoAnchor.INSTANCE.onThread();
            } catch (Exception var2) {
               var2.printStackTrace();
               if (ClientSetting.INSTANCE.debug.getValue()) {
                  CommandManager.sendMessage("§4An error has occurred [Thread] Message: [" + var2.getMessage() + "]");
               }
            }
         }
      }
   }
}
