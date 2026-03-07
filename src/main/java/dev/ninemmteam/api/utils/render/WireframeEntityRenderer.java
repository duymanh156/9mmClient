package dev.ninemmteam.api.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.ninemmteam.api.utils.Wrapper;
import java.awt.Color;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPart.Cuboid;
import net.minecraft.client.model.ModelPart.Quad;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class WireframeEntityRenderer implements Wrapper {
    private static MatrixStack matrices = new MatrixStack();
    private static final Vector4f pos1 = new Vector4f();
    private static final Vector4f pos2 = new Vector4f();
    private static final Vector4f pos3 = new Vector4f();
    private static final Vector4f pos4 = new Vector4f();

    private static Color sideColor;
    private static Color lineColor;
    private static RenderType shapeMode;

    private WireframeEntityRenderer() {
    }

    public static void renderModel(MatrixStack matrix, BipedEntityModel<?> model, RenderType type, Color sideColor, Color lineColor) {
        matrices = matrix;
        shapeMode = type;
        WireframeEntityRenderer.sideColor = sideColor;
        WireframeEntityRenderer.lineColor = lineColor;

        render(model.head);
        render(model.body);
        render(model.leftArm);
        render(model.rightArm);
        render(model.leftLeg);
        render(model.rightLeg);
    }

    private static void render(ModelPart part) {
        if (!part.visible || (part.cuboids.isEmpty() && part.children.isEmpty())) return;

        matrices.push();
        part.rotate(matrices);

        for (Cuboid cuboid : part.cuboids) {
            render(cuboid);
        }
        for (ModelPart child : part.children.values()) {
            render(child);
        }

        matrices.pop();
    }

    private static void render(Cuboid cuboid) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        for (Quad quad : cuboid.sides) {
            pos1.set(quad.vertices[0].pos.x / 16.0F, quad.vertices[0].pos.y / 16.0F, quad.vertices[0].pos.z / 16.0F, 1.0F);
            pos1.mul(matrix);
            pos2.set(quad.vertices[1].pos.x / 16.0F, quad.vertices[1].pos.y / 16.0F, quad.vertices[1].pos.z / 16.0F, 1.0F);
            pos2.mul(matrix);
            pos3.set(quad.vertices[2].pos.x / 16.0F, quad.vertices[2].pos.y / 16.0F, quad.vertices[2].pos.z / 16.0F, 1.0F);
            pos3.mul(matrix);
            pos4.set(quad.vertices[3].pos.x / 16.0F, quad.vertices[3].pos.y / 16.0F, quad.vertices[3].pos.z / 16.0F, 1.0F);
            pos4.mul(matrix);

            if (shapeMode.sides()) {
                BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
                buffer.vertex(pos1.x, pos1.y, pos1.z).color(sideColor.getRed() / 255.0F, sideColor.getGreen() / 255.0F, sideColor.getBlue() / 255.0F, sideColor.getAlpha() / 255.0F);
                buffer.vertex(pos2.x, pos2.y, pos2.z).color(sideColor.getRed() / 255.0F, sideColor.getGreen() / 255.0F, sideColor.getBlue() / 255.0F, sideColor.getAlpha() / 255.0F);
                buffer.vertex(pos3.x, pos3.y, pos3.z).color(sideColor.getRed() / 255.0F, sideColor.getGreen() / 255.0F, sideColor.getBlue() / 255.0F, sideColor.getAlpha() / 255.0F);
                buffer.vertex(pos4.x, pos4.y, pos4.z).color(sideColor.getRed() / 255.0F, sideColor.getGreen() / 255.0F, sideColor.getBlue() / 255.0F, sideColor.getAlpha() / 255.0F);
                BufferRenderer.drawWithGlobalProgram(buffer.endNullable());
            }

            if (shapeMode.lines()) {
                BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
                buffer.vertex(pos1.x, pos1.y, pos1.z).color(lineColor.getRed() / 255.0F, lineColor.getGreen() / 255.0F, lineColor.getBlue() / 255.0F, lineColor.getAlpha() / 255.0F);
                buffer.vertex(pos2.x, pos2.y, pos2.z).color(lineColor.getRed() / 255.0F, lineColor.getGreen() / 255.0F, lineColor.getBlue() / 255.0F, lineColor.getAlpha() / 255.0F);
                buffer.vertex(pos2.x, pos2.y, pos2.z).color(lineColor.getRed() / 255.0F, lineColor.getGreen() / 255.0F, lineColor.getBlue() / 255.0F, lineColor.getAlpha() / 255.0F);
                buffer.vertex(pos3.x, pos3.y, pos3.z).color(lineColor.getRed() / 255.0F, lineColor.getGreen() / 255.0F, lineColor.getBlue() / 255.0F, lineColor.getAlpha() / 255.0F);
                buffer.vertex(pos3.x, pos3.y, pos3.z).color(lineColor.getRed() / 255.0F, lineColor.getGreen() / 255.0F, lineColor.getBlue() / 255.0F, lineColor.getAlpha() / 255.0F);
                buffer.vertex(pos4.x, pos4.y, pos4.z).color(lineColor.getRed() / 255.0F, lineColor.getGreen() / 255.0F, lineColor.getBlue() / 255.0F, lineColor.getAlpha() / 255.0F);
                buffer.vertex(pos4.x, pos4.y, pos4.z).color(lineColor.getRed() / 255.0F, lineColor.getGreen() / 255.0F, lineColor.getBlue() / 255.0F, lineColor.getAlpha() / 255.0F);
                buffer.vertex(pos1.x, pos1.y, pos1.z).color(lineColor.getRed() / 255.0F, lineColor.getGreen() / 255.0F, lineColor.getBlue() / 255.0F, lineColor.getAlpha() / 255.0F);
                BufferRenderer.drawWithGlobalProgram(buffer.endNullable());
            }
        }
    }
}
