package dev.ninemmteam.mod.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.ninemmteam.api.events.eventbus.EventListener;
import dev.ninemmteam.api.events.impl.Render3DEvent;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.settings.impl.BooleanSetting;
import dev.ninemmteam.mod.modules.settings.impl.EnumSetting;
import dev.ninemmteam.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Hat extends Module {

    public static Hat INSTANCE;

    public final EnumSetting<CharacterType> character = this.add(new EnumSetting<>("Character", CharacterType.QUAN));
    public final BooleanSetting onlySelf = this.add(new BooleanSetting("OnlySelf", true));
    public final SliderSetting size = this.add(new SliderSetting("Scale", 0.8, 0.75, 1.0, 0.01));
    public final SliderSetting offsetX = this.add(new SliderSetting("OffsetX", 90.0, -180.0, 180.0, 1.0));
    public final SliderSetting offsetY = this.add(new SliderSetting("OffsetY", 5.0, -180.0, 180.0, 1.0));
    public final BooleanSetting dynamicFloat = this.add(new BooleanSetting("DynamicFloat", true));
    public final SliderSetting floatRange = this.add(new SliderSetting("FloatRange", 0.05, 0.01, 0.1, 0.01, () -> dynamicFloat.getValue()));
    public final SliderSetting floatSpeed = this.add(new SliderSetting("FloatSpeed", 0.1, 0.1, 2.0, 0.1, () -> dynamicFloat.getValue()));
    public final BooleanSetting firstPerson = this.add(new BooleanSetting("FirstPerson", true));
    public final BooleanSetting raiseReduceOpacity = this.add(new BooleanSetting("RaiseReduceOpacity", true, () -> firstPerson.getValue()));
    public final SliderSetting minOpacity = this.add(new SliderSetting("MinOpacity", 30.0, 0.0, 255.0, 1.0, () -> firstPerson.getValue() && raiseReduceOpacity.getValue()));
    public final BooleanSetting trackCamera = this.add(new BooleanSetting("TrackCamera", true));
    public final SliderSetting trackRange = this.add(new SliderSetting("TrackRange", 0.4, 0.35, 0.5, 0.01, () -> trackCamera.getValue()));
    public final SliderSetting positivePitchLimit = this.add(new SliderSetting("PositivePitchLimit", 30.0, 0.0, 30.0, 1.0, () -> trackCamera.getValue()));
    public final SliderSetting negativePitchLimit = this.add(new SliderSetting("NegativePitchLimit", -30.0, -30.0, 0.0, 1.0, () -> trackCamera.getValue()));

    // 纹理标识
    private static final Identifier QUAN_TEXTURE = Identifier.of("fentanyl", "textures/halo/quan.png");
    private static final Identifier CNM_TEXTURE = Identifier.of("fentanyl", "textures/halo/cnm.png");
   

    // 存储玩家眼睛高度
    private final Map<PlayerEntity, Double> prevEyeHeights = new HashMap<>();

    public Hat() {
        super("Hat", Category.Render);
        this.setChinese("光环");
        INSTANCE = this;
    }

    @EventListener
    public void onRender3D(Render3DEvent event) {
        if (!this.isOn() || mc.world == null || mc.player == null) return;

        // 获取要渲染的玩家列表 - 修复类型问题
        List<PlayerEntity> players;
        if (onlySelf.getValue()) {
            players = List.of(mc.player);
        } else {
            // 直接使用getPlayers()，然后转换为List<PlayerEntity>
            players = (List<PlayerEntity>) (List<?>) mc.world.getPlayers();
        }

        // 如果是第一人称且关闭了第一人称显示，移除自己
        if (!firstPerson.getValue() && players.contains(mc.player)) {
            if (mc.options.getPerspective().isFirstPerson()) {
                players = players.stream().filter(p -> p != mc.player).toList();
            }
        }

        float time = System.currentTimeMillis() * 0.001f;
        float floatY = 0;
        if (dynamicFloat.getValue()) {
            float freq = floatSpeed.getValueFloat();
            float amp = floatRange.getValueFloat();
            floatY = (float) (Math.sin(time * freq) * amp);
        }

        // 保存渲染状态
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.SRC_ALPHA,
                GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SrcFactor.ONE,
                GlStateManager.DstFactor.ZERO
        );
        RenderSystem.depthMask(false);

        // 获取当前角色的纹理
        Identifier texture = getCharacterTexture();
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);

        for (PlayerEntity player : players) {
            if (player.isSpectator() || !player.isAlive()) continue;

            renderPlayerHalo(event.matrixStack, event.tickDelta, player, texture, floatY);
        }

        // 恢复渲染状态
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private Identifier getCharacterTexture() {
        return switch (character.getValue()) {
            case QUAN -> QUAN_TEXTURE;
            case CNM -> CNM_TEXTURE;
        };
    }

    private void renderPlayerHalo(MatrixStack matrices, float tickDelta, PlayerEntity player, Identifier texture, float floatY) {
        // 计算插值位置
        double x = MathHelper.lerp(tickDelta, player.prevX, player.getX());
        double y = MathHelper.lerp(tickDelta, player.prevY, player.getY());
        double z = MathHelper.lerp(tickDelta, player.prevZ, player.getZ());

        // 判断是否是第一人称视角
        boolean isFirstPerson = player == mc.player && mc.options.getPerspective().isFirstPerson();

        // 计算基础Y位置
        double baseY;
        if (isFirstPerson) {
            // 第一人称：使用相机位置
            var camera = mc.gameRenderer.getCamera();
            var cameraPos = camera.getPos();
            baseY = cameraPos.y - y;
        } else {
            // 第三人称：使用眼睛高度
            double prevEyeHeight = prevEyeHeights.getOrDefault(player, (double) player.getEyeHeight(player.getPose()));
            double currentEyeHeight = player.getEyeHeight(player.getPose());
            double eyeHeight = MathHelper.lerp(tickDelta, prevEyeHeight, currentEyeHeight);
            baseY = eyeHeight;
            prevEyeHeights.put(player, currentEyeHeight);
        }

        // 计算透明度
        int alpha = 255;
        if (isFirstPerson && raiseReduceOpacity.getValue()) {
            float pitch = player.getPitch();
            if (pitch < -30f) {
                float t = MathHelper.clamp((pitch + 30f) / -60f, 0f, 1f);
                float min = minOpacity.getValueFloat();
                alpha = (int) (255 - (255 - min) * t);
            }
        }

        // 计算相机跟踪偏移
        double pitchOffsetX = 0, pitchOffsetY = 0, pitchOffsetZ = 0;
        if (trackCamera.getValue()) {
            float pitch = player.getPitch();
            pitch = MathHelper.clamp(pitch, negativePitchLimit.getValueFloat(), positivePitchLimit.getValueFloat());
            float pitchRad = (float) Math.toRadians(pitch);
            float yawRad = (float) Math.toRadians(player.getYaw());

            double radius = trackRange.getValue();
            pitchOffsetX = -Math.sin(yawRad) * Math.sin(pitchRad) * radius;
            pitchOffsetY = Math.cos(pitchRad) * radius;
            pitchOffsetZ = Math.cos(yawRad) * Math.sin(pitchRad) * radius;
        }

        // 计算最终位置
        double haloX = x + pitchOffsetX;
        double haloY = y + baseY + floatY + pitchOffsetY;
        double haloZ = z + pitchOffsetZ;

        // 获取相机位置
        var camera = mc.gameRenderer.getCamera();
        double cameraX = camera.getPos().x;
        double cameraY = camera.getPos().y;
        double cameraZ = camera.getPos().z;

        matrices.push();

        // 平移到光环位置（相对相机）
        matrices.translate(haloX - cameraX, haloY - cameraY, haloZ - cameraZ);

        // 应用缩放
        float scale = size.getValueFloat();
        matrices.scale(scale, scale, scale);

        // 相机跟踪旋转
        if (trackCamera.getValue()) {
            float yaw = player.getYaw();
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
        }

        // 应用偏移
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(offsetX.getValueFloat()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(offsetY.getValueFloat()));

        // 准备渲染
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float half = 0.5f;
        int color = (alpha << 24) | 0xFFFFFF; // 白色带透明度

        // 绘制四边形（Billboard效果）
        buffer.vertex(matrix, -half, -half, 0).texture(0, 1).color(color);
        buffer.vertex(matrix, -half, half, 0).texture(0, 0).color(color);
        buffer.vertex(matrix, half, half, 0).texture(1, 0).color(color);
        buffer.vertex(matrix, half, -half, 0).texture(1, 1).color(color);

        // 提交渲染
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        matrices.pop();
    }

    @Override
    public void onEnable() {
        prevEyeHeights.clear();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        prevEyeHeights.clear();
        super.onDisable();
    }

    // 角色类型枚举
    public enum CharacterType {
        QUAN("Quan"),
        CNM("Cnm");

        private final String displayName;

        CharacterType(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}