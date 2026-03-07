package dev.ninemmteam.api.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.ninemmteam.api.utils.Wrapper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector4d;

import java.awt.*;

public class EntityVisualUtil implements Wrapper {

    // 默认图片标识符
    private static final Identifier DEFAULT_TEXTURE = Identifier.of("fentanyl", "textures/rectangle.png");

    /**
     * 图片视觉配置类
     */
    public static class ImageVisualConfig {
        public Identifier texture = DEFAULT_TEXTURE;
        public float scale = 1.0f;
        public float opacity = 1.0f;
        public Color color = Color.WHITE;
        public boolean enableTint = false;
        public float redTint = 1.0f;
        public float greenTint = 1.0f;
        public float blueTint = 1.0f;
        public boolean colorCycle = false;
        public float cycleSpeed = 1.0f;
        public float xOffset = 0.0f;
        public float yOffset = 0.0f;
        public boolean enableRotation = false;
        public float rotationSpeed = 1.0f;
        public RotationDirection rotationDirection = RotationDirection.CLOCKWISE;
        public float rotationAngle = 0.0f;
        public long rotationStartTime = System.currentTimeMillis();

        // 创建默认配置
        public static ImageVisualConfig createDefault() {
            return new ImageVisualConfig();
        }

        // 创建高亮目标配置
        public static ImageVisualConfig createTargetHighlight() {
            ImageVisualConfig config = new ImageVisualConfig();
            config.color = new Color(255, 50, 50, 200);
            config.scale = 1.2f;
            config.enableRotation = true;
            config.rotationSpeed = 0.25f;
            config.colorCycle = true;
            config.cycleSpeed = 2.0f;
            return config;
        }

        // 创建简单方框配置
        public static ImageVisualConfig createSimpleBox() {
            ImageVisualConfig config = new ImageVisualConfig();
            config.scale = 0.8f;
            config.opacity = 0.7f;
            config.color = new Color(255, 255, 255, 180);
            return config;
        }
    }

    /**
     * 在实体上绘制图片视觉
     * @param context DrawContext
     * @param entity 目标实体
     * @param config 视觉配置
     */
    public static void drawImageOnEntity(DrawContext context, Entity entity, ImageVisualConfig config) {
        if (entity == null || config == null || mc.world == null || mc.player == null) return;

        // 获取实体在屏幕上的位置
        Vector4d screenPos = getEntityScreenPosition(entity);
        if (screenPos == null) return;

        // 计算中心点和大小
        double centerX = screenPos.x + (screenPos.z - screenPos.x) / 2.0;
        double centerY = screenPos.y + (screenPos.w - screenPos.y) / 2.0;

        double entityWidth = screenPos.z - screenPos.x;
        double entityHeight = screenPos.w - screenPos.y;

        // 基于实体大小计算图片大小
        float baseSize = (float) Math.max(entityWidth, entityHeight);
        float scaledSize = baseSize * config.scale;

        // 计算绘制位置
        float imageX = (float) (centerX - scaledSize / 2.0 + config.xOffset);
        float imageY = (float) (centerY - scaledSize / 2.0 + config.yOffset);

        // 准备渲染
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // 计算颜色
        float red, green, blue, alpha = config.opacity;

        if (config.enableTint) {
            if (config.colorCycle) {
                // 动态颜色循环
                long currentTime = System.currentTimeMillis();
                float cycleProgress = (float) ((currentTime * config.cycleSpeed * 0.001) % 360.0);

                red = (float) (0.5 + 0.5 * Math.sin(Math.toRadians(cycleProgress)));
                green = (float) (0.5 + 0.5 * Math.sin(Math.toRadians(cycleProgress + 120.0)));
                blue = (float) (0.5 + 0.5 * Math.sin(Math.toRadians(cycleProgress + 240.0)));
            } else {
                // 静态颜色调节
                red = config.redTint;
                green = config.greenTint;
                blue = config.blueTint;
            }
        } else {
            // 使用预设颜色
            red = config.color.getRed() / 255.0f;
            green = config.color.getGreen() / 255.0f;
            blue = config.color.getBlue() / 255.0f;
        }

        // 设置颜色
        RenderSystem.setShaderColor(red, green, blue, alpha);

        // 计算旋转角度
        // 在 EntityVisualUtil.java 的 drawImageOnEntity 方法中，修改旋转计算：
        float rotationAngle = 0.0f;
        if (config.enableRotation) {
            // 修复：使用更合理的系数
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - config.rotationStartTime;

            // 将毫秒转换为秒，再计算角度
            float elapsedSeconds = elapsedTime / 1000.0f;

            // 旋转速度 = 度/秒
            rotationAngle = (elapsedSeconds * config.rotationSpeed * 360.0f) % 360.0f;

            // 或者使用这个更简单的公式：
            // rotationAngle = (float) ((System.currentTimeMillis() / 1000.0 * config.rotationSpeed * 360.0) % 360.0);

            if (config.rotationDirection == RotationDirection.COUNTER_CLOCKWISE) {
                rotationAngle = -rotationAngle;
            }
        } else {
            // 静态角度
            rotationAngle = config.rotationAngle;
        }

        // 保存矩阵状态并应用变换
        context.getMatrices().push();

        // 移动到中心并旋转
        context.getMatrices().translate(imageX + scaledSize / 2.0, imageY + scaledSize / 2.0, 0);

        if (Math.abs(rotationAngle) > 0.001f) {
            context.getMatrices().multiply(new Quaternionf().rotateZ((float) Math.toRadians(rotationAngle)));
        }

        context.getMatrices().translate(-(imageX + scaledSize / 2.0), -(imageY + scaledSize / 2.0), 0);

        // 绘制图片
        context.drawTexture(
                config.texture,
                (int) imageX,
                (int) imageY,
                0,
                0,
                (int) scaledSize,
                (int) scaledSize,
                (int) scaledSize,
                (int) scaledSize
        );

        // 恢复矩阵状态
        context.getMatrices().pop();

        // 重置颜色
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    /**
     * 在实体上绘制简单的方框（无需图片资源）
     * @param context DrawContext
     * @param entity 目标实体
     * @param color 方框颜色
     * @param thickness 线条粗细
     * @param scale 缩放比例
     */
    public static void drawSimpleBoxOnEntity(DrawContext context, Entity entity, Color color,
                                             float thickness, float scale) {
        if (entity == null || mc.world == null || mc.player == null) return;

        Vector4d screenPos = getEntityScreenPosition(entity);
        if (screenPos == null) return;

        float x = (float) screenPos.x;
        float y = (float) screenPos.y;
        float width = (float) (screenPos.z - screenPos.x) * scale;
        float height = (float) (screenPos.w - screenPos.y) * scale;

        // 计算偏移使方框居中
        float offsetX = ((float) (screenPos.z - screenPos.x) - width) / 2.0f;
        float offsetY = ((float) (screenPos.w - screenPos.y) - height) / 2.0f;

        x += offsetX;
        y += offsetY;

        // 绘制四条边
        drawRect(context, x - thickness, y, width + thickness * 2, thickness, color); // 上边
        drawRect(context, x - thickness, y + height, width + thickness * 2, thickness, color); // 下边
        drawRect(context, x - thickness, y, thickness, height, color); // 左边
        drawRect(context, x + width, y, thickness, height, color); // 右边
    }

    /**
     * 在实体上绘制十字准星
     * @param context DrawContext
     * @param entity 目标实体
     * @param color 颜色
     * @param size 大小
     * @param thickness 粗细
     */
    public static void drawCrosshairOnEntity(DrawContext context, Entity entity, Color color,
                                             float size, float thickness) {
        if (entity == null || mc.world == null || mc.player == null) return;

        Vector4d screenPos = getEntityScreenPosition(entity);
        if (screenPos == null) return;

        double centerX = screenPos.x + (screenPos.z - screenPos.x) / 2.0;
        double centerY = screenPos.y + (screenPos.w - screenPos.y) / 2.0;

        float halfSize = size / 2.0f;

        // 绘制水平线
        drawRect(context,
                (float) centerX - halfSize,
                (float) centerY - thickness / 2,
                size,
                thickness,
                color);

        // 绘制垂直线
        drawRect(context,
                (float) centerX - thickness / 2,
                (float) centerY - halfSize,
                thickness,
                size,
                color);
    }

    /**
     * 获取实体在屏幕上的位置（修复版本）
     */
    private static Vector4d getEntityScreenPosition(Entity entity) {
        if (entity == null || mc.world == null || mc.player == null) return null;

        // 修复：正确获取部分刻
        float partialTicks = mc.getRenderTickCounter().getTickDelta(true);

        // 计算插值位置
        double x = entity.prevX + (entity.getX() - entity.prevX) * partialTicks;
        double y = entity.prevY + (entity.getY() - entity.prevY) * partialTicks;
        double z = entity.prevZ + (entity.getZ() - entity.prevZ) * partialTicks;

        Box entityBox = entity.getBoundingBox();
        Box adjustedBox = new Box(
                entityBox.minX - entity.getX() + x - 0.05,
                entityBox.minY - entity.getY() + y,
                entityBox.minZ - entity.getZ() + z - 0.05,
                entityBox.maxX - entity.getX() + x + 0.05,
                entityBox.maxY - entity.getY() + y + 0.15,
                entityBox.maxZ - entity.getZ() + z + 0.05
        );

        // 获取包围盒的8个顶点
        Vec3d[] vertices = {
                new Vec3d(adjustedBox.minX, adjustedBox.minY, adjustedBox.minZ),
                new Vec3d(adjustedBox.minX, adjustedBox.maxY, adjustedBox.minZ),
                new Vec3d(adjustedBox.maxX, adjustedBox.minY, adjustedBox.minZ),
                new Vec3d(adjustedBox.maxX, adjustedBox.maxY, adjustedBox.minZ),
                new Vec3d(adjustedBox.minX, adjustedBox.minY, adjustedBox.maxZ),
                new Vec3d(adjustedBox.minX, adjustedBox.maxY, adjustedBox.maxZ),
                new Vec3d(adjustedBox.maxX, adjustedBox.minY, adjustedBox.maxZ),
                new Vec3d(adjustedBox.maxX, adjustedBox.maxY, adjustedBox.maxZ)
        };

        Vector4d position = null;

        for (Vec3d vertex : vertices) {
            // 转换到屏幕空间
            Vec3d screenPos = TextUtil.worldSpaceToScreenSpace(vertex);
            if (screenPos == null || screenPos.z <= 0.0 || screenPos.z >= 1.0) continue;

            if (position == null) {
                position = new Vector4d(screenPos.x, screenPos.y, screenPos.x, screenPos.y);
            } else {
                position.x = Math.min(screenPos.x, position.x);
                position.y = Math.min(screenPos.y, position.y);
                position.z = Math.max(screenPos.x, position.z);
                position.w = Math.max(screenPos.y, position.w);
            }
        }

        return position;
    }

    /**
     * 简化的获取实体屏幕位置方法（用于2D渲染）
     * @param entity 实体
     * @param tickDelta 部分刻
     * @return 屏幕位置
     */
    public static Vector4d getEntityScreenPosition(Entity entity, float tickDelta) {
        if (entity == null || mc.world == null || mc.player == null) return null;

        double x = entity.prevX + (entity.getX() - entity.prevX) * tickDelta;
        double y = entity.prevY + (entity.getY() - entity.prevY) * tickDelta;
        double z = entity.prevZ + (entity.getZ() - entity.prevZ) * tickDelta;

        Box axisAlignedBB2 = entity.getBoundingBox();
        Box axisAlignedBB = new Box(
                axisAlignedBB2.minX - entity.getX() + x - 0.05,
                axisAlignedBB2.minY - entity.getY() + y,
                axisAlignedBB2.minZ - entity.getZ() + z - 0.05,
                axisAlignedBB2.maxX - entity.getX() + x + 0.05,
                axisAlignedBB2.maxY - entity.getY() + y + 0.15,
                axisAlignedBB2.maxZ - entity.getZ() + z + 0.05
        );

        Vec3d[] vectors = {
                new Vec3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ),
                new Vec3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ),
                new Vec3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ),
                new Vec3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ),
                new Vec3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ),
                new Vec3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ),
                new Vec3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ),
                new Vec3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ)
        };

        Vector4d position = null;
        for (Vec3d vector : vectors) {
            vector = TextUtil.worldSpaceToScreenSpace(vector);
            if (vector == null || vector.z <= 0.0 || vector.z >= 1.0) continue;

            if (position == null) {
                position = new Vector4d(vector.x, vector.y, vector.x, vector.y);
            } else {
                position.x = Math.min(vector.x, position.x);
                position.y = Math.min(vector.y, position.y);
                position.z = Math.max(vector.x, position.z);
                position.w = Math.max(vector.y, position.w);
            }
        }

        return position;
    }

    /**
     * 绘制矩形辅助方法
     */
    private static void drawRect(DrawContext context, float x, float y, float width, float height, Color color) {
        context.fill((int) x, (int) y, (int) (x + width), (int) (y + height), color.getRGB());
    }

    /**
     * 旋转方向枚举
     */
    public enum RotationDirection {
        CLOCKWISE("顺时针"),
        COUNTER_CLOCKWISE("逆时针");

        private final String name;

        RotationDirection(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * 预设视觉样式
     */
    public static class PresetStyles {
        // 目标高亮样式
        public static ImageVisualConfig TARGET_HIGHLIGHT = ImageVisualConfig.createTargetHighlight();

        // 简单方框样式
        public static ImageVisualConfig SIMPLE_BOX = ImageVisualConfig.createSimpleBox();

        // 队友指示器样式
        public static ImageVisualConfig TEAMMATE = createTeammateStyle();

        // 敌人指示器样式
        public static ImageVisualConfig ENEMY = createEnemyStyle();

        // 重要目标样式
        public static ImageVisualConfig IMPORTANT_TARGET = createImportantTargetStyle();

        private static ImageVisualConfig createTeammateStyle() {
            ImageVisualConfig config = new ImageVisualConfig();
            config.color = new Color(0, 255, 0, 150);
            config.scale = 0.9f;
            config.enableRotation = true;
            config.rotationSpeed = 0.5f;
            return config;
        }

        private static ImageVisualConfig createEnemyStyle() {
            ImageVisualConfig config = new ImageVisualConfig();
            config.color = new Color(255, 0, 0, 180);
            config.scale = 1.1f;
            config.enableTint = true;
            config.redTint = 1.0f;
            config.greenTint = 0.3f;
            config.blueTint = 0.3f;
            return config;
        }

        private static ImageVisualConfig createImportantTargetStyle() {
            ImageVisualConfig config = new ImageVisualConfig();
            config.color = new Color(255, 255, 0, 200);
            config.scale = 1.3f;
            config.enableRotation = true;
            config.rotationSpeed = 3.0f;
            config.colorCycle = true;
            config.cycleSpeed = 3.0f;
            return config;
        }
    }

    /**
     * 简单的填充方框绘制
     * @param context DrawContext
     * @param x X坐标
     * @param y Y坐标
     * @param width 宽度
     * @param height 高度
     * @param color 颜色
     */
    public static void drawFilledRect(DrawContext context, float x, float y, float width, float height, Color color) {
        context.fill((int) x, (int) y, (int) (x + width), (int) (y + height), color.getRGB());
    }

    /**
     * 绘制带边框的方框
     * @param context DrawContext
     * @param x X坐标
     * @param y Y坐标
     * @param width 宽度
     * @param height 高度
     * @param fillColor 填充颜色
     * @param borderColor 边框颜色
     * @param borderThickness 边框粗细
     */
    public static void drawBorderedRect(DrawContext context, float x, float y, float width, float height,
                                        Color fillColor, Color borderColor, float borderThickness) {
        // 绘制边框
        drawFilledRect(context, x - borderThickness, y - borderThickness,
                width + borderThickness * 2, borderThickness, borderColor); // 上边框
        drawFilledRect(context, x - borderThickness, y + height,
                width + borderThickness * 2, borderThickness, borderColor); // 下边框
        drawFilledRect(context, x - borderThickness, y,
                borderThickness, height, borderColor); // 左边框
        drawFilledRect(context, x + width, y,
                borderThickness, height, borderColor); // 右边框

        // 绘制填充
        drawFilledRect(context, x, y, width, height, fillColor);
    }
}