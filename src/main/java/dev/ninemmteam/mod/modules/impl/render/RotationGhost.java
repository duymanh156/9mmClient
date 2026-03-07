package dev.ninemmteam.mod.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.ninemmteam.api.utils.math.MathUtil;
import dev.ninemmteam.core.impl.RotationManager;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.settings.impl.BooleanSetting;
import dev.ninemmteam.mod.modules.settings.impl.ColorSetting;
import dev.ninemmteam.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class RotationGhost extends Module {
    public static RotationGhost INSTANCE;
    
    private final BooleanSetting showReal = this.add(new BooleanSetting("ShowReal", true));
    private final ColorSetting realColor = this.add(new ColorSetting("RealColor", new Color(0, 255, 0, 180), () -> this.showReal.getValue()));
    private final BooleanSetting showServer = this.add(new BooleanSetting("ShowServer", true));
    private final ColorSetting serverColor = this.add(new ColorSetting("ServerColor", new Color(255, 0, 0, 180), () -> this.showServer.getValue()));
    private final BooleanSetting showLine = this.add(new BooleanSetting("ShowLine", true));
    private final ColorSetting lineColor = this.add(new ColorSetting("LineColor", new Color(255, 255, 0, 120), () -> this.showLine.getValue()));
    private final SliderSetting length = this.add(new SliderSetting("Length", 1.0, 0.1, 3.0, 0.1));
    private final SliderSetting lineWidth = this.add(new SliderSetting("LineWidth", 2.0, 1.0, 5.0, 0.5));
    private final SliderSetting headOffset = this.add(new SliderSetting("HeadOffset", 1.62, 1.0, 2.0, 0.01));
    private final BooleanSetting showOnlyOnRotate = this.add(new BooleanSetting("OnlyOnRotate", false));
    private final SliderSetting fadeTime = this.add(new SliderSetting("FadeTime", 0.5, 0.1, 2.0, 0.1, () -> this.showOnlyOnRotate.getValue()));
    
    private float lastRealYaw;
    private float lastRealPitch;
    private float lastServerYaw;
    private float lastServerPitch;
    private long lastRotateTime;
    
    public RotationGhost() {
        super("RotationGhost", Module.Category.Render);
        this.setChinese("旋转重影");
        INSTANCE = this;
    }
    
    @Override
    public void onRender3D(MatrixStack matrixStack) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        
        if (this.showOnlyOnRotate.getValue()) {
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
            
            if (System.currentTimeMillis() - lastRotateTime > this.fadeTime.getValueFloat() * 1000) {
                return;
            }
        }
        
        float tickDelta = mc.getRenderTickCounter().getTickDelta(true);
        
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        Vec3d playerPos = MathUtil.getRenderPosition(mc.player, tickDelta);
        double eyeHeight = this.headOffset.getValue();
        
        Vec3d eyePos = new Vec3d(playerPos.x, playerPos.y + eyeHeight, playerPos.z);
        Vec3d eyePosRelative = eyePos.subtract(cameraPos);
        
        float realYaw = MathHelper.lerpAngleDegrees(tickDelta, mc.player.prevYaw, mc.player.getYaw());
        float realPitch = MathHelper.lerpAngleDegrees(tickDelta, mc.player.prevPitch, mc.player.getPitch());
        float serverYaw = RotationManager.getRotationYawHead();
        float serverPitch = RotationManager.getRenderPitch();
        
        float arrowLength = this.length.getValueFloat();
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.lineWidth((float) this.lineWidth.getValue());
        
        Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();
        
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        
        if (this.showReal.getValue()) {
            Color c = this.realColor.getValue();
            float r = c.getRed() / 255.0f;
            float g = c.getGreen() / 255.0f;
            float b = c.getBlue() / 255.0f;
            float a = c.getAlpha() / 255.0f;
            
            Vec3d realDir = getDirectionVector(realYaw, realPitch);
            Vec3d realEnd = eyePosRelative.add(realDir.multiply(arrowLength));
            
            bufferBuilder.vertex(matrix4f, (float) eyePosRelative.x, (float) eyePosRelative.y, (float) eyePosRelative.z).color(r, g, b, a);
            bufferBuilder.vertex(matrix4f, (float) realEnd.x, (float) realEnd.y, (float) realEnd.z).color(r, g, b, a);
        }
        
        if (this.showServer.getValue()) {
            Color c = this.serverColor.getValue();
            float r = c.getRed() / 255.0f;
            float g = c.getGreen() / 255.0f;
            float b = c.getBlue() / 255.0f;
            float a = c.getAlpha() / 255.0f;
            
            Vec3d serverDir = getDirectionVector(serverYaw, serverPitch);
            Vec3d serverEnd = eyePosRelative.add(serverDir.multiply(arrowLength));
            
            bufferBuilder.vertex(matrix4f, (float) eyePosRelative.x, (float) eyePosRelative.y, (float) eyePosRelative.z).color(r, g, b, a);
            bufferBuilder.vertex(matrix4f, (float) serverEnd.x, (float) serverEnd.y, (float) serverEnd.z).color(r, g, b, a);
        }
        
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        
        if (this.showLine.getValue() && this.showReal.getValue() && this.showServer.getValue()) {
            Color c = this.lineColor.getValue();
            float r = c.getRed() / 255.0f;
            float g = c.getGreen() / 255.0f;
            float b = c.getBlue() / 255.0f;
            float a = c.getAlpha() / 255.0f;
            
            Vec3d realDir = getDirectionVector(realYaw, realPitch);
            Vec3d serverDir = getDirectionVector(serverYaw, serverPitch);
            Vec3d realEnd = eyePosRelative.add(realDir.multiply(arrowLength));
            Vec3d serverEnd = eyePosRelative.add(serverDir.multiply(arrowLength));
            
            BufferBuilder lineBuffer = Tessellator.getInstance().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            lineBuffer.vertex(matrix4f, (float) realEnd.x, (float) realEnd.y, (float) realEnd.z).color(r, g, b, a);
            lineBuffer.vertex(matrix4f, (float) serverEnd.x, (float) serverEnd.y, (float) serverEnd.z).color(r, g, b, a);
            BufferRenderer.drawWithGlobalProgram(lineBuffer.end());
        }
        
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
    
    private Vec3d getDirectionVector(float yaw, float pitch) {
        float yawRad = (float) Math.toRadians(-yaw);
        float pitchRad = (float) Math.toRadians(-pitch);
        
        double x = Math.sin(yawRad) * Math.cos(pitchRad);
        double y = Math.sin(pitchRad);
        double z = Math.cos(yawRad) * Math.cos(pitchRad);
        
        return new Vec3d(x, y, z);
    }
    
    @Override
    public String getInfo() {
        return null;
    }
}
