package dev.ninemmteam.asm.mixins;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;

@Mixin(MinecraftClient.class)
public class MixinIcon {

    private static boolean customIconSet = false;
    private static int tickCounter = 0;

    @Inject(method = "tick", at = @At("TAIL"))
    private void onClientTick(CallbackInfo ci) {
        if (customIconSet) return;

        tickCounter++;

        // 延迟5个tick（约250ms）后再设置图标
        if (tickCounter >= 5) {
            try {
                MinecraftClient client = (MinecraftClient) (Object) this;
                Window window = client.getWindow();

                if (window != null && window.getHandle() != 0) {
                    setCustomWindowIcon(window);
                    customIconSet = true;
                    System.out.println("[fent@nyl] Custom icon set after delay!");
                }
            } catch (Exception e) {
                System.err.println("[fent@nyl] Failed to set custom icon: " + e.getMessage());
            }
        }
    }

    private void setCustomWindowIcon(Window window) {
        try {
            long windowHandle = window.getHandle();

            // 加载512x512图标
            InputStream iconStream = getClass().getClassLoader()
                    .getResourceAsStream("assets/fentanyl/textures/icon.png");

            if (iconStream == null) {
                System.err.println("[fent@nyl] Custom icon not found!");
                return;
            }

            BufferedImage largeIcon = ImageIO.read(iconStream);
            iconStream.close();

            // 准备多种尺寸
            int[] sizes = {16, 32, 64, 128, 256, 512};
            try (org.lwjgl.system.MemoryStack stack = org.lwjgl.system.MemoryStack.stackPush()) {
                GLFWImage.Buffer icons = GLFWImage.mallocStack(sizes.length, stack);

                for (int i = 0; i < sizes.length; i++) {
                    int size = sizes[i];
                    BufferedImage resized = resizeImage(largeIcon, size, size);
                    ByteBuffer buffer = convertToBuffer(resized);

                    GLFWImage icon = GLFWImage.mallocStack(stack);
                    icon.set(size, size, buffer);
                    icons.put(i, icon);
                }

                // 覆盖设置窗口图标
                GLFW.glfwSetWindowIcon(windowHandle, icons);
                System.out.println("[fent@nyl] Window icon replaced successfully!");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ByteBuffer convertToBuffer(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);
                buffer.put((byte) ((pixel >> 16) & 0xFF));
                buffer.put((byte) ((pixel >> 8) & 0xFF));
                buffer.put((byte) (pixel & 0xFF));
                buffer.put((byte) ((pixel >> 24) & 0xFF));
            }
        }

        buffer.flip();
        return buffer;
    }

    private BufferedImage resizeImage(BufferedImage original, int width, int height) {
        java.awt.Image tmp = original.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2d = resized.createGraphics();
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(tmp, 0, 0, width, height, null);
        g2d.dispose();
        return resized;
    }
}