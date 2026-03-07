package dev.ninemmteam.mod.gui.earth;

import dev.ninemmteam.fentanyl;
import dev.ninemmteam.mod.gui.earth.component.impl.ModuleComponent;
import dev.ninemmteam.mod.gui.earth.frame.CategoryFrame;
import dev.ninemmteam.mod.gui.earth.frame.DescriptionFrame;
import dev.ninemmteam.mod.gui.earth.frame.ModulesFrame;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.impl.client.ClickGui;
import dev.ninemmteam.mod.modules.impl.client.UISettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;

public class EarthClickGui extends Screen {
    private static EarthClickGui INSTANCE;
    private final ArrayList<Frame> frames = new ArrayList<>();
    private Module.Category[] categories = Module.Category.values();
    private boolean oldVal = false;
    private boolean attached = false;
    private DrawContext context;
    private String description;
    
    private String searchText = "";
    private boolean searchFocused = false;
    private int searchBoxX;
    private int searchBoxY;
    private int searchBoxWidth = 150;
    private int searchBoxHeight = 16;
    
    private boolean lastAbcValue = true;

    public static DescriptionFrame descriptionFrame;

    public EarthClickGui() {
        super(Text.literal("EarthClickGui"));
        INSTANCE = this;
    }

    public static EarthClickGui getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new EarthClickGui();
        }
        return INSTANCE;
    }

    public void init() {
        if (!attached) {
            attached = true;
        }

        getFrames().clear();
        int x = 10;
        int y = 4;
        int frameWidth = 110;
        int frameSpacing = 3;
        
        for (Module.Category category : categories) {
            if (!fentanyl.MODULE.getModulesByCategory(category).isEmpty()) {
                getFrames().add(new CategoryFrame(category, x, y, frameWidth, 16));
                x += frameWidth + frameSpacing;
            }
        }

        descriptionFrame = new DescriptionFrame(x, y, UISettings.getInstance().descriptionWidth.getValueInt(), 16);
        getFrames().add(descriptionFrame);

        getFrames().forEach(Frame::init);
        oldVal = UISettings.getInstance().catEars.getValue();
        
        try {
            lastAbcValue = ClickGui.getInstance().abc.getValue();
        } catch (Exception e) {
            lastAbcValue = true;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.context = context;
        
        if (oldVal != UISettings.getInstance().catEars.getValue()) {
            init();
            oldVal = UISettings.getInstance().catEars.getValue();
        }
        
        try {
            boolean currentAbc = ClickGui.getInstance().abc.getValue();
            if (currentAbc != lastAbcValue) {
                lastAbcValue = currentAbc;
                resortFrames();
            }
        } catch (Exception e) {
        }

        this.description = null;
        getFrames().forEach(frame -> frame.drawScreen(context, mouseX, mouseY, delta));
        renderSearchBox(context, mouseX, mouseY);
    }
    
    private void renderSearchBox(DrawContext context, int mouseX, int mouseY) {
        searchBoxX = context.getScaledWindowWidth() - searchBoxWidth - 10;
        searchBoxY = context.getScaledWindowHeight() - searchBoxHeight - 10;
        
        int bgColor = searchFocused ? 0xDD333333 : 0xAA222222;
        int borderColor = searchFocused ? 0xFF0050FF : 0xFF444444;
        
        context.fill(searchBoxX, searchBoxY, searchBoxX + searchBoxWidth, searchBoxY + searchBoxHeight, bgColor);
        context.drawBorder(searchBoxX, searchBoxY, searchBoxWidth, searchBoxHeight, borderColor);
        
        String displayText = searchText;
        if (searchText.isEmpty() && !searchFocused) {
            displayText = "Search...";
        }
        
        int textColor = searchText.isEmpty() && !searchFocused ? 0x88888888 : 0xFFFFFFFF;
        context.drawText(this.client.textRenderer, displayText, searchBoxX + 4, searchBoxY + 4, textColor, false);
        
        if (searchFocused && System.currentTimeMillis() % 1000 < 500) {
            int cursorX = searchBoxX + 4 + this.client.textRenderer.getWidth(searchText);
            context.fill(cursorX, searchBoxY + 3, cursorX + 1, searchBoxY + searchBoxHeight - 3, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX;
        int my = (int) mouseY;
        
        if (mx >= searchBoxX && mx <= searchBoxX + searchBoxWidth && my >= searchBoxY && my <= searchBoxY + searchBoxHeight) {
            searchFocused = true;
            return true;
        } else {
            searchFocused = false;
        }
        
        if (button == 0) {
            for (Frame frame : new ArrayList<>(getFrames())) {
                frame.setDragging(false);
            }
            
            for (Frame frame : new ArrayList<>(getFrames())) {
                if (frame.processMouseClick(mx, my, button)) {
                    frame.setDragging(true);
                    break;
                }
            }
        }
        
        for (Frame frame : new ArrayList<>(getFrames())) {
            frame.mouseClicked(mx, my, button);
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX;
        int my = (int) mouseY;
        
        if (button == 0) {
            for (Frame frame : new ArrayList<>(getFrames())) {
                frame.setDragging(false);
            }
        }
        
        for (Frame frame : new ArrayList<>(getFrames())) {
            frame.mouseReleased(mx, my, button);
        }
        
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int mx = (int) mouseX;
        int my = (int) mouseY;
        
        for (Frame frame : new ArrayList<>(getFrames())) {
            if (frame instanceof ModulesFrame) {
                if (Frame.mouseWithinBounds(mx, my, frame.getPosX(), frame.getPosY() + frame.getHeight(), frame.getWidth(), 1000)) {
                    ((ModulesFrame) frame).handleScroll(verticalAmount);
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                searchFocused = false;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !searchText.isEmpty()) {
                searchText = searchText.substring(0, searchText.length() - 1);
                resortFrames();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                searchFocused = false;
                return true;
            }
            return true;
        }
        
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.close();
            return true;
        }
        getFrames().forEach(frame -> frame.keyTyped((char) keyCode, keyCode));
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchFocused) {
            if (chr >= 32 && chr <= 126) {
                searchText += chr;
                resortFrames();
            }
            return true;
        }
        getFrames().forEach(frame -> frame.charTyped(chr, modifiers));
        return super.charTyped(chr, modifiers);
    }
    
    private void resortFrames() {
        for (Frame frame : new ArrayList<>(getFrames())) {
            if (frame instanceof CategoryFrame) {
                ((CategoryFrame) frame).resort();
            }
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        searchText = "";
        searchFocused = false;
        for (Frame frame : new ArrayList<>(getFrames())) {
            for (var component : new ArrayList<>(frame.getComponents())) {
                if (component instanceof ModuleComponent) {
                    ((ModuleComponent) component).setExtended(false);
                }
            }
        }
        super.close();
    }

    public ArrayList<Frame> getFrames() {
        return frames;
    }

    public DrawContext getContext() {
        return context;
    }

    public void setDescription(String description) {
        this.description = description;
        if (descriptionFrame != null) {
            descriptionFrame.setDescription(description);
        }
    }

    public String getDescription() {
        return description;
    }
    
    public String getSearchText() {
        return searchText;
    }
    
    public boolean isSearchMatch(String moduleName) {
        if (searchText.isEmpty()) {
            return false;
        }
        String search = searchText.toLowerCase();
        String name = moduleName.toLowerCase();
        return name.contains(search);
    }
}
