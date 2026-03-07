package dev.ninemmteam;

import dev.ninemmteam.api.events.eventbus.EventBus;
import dev.ninemmteam.api.events.impl.InitEvent;
import dev.ninemmteam.auth.AuthClient;
import dev.ninemmteam.auth.AuthGUI;
import dev.ninemmteam.core.impl.BlurManager;
import dev.ninemmteam.core.impl.BreakManager;
import dev.ninemmteam.core.impl.CleanerManager;
import dev.ninemmteam.core.impl.CommandManager;
import dev.ninemmteam.core.impl.ConfigManager;
import dev.ninemmteam.core.impl.CrystalManager;
import dev.ninemmteam.core.impl.FPSManager;
import dev.ninemmteam.core.impl.FriendManager;
import dev.ninemmteam.core.impl.FriendMineTracker;
import dev.ninemmteam.core.impl.HoleManager;
import dev.ninemmteam.core.impl.ModuleManager;
import dev.ninemmteam.core.impl.NetworkManager;
import dev.ninemmteam.core.impl.PlayerManager;
import dev.ninemmteam.core.impl.PopManager;
import dev.ninemmteam.core.impl.RotationManager;
import dev.ninemmteam.core.impl.ServerManager;
import dev.ninemmteam.core.impl.ShaderManager;
import dev.ninemmteam.core.impl.ThreadManager;
import dev.ninemmteam.core.impl.TimerManager;
import dev.ninemmteam.core.impl.TradeManager;
import dev.ninemmteam.mod.modules.impl.client.ClientSetting;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.lang.invoke.MethodHandles;

public class fentanyl implements ModInitializer {
    public static final String NAME = "fent@nyl";
    public static final String VERSION = "b1.6.1";
    public static final boolean nightly = true;
    public static final EventBus EVENT_BUS = new EventBus();
    public static HoleManager HOLE;
   public static PlayerManager PLAYER;
   public static TradeManager TRADE;
   public static CleanerManager CLEANER;
   public static ModuleManager MODULE;
   public static CommandManager COMMAND;
   public static ConfigManager CONFIG;
   public static RotationManager ROTATION;
   public static BreakManager BREAK;
   public static PopManager POP;
   public static FriendManager FRIEND;
   public static FriendMineTracker FRIEND_MINE_TRACKER;
   public static TimerManager TIMER;
   public static ShaderManager SHADER;
   public static BlurManager BLUR;
   public static FPSManager FPS;
   public static ServerManager SERVER;
   public static NetworkManager NETWORK;
   public static ThreadManager THREAD;
    public static boolean loaded;
    public static long initTime;
    public static boolean mioLoaded = false;
    public static boolean irisLoaded = false;

    public static String getPrefix() {
        return ClientSetting.INSTANCE.prefix.getValue();
    }

    public static void save() {
        System.out.println("[" + NAME + "]" + " Saving");
        CONFIG.save();
        CLEANER.save();
        FRIEND.save();
        TRADE.save();
        System.out.println("[" + NAME + "]" + " Saved");
    }

    @Override
    public void onInitialize() {
        if (!doAuth()) {
            System.exit(0);
            return;
        }
        checkCompatibility();
        load();
    }
    
    private boolean doAuth() {
        AuthClient.AuthResult localAuth = AuthClient.checkLocalAuth();
        if (localAuth.success()) {
            System.out.println("[" + NAME + "] 本地验证成功，跳过登录窗口");
            return true;
        }
        
        System.out.println("[" + NAME + "] 本地验证失败，显示登录窗口");
        AuthGUI authGUI = new AuthGUI();
        return authGUI.showAndWait();
    }

    private void checkCompatibility() {
        FabricLoader loader = FabricLoader.getInstance();
        
        mioLoaded = loader.isModLoaded("mioloader");
        irisLoaded = loader.isModLoaded("iris");
        
        if (mioLoaded && irisLoaded) {
            System.err.println("[" + NAME + "] " + "§c========================================");
            System.err.println("[" + NAME + "] " + "§cWARNING: Incompatible mods detected!");
            System.err.println("[" + NAME + "] " + "§cMioClient and Iris are NOT compatible!");
            System.err.println("[" + NAME + "] " + "§cThis will cause a crash during texture loading.");
            System.err.println("[" + NAME + "] " + "§cPlease remove one of them to prevent crashes.");
            System.err.println("[" + NAME + "] " + "§c========================================");
        }
    }

    private void load() {
      this.register();
      CONFIG = new ConfigManager();
      HOLE = new HoleManager();
      MODULE = new ModuleManager();
      COMMAND = new CommandManager();
      FRIEND = new FriendManager();
      FRIEND_MINE_TRACKER = new FriendMineTracker();
      CLEANER = new CleanerManager();
      TRADE = new TradeManager();
      ROTATION = new RotationManager();
      BREAK = new BreakManager();
      PLAYER = new PlayerManager();
      POP = new PopManager();
      TIMER = new TimerManager();
      SHADER = new ShaderManager();
      BLUR = new BlurManager();
      FPS = new FPSManager();
      SERVER = new ServerManager();
      NETWORK = new NetworkManager();
      THREAD = new ThreadManager();
      new CrystalManager();
      CONFIG.load();
      initTime = System.currentTimeMillis();
      loaded = true;
      EVENT_BUS.post(new InitEvent());

      File folder = new File(MinecraftClient.getInstance().runDirectory.getPath() + File.separator + "fent@nyl".toLowerCase() + File.separator + "cfg");
      if (!folder.exists()) {
         folder.mkdirs();
      }
   }

    private void register() {
        EVENT_BUS.registerLambdaFactory((lookupInMethod, klass) -> (MethodHandles.Lookup)lookupInMethod.invoke(null, klass, MethodHandles.lookup()));
        EVENT_BUS.subscribe(this);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (loaded) {
                save();
            }
        }));
    }
}
