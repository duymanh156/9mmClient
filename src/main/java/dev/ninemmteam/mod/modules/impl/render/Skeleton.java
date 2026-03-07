package dev.ninemmteam.mod.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.ninemmteam.api.utils.math.MathUtil;
import dev.ninemmteam.api.utils.render.Render3DUtil;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.settings.impl.ColorSetting;
import java.awt.Color;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class Skeleton extends Module {
    public static Skeleton INSTANCE;

    private final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 255, 255, 255)));

    public Skeleton() {
        super("Skeleton", Module.Category.Render);
        this.setChinese("骨骼");
        INSTANCE = this;
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        if (mc.gameRenderer == null || mc.getCameraEntity() == null) {
            return;
        }

        float tickDelta = mc.getRenderTickCounter().getTickDelta(true);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(mc.isFancyGraphicsOrBetter());
        RenderSystem.enableCull();

        for (Entity entity : mc.world.getEntities()) {
            if (entity == null || !entity.isAlive()) {
                continue;
            }
            if (entity instanceof PlayerEntity playerEntity) {
                if (mc.options.getPerspective().isFirstPerson() && playerEntity == mc.player) {
                    continue;
                }

                Vec3d skeletonPos = MathUtil.getRenderPosition(entity, tickDelta);

                PlayerEntityRenderer livingEntityRenderer =
                        (PlayerEntityRenderer) (LivingEntityRenderer<?, ?>) mc.getEntityRenderDispatcher().getRenderer(playerEntity);
                PlayerEntityModel<AbstractClientPlayerEntity> playerModel = livingEntityRenderer.getModel();

                float bodyYaw = MathHelper.lerpAngleDegrees(tickDelta,
                        playerEntity.prevBodyYaw, playerEntity.bodyYaw);
                float headYaw = MathHelper.lerpAngleDegrees(tickDelta,
                        playerEntity.prevHeadYaw, playerEntity.headYaw);

                BipedEntityModel.ArmPose armPose = getArmPose(playerEntity, Hand.MAIN_HAND);
                BipedEntityModel.ArmPose armPose2 = getArmPose(playerEntity, Hand.OFF_HAND);

                if (armPose.isTwoHanded()) {
                    armPose2 = playerEntity.getOffHandStack().isEmpty() ? BipedEntityModel.ArmPose.EMPTY : BipedEntityModel.ArmPose.ITEM;
                }

                if (playerEntity.getMainArm() == Arm.RIGHT) {
                    playerModel.rightArmPose = armPose;
                    playerModel.leftArmPose = armPose2;
                } else {
                    playerModel.rightArmPose = armPose2;
                    playerModel.leftArmPose = armPose;
                }

                float limbSpeed = 0.0f;
                float limbAngle = 0.0f;
                if (!playerEntity.hasVehicle() && playerEntity.isAlive()) {
                    limbSpeed = playerEntity.limbAnimator.getSpeed(tickDelta);
                    limbAngle = playerEntity.limbAnimator.getPos(tickDelta);
                    if (playerEntity.isBaby()) {
                        limbAngle *= 3.0f;
                    }
                    if (limbSpeed > 1.0f) {
                        limbSpeed = 1.0f;
                    }
                }

                float age = playerEntity.age + tickDelta;
                float headYawDiff = headYaw - bodyYaw;
                float pitch = playerEntity.getPitch(tickDelta);

                playerModel.animateModel((AbstractClientPlayerEntity) playerEntity, limbAngle, limbSpeed, tickDelta);
                playerModel.setAngles((AbstractClientPlayerEntity) playerEntity, limbAngle, limbSpeed, age, headYawDiff, pitch);

                boolean swimming = playerEntity.isInSwimmingPose();
                boolean sneaking = playerEntity.isInSneakingPose();
                boolean flying = playerEntity.isFallFlying();

                ModelPart head = playerModel.head;
                ModelPart leftArm = playerModel.leftArm;
                ModelPart rightArm = playerModel.rightArm;
                ModelPart leftLeg = playerModel.leftLeg;
                ModelPart rightLeg = playerModel.rightLeg;

                playerModel.sneaking = entity.isInSneakingPose();

                MatrixStack renderStack = Render3DUtil.matrixFrom(skeletonPos.x, skeletonPos.y, skeletonPos.z);
                renderStack.push();
                if (swimming) {
                    renderStack.translate(0, 0.35f, 0);
                }
                renderStack.multiply(new Quaternionf().setAngleAxis((bodyYaw + 180.0f) * Math.PI / 180.0f, 0, -1, 0));
                if (swimming || flying) {
                    renderStack.multiply(new Quaternionf().setAngleAxis((90.0f + pitch) * Math.PI / 180.0f, -1, 0, 0));
                }
                if (swimming) {
                    renderStack.translate(0, -0.95f, 0);
                }

                Matrix4f matrix4f = renderStack.peek().getPositionMatrix();
                Color c = color.getValue();
                float r = c.getRed() / 255.0f;
                float g = c.getGreen() / 255.0f;
                float b = c.getBlue() / 255.0f;
                float a = c.getAlpha() / 255.0f;

                BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

                bufferBuilder.vertex(matrix4f, 0, sneaking ? 0.6f : 0.7f, sneaking ? 0.23f : 0).color(r, g, b, a);
                bufferBuilder.vertex(matrix4f, 0, sneaking ? 1.05f : 1.4f, 0).color(r, g, b, a);

                bufferBuilder.vertex(matrix4f, -0.37f, sneaking ? 1.05f : 1.35f, 0).color(r, g, b, a);
                bufferBuilder.vertex(matrix4f, 0.37f, sneaking ? 1.05f : 1.35f, 0).color(r, g, b, a);

                bufferBuilder.vertex(matrix4f, -0.15f, sneaking ? 0.6f : 0.7f, sneaking ? 0.23f : 0).color(r, g, b, a);
                bufferBuilder.vertex(matrix4f, 0.15f, sneaking ? 0.6f : 0.7f, sneaking ? 0.23f : 0).color(r, g, b, a);

                renderStack.push();
                renderStack.translate(0, sneaking ? 1.05f : 1.4f, 0);
                rotateSkeleton(renderStack, head);
                matrix4f = renderStack.peek().getPositionMatrix();
                bufferBuilder.vertex(matrix4f, 0, 0, 0).color(r, g, b, a);
                bufferBuilder.vertex(matrix4f, 0, 0.25f, 0).color(r, g, b, a);
                renderStack.pop();

                renderStack.push();
                renderStack.translate(0.15f, sneaking ? 0.6f : 0.7f, sneaking ? 0.23f : 0);
                rotateSkeleton(renderStack, rightLeg);
                matrix4f = renderStack.peek().getPositionMatrix();
                bufferBuilder.vertex(matrix4f, 0, 0, 0).color(r, g, b, a);
                bufferBuilder.vertex(matrix4f, 0, -0.6f, 0).color(r, g, b, a);
                renderStack.pop();

                renderStack.push();
                renderStack.translate(-0.15f, sneaking ? 0.6f : 0.7f, sneaking ? 0.23f : 0);
                rotateSkeleton(renderStack, leftLeg);
                matrix4f = renderStack.peek().getPositionMatrix();
                bufferBuilder.vertex(matrix4f, 0, 0, 0).color(r, g, b, a);
                bufferBuilder.vertex(matrix4f, 0, -0.6f, 0).color(r, g, b, a);
                renderStack.pop();

                renderStack.push();
                renderStack.translate(0.37f, sneaking ? 1.05f : 1.35f, 0);
                rotateSkeleton(renderStack, rightArm);
                matrix4f = renderStack.peek().getPositionMatrix();
                bufferBuilder.vertex(matrix4f, 0, 0, 0).color(r, g, b, a);
                bufferBuilder.vertex(matrix4f, 0, -0.55f, 0).color(r, g, b, a);
                renderStack.pop();

                renderStack.push();
                renderStack.translate(-0.37f, sneaking ? 1.05f : 1.35f, 0);
                rotateSkeleton(renderStack, leftArm);
                matrix4f = renderStack.peek().getPositionMatrix();
                bufferBuilder.vertex(matrix4f, 0, 0, 0).color(r, g, b, a);
                bufferBuilder.vertex(matrix4f, 0, -0.55f, 0).color(r, g, b, a);
                renderStack.pop();

                BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());

                if (swimming) {
                    renderStack.translate(0, 0.95f, 0);
                }
                if (swimming || flying) {
                    renderStack.multiply(new Quaternionf().setAngleAxis((90.0f + pitch) * Math.PI / 180.0f, 1, 0, 0));
                }
                if (swimming) {
                    renderStack.translate(0, -0.35f, 0);
                }
                renderStack.multiply(new Quaternionf().setAngleAxis((bodyYaw + 180.0f) * Math.PI / 180.0f, 0, 1, 0));
                renderStack.translate(-skeletonPos.x, -skeletonPos.y, -skeletonPos.z);
                renderStack.pop();
            }
        }

        RenderSystem.disableCull();
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
    }

    private void rotateSkeleton(MatrixStack matrix, ModelPart modelPart) {
        if (modelPart.roll != 0.0f) {
            matrix.multiply(RotationAxis.POSITIVE_Z.rotation(modelPart.roll));
        }
        if (modelPart.yaw != 0.0f) {
            matrix.multiply(RotationAxis.NEGATIVE_Y.rotation(modelPart.yaw));
        }
        if (modelPart.pitch != 0.0f) {
            matrix.multiply(RotationAxis.NEGATIVE_X.rotation(modelPart.pitch));
        }
    }

    private BipedEntityModel.ArmPose getArmPose(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (stack.isEmpty()) {
            return BipedEntityModel.ArmPose.EMPTY;
        }

        if (player.isUsingItem() && player.getActiveHand() == hand) {
            UseAction useAction = stack.getUseAction();
            switch (useAction) {
                case BLOCK:
                    return BipedEntityModel.ArmPose.BLOCK;
                case BOW:
                    return BipedEntityModel.ArmPose.BOW_AND_ARROW;
                case CROSSBOW:
                    return BipedEntityModel.ArmPose.CROSSBOW_CHARGE;
                case SPEAR:
                    return BipedEntityModel.ArmPose.THROW_SPEAR;
                case SPYGLASS:
                    return BipedEntityModel.ArmPose.SPYGLASS;
                case TOOT_HORN:
                    return BipedEntityModel.ArmPose.TOOT_HORN;
                case BRUSH:
                    return BipedEntityModel.ArmPose.BRUSH;
                default:
                    return BipedEntityModel.ArmPose.ITEM;
            }
        }

        if (stack.getItem() == Items.BOW) {
            return BipedEntityModel.ArmPose.BOW_AND_ARROW;
        }
        if (stack.getItem() == Items.CROSSBOW) {
            return BipedEntityModel.ArmPose.CROSSBOW_HOLD;
        }
        if (stack.getItem() == Items.TRIDENT) {
            return BipedEntityModel.ArmPose.THROW_SPEAR;
        }
        if (stack.getItem() == Items.SPYGLASS) {
            return BipedEntityModel.ArmPose.SPYGLASS;
        }

        return BipedEntityModel.ArmPose.ITEM;
    }

    @Override
    public String getInfo() {
        return null;
    }
}
