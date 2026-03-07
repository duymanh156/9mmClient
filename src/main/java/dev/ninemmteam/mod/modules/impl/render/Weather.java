package dev.ninemmteam.mod.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.settings.impl.ColorSetting;
import dev.ninemmteam.mod.modules.settings.impl.EnumSetting;
import dev.ninemmteam.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public class Weather extends Module {
    private static final Identifier RAIN = Identifier.of("minecraft", "textures/environment/rain.png");
    private static final Identifier SNOW = Identifier.of("minecraft", "textures/environment/snow.png");

    private final EnumSetting<WeatherMode> type = this.add(new EnumSetting<>("Type", WeatherMode.Rain));
    private final SliderSetting height = this.add(new SliderSetting("Height", 0.0, 0.0, 320.0, 1.0));
    private final SliderSetting strength = this.add(new SliderSetting("Strength", 0.8, 0.0, 5.0, 0.1));
    private final ColorSetting weatherColor = this.add(new ColorSetting("Color", new Color(200, 200, 255)));
    private final SliderSetting size = this.add(new SliderSetting("Size", 5.0, 1.0, 20.0, 1.0));
    private final SliderSetting speed = this.add(new SliderSetting("Speed", 1.0, 0.1, 5.0, 0.1));

    public Weather() {
        super("Weather", "Renders custom weather effects", Category.Render);
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        if (nullCheck()) return;

        WeatherMode mode = type.getValue();
        if (mode == WeatherMode.None) return;

        if (mode == WeatherMode.Both) {
            renderWeather(matrixStack, WeatherMode.Rain);
            renderWeather(matrixStack, WeatherMode.Snow);
        } else {
            renderWeather(matrixStack, mode);
        }
    }

    private void renderWeather(MatrixStack matrixStack, WeatherMode mode) {
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        double camX = cameraPos.x;
        double camY = cameraPos.y;
        double camZ = cameraPos.z;
        
        float tickDelta = mc.getRenderTickCounter().getTickDelta(true);
        int ticks = mc.player != null ? mc.player.age : 0;
        
        float r = weatherColor.getValue().getRed() / 255f;
        float g = weatherColor.getValue().getGreen() / 255f;
        float b = weatherColor.getValue().getBlue() / 255f;
        float a = strength.getValueFloat();
        
        int range = size.getValueInt();
        int camIntX = MathHelper.floor(camX);
        int camIntY = MathHelper.floor(camY);
        int camIntZ = MathHelper.floor(camZ);
        
        int minY = MathHelper.clamp((int) height.getValue(), 0, 320);
        int maxY = MathHelper.clamp(camIntY + range, minY, 320);
        
        LightmapTextureManager lightmap = mc.gameRenderer.getLightmapTextureManager();
        lightmap.enable();
        
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
            GlStateManager.SrcFactor.SRC_ALPHA,
            GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SrcFactor.ONE,
            GlStateManager.DstFactor.ZERO
        );
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        
        Identifier texture = mode == WeatherMode.Rain ? RAIN : SNOW;
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        World world = mc.world;
        if (world == null) return;
        
        for (int z = camIntZ - range; z <= camIntZ + range; z++) {
            for (int x = camIntX - range; x <= camIntX + range; x++) {
                double dx = x + 0.5 - camX;
                double dz = z + 0.5 - camZ;
                double dist = Math.sqrt(dx * dx + dz * dz);
                
                if (dist > range) continue;
                
                float alpha = (1f - (float)(dist / range)) * a;
                
                var random = net.minecraft.util.math.random.Random.create(
                    (long)(x * x * 3121 + x * 45238971L ^ z * z * 418711L + z * 13761L)
                );
                
                float speedMult = speed.getValueFloat();
                
                if (mode == WeatherMode.Rain) {
                    int seed = ticks + x * x * 3121 + x * 45238971 + z * z * 418711 + z * 13761 & 0x1F;
                    float texV = -((float)seed + tickDelta) / 32f * (3f + random.nextFloat());
                    
                    double offsetX = random.nextGaussian() * 0.3;
                    double offsetZ = random.nextGaussian() * 0.3;
                    
                    float x1 = (float)(x - camX + offsetX);
                    float z1 = (float)(z - camZ + offsetZ);
                    float y1 = (float)(maxY - camY);
                    float y2 = (float)(minY - camY);
                    
                    buffer.vertex(x1 - 0.5f, y1, z1 - 0.5f).texture(0, minY * 0.25f + texV).color(r, g, b, alpha);
                    buffer.vertex(x1 + 0.5f, y1, z1 + 0.5f).texture(1, minY * 0.25f + texV).color(r, g, b, alpha);
                    buffer.vertex(x1 + 0.5f, y2, z1 + 0.5f).texture(1, maxY * 0.25f + texV).color(r, g, b, alpha);
                    buffer.vertex(x1 - 0.5f, y2, z1 - 0.5f).texture(0, maxY * 0.25f + texV).color(r, g, b, alpha);
                } else {
                    float snowSpeed = -((float)(ticks & 0x1FF) + tickDelta) / 512f * speedMult;
                    float texOffset = (float)random.nextGaussian() * 0.1f;
                    
                    double offsetX = random.nextGaussian() * 0.3;
                    double offsetZ = random.nextGaussian() * 0.3;
                    
                    float x1 = (float)(x - camX + offsetX);
                    float z1 = (float)(z - camZ + offsetZ);
                    float y1 = (float)(maxY - camY);
                    float y2 = (float)(minY - camY);
                    
                    buffer.vertex(x1 - 0.5f, y1, z1 - 0.5f).texture(texOffset, minY * 0.25f + snowSpeed).color(r, g, b, alpha);
                    buffer.vertex(x1 + 0.5f, y1, z1 + 0.5f).texture(1 + texOffset, minY * 0.25f + snowSpeed).color(r, g, b, alpha);
                    buffer.vertex(x1 + 0.5f, y2, z1 + 0.5f).texture(1 + texOffset, maxY * 0.25f + snowSpeed).color(r, g, b, alpha);
                    buffer.vertex(x1 - 0.5f, y2, z1 - 0.5f).texture(texOffset, maxY * 0.25f + snowSpeed).color(r, g, b, alpha);
                }
            }
        }
        
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        lightmap.disable();
    }

    public enum WeatherMode {
        None,
        Rain,
        Snow,
        Both
    }
}
