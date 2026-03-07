package dev.ninemmteam.mod.modules.impl.render;

import dev.ninemmteam.api.utils.math.MathUtil;
import dev.ninemmteam.api.utils.render.ModelPlayer;
import dev.ninemmteam.core.impl.RotationManager;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.settings.impl.BooleanSetting;
import dev.ninemmteam.mod.modules.settings.impl.ColorSetting;
import dev.ninemmteam.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

public class DoubleVision extends Module {
    public static DoubleVision INSTANCE;
    
    private final BooleanSetting showReal = this.add(new BooleanSetting("ShowReal", true));
    private final ColorSetting realFill = this.add(new ColorSetting("RealFill", new Color(0, 255, 0, 80), () -> this.showReal.getValue()).injectBoolean(true));
    private final ColorSetting realLine = this.add(new ColorSetting("RealLine", new Color(0, 255, 0, 200), () -> this.showReal.getValue()).injectBoolean(true));
    
    private final BooleanSetting showServer = this.add(new BooleanSetting("ShowServer", true));
    private final ColorSetting serverFill = this.add(new ColorSetting("ServerFill", new Color(255, 0, 0, 80), () -> this.showServer.getValue()).injectBoolean(true));
    private final ColorSetting serverLine = this.add(new ColorSetting("ServerLine", new Color(255, 0, 0, 200), () -> this.showServer.getValue()).injectBoolean(true));
    
    private final SliderSetting lineWidth = this.add(new SliderSetting("LineWidth", 1.5, 0.5, 3.0, 0.1));
    private final BooleanSetting onlyOnRotate = this.add(new BooleanSetting("OnlyOnRotate", false));
    private final SliderSetting fadeTime = this.add(new SliderSetting("FadeTime", 0.5, 0.1, 2.0, 0.1, () -> this.onlyOnRotate.getValue()));
    
    private float lastRealYaw;
    private float lastRealPitch;
    private float lastServerYaw;
    private float lastServerPitch;
    private long lastRotateTime;
    
    private ModelPlayer realModel;
    private ModelPlayer serverModel;
    
    public DoubleVision() {
        super("DoubleVision", Module.Category.Render);
        this.setChinese("双重视觉");
        INSTANCE = this;
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        realModel = null;
        serverModel = null;
    }
    
    @Override
    public void onRender3D(MatrixStack matrixStack) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        
        if (onlyOnRotate.getValue()) {
            float currentRealYaw = mc.player.getYaw();
            float currentRealPitch = mc.player.getPitch();
            float currentServerYaw = RotationManager.getRotationYawHead();
            float currentServerPitch = RotationManager.getRenderPitch();
            
            if (Math.abs(currentRealYaw - lastRealYaw) > 0.1f || 
                Math.abs(currentRealPitch - lastRealPitch) > 0.1f ||
                Math.abs(currentServerYaw - lastServerYaw) > 0.1f || 
                Math.abs(currentServerPitch - lastServerPitch) > 0.1f) {
                lastRotateTime = System.currentTimeMillis();
            }
            
            lastRealYaw = currentRealYaw;
            lastRealPitch = currentRealPitch;
            lastServerYaw = currentServerYaw;
            lastServerPitch = currentServerPitch;
            
            if (System.currentTimeMillis() - lastRotateTime > fadeTime.getValueFloat() * 1000) {
                return;
            }
        }
        
        if (mc.options.getPerspective().isFirstPerson()) {
            return;
        }
        
        if (realModel == null) {
            realModel = new ModelPlayer(mc.player);
        }
        if (serverModel == null) {
            serverModel = new ModelPlayer(mc.player);
        }
        
        float tickDelta = mc.getRenderTickCounter().getTickDelta(true);
        
        float realYaw = MathHelper.lerpAngleDegrees(tickDelta, mc.player.prevYaw, mc.player.getYaw());
        float realPitch = MathHelper.lerpAngleDegrees(tickDelta, mc.player.prevPitch, mc.player.getPitch());
        float serverYaw = RotationManager.getRotationYawHead();
        float serverPitch = RotationManager.getRenderPitch();
        
        if (showReal.getValue()) {
            float savedYaw = mc.player.headYaw;
            float savedPitch = mc.player.getPitch();
            float savedBodyYaw = mc.player.bodyYaw;
            
            mc.player.headYaw = realYaw;
            mc.player.bodyYaw = realYaw;
            mc.player.setPitch(realPitch);
            
            realModel.render(matrixStack, realFill, realLine);
            
            mc.player.headYaw = savedYaw;
            mc.player.bodyYaw = savedBodyYaw;
            mc.player.setPitch(savedPitch);
        }
        
        if (showServer.getValue()) {
            float savedYaw = mc.player.headYaw;
            float savedPitch = mc.player.getPitch();
            float savedBodyYaw = mc.player.bodyYaw;
            
            mc.player.headYaw = serverYaw;
            mc.player.bodyYaw = serverYaw;
            mc.player.setPitch(serverPitch);
            
            serverModel.render(matrixStack, serverFill, serverLine);
            
            mc.player.headYaw = savedYaw;
            mc.player.bodyYaw = savedBodyYaw;
            mc.player.setPitch(savedPitch);
        }
    }
    
    @Override
    public String getInfo() {
        return null;
    }
}
