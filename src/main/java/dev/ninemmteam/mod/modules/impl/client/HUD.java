package dev.ninemmteam.mod.modules.impl.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.ninemmteam.fentanyl;
import dev.ninemmteam.api.events.eventbus.EventListener;
import dev.ninemmteam.api.events.impl.ClientTickEvent;
import dev.ninemmteam.api.events.impl.InitEvent;
import dev.ninemmteam.api.utils.Wrapper;
import dev.ninemmteam.api.utils.math.Animation;
import dev.ninemmteam.api.utils.math.Easing;
import dev.ninemmteam.api.utils.render.ColorUtil;
import dev.ninemmteam.api.utils.render.Render2DUtil;
import dev.ninemmteam.api.utils.render.TextUtil;
import dev.ninemmteam.asm.accessors.ISimpleRegistry;
import dev.ninemmteam.core.impl.FontManager;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.settings.impl.BooleanSetting;
import dev.ninemmteam.mod.modules.settings.impl.ColorSetting;
import dev.ninemmteam.mod.modules.settings.impl.EnumSetting;
import dev.ninemmteam.mod.modules.settings.impl.SliderSetting;
import dev.ninemmteam.mod.modules.settings.impl.StringSetting;
import java.awt.Color;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import static dev.ninemmteam.fentanyl.VERSION;

public class HUD extends Module {
    public static HUD INSTANCE;
    public final EnumSetting<HUD.Page> page = this.add(new EnumSetting("Page", HUD.Page.General));
    public final BooleanSetting renderingUp = this.add(new BooleanSetting("RenderingUp", false, () -> this.page.is(HUD.Page.General)));
    public final BooleanSetting font = this.add(new BooleanSetting("Font", false, () -> this.page.is(HUD.Page.General)));
    public final BooleanSetting shadow = this.add(new BooleanSetting("Shadow", true, () -> this.page.is(HUD.Page.General)));
    public final BooleanSetting lowerCase = this.add(new BooleanSetting("LowerCase", false, () -> this.page.is(HUD.Page.General)));
    public final BooleanSetting sort = this.add(new BooleanSetting("Sort", false, () -> this.page.is(HUD.Page.General)));
    public final EnumSetting<Easing> easing = this.add(new EnumSetting("Easing", Easing.CircInOut, () -> this.page.is(HUD.Page.General)));
    public final BooleanSetting arrayList = this.add(new BooleanSetting("ArrayList", true, () -> this.page.is(HUD.Page.Element)).setParent());
    public final SliderSetting xOffset = this.add(new SliderSetting("XOffset", 38.3, 0.0, 50.0, 0.1, () -> this.page.is(HUD.Page.Element)&& this.arrayList.isOpen()));
    public final SliderSetting yOffset = this.add(new SliderSetting("YOffset", 17.2, 0.0, 50.0, 0.1, () -> this.page.is(HUD.Page.Element)&& this.arrayList.isOpen()));
    public final SliderSetting textOffset = this.add(new SliderSetting("TextOffset", 0.0, -10.0, 10.0, 0.1, () -> this.page.is(HUD.Page.Element) && this.arrayList.isOpen()));
    public final SliderSetting interval = this.add(new SliderSetting("Interval", 2.2, 0.0, 15.0, 0.1, () -> this.page.is(HUD.Page.Element) && this.arrayList.isOpen()));
    public final SliderSetting enableLength = this.add(new SliderSetting("EnableLength", 200, 0, 1000, () -> this.page.is(HUD.Page.Element) && this.arrayList.isOpen()));
    public final SliderSetting disableLength = this.add(new SliderSetting("DisableLength", 200, 0, 1000, () -> this.page.is(HUD.Page.Element) && this.arrayList.isOpen()));
    public final SliderSetting fadeLength = this.add(new SliderSetting("FadeLength", 200, 0, 1000, () -> this.page.is(HUD.Page.Element) && this.arrayList.isOpen()));
    public final BooleanSetting listSort = this.add(new BooleanSetting("ListSort", true, () -> this.page.is(HUD.Page.Element) && this.arrayList.isOpen()));

    // 新增moduleList图标设置
    public final BooleanSetting moduleIcon = this.add(new BooleanSetting("ModuleIcon", false, () -> this.page.is(HUD.Page.Element) && this.arrayList.isOpen()).setParent());
    public final SliderSetting iconSize = this.add(new SliderSetting("IconSize", 13.5, 8.0, 24.0, 0.1, () -> this.page.is(HUD.Page.Element) && this.arrayList.isOpen() && this.moduleIcon.getValue()));
    public final SliderSetting iconSpacing = this.add(new SliderSetting("IconSpacing", 5.9, 0.0, 20.0, 0.1, () -> this.page.is(HUD.Page.Element) && this.arrayList.isOpen() && this.moduleIcon.getValue()));
    public final SliderSetting iconRoundness = this.add(new SliderSetting("IconRoundness", 3.0, 0.0, 8.0, 0.1, () -> this.page.is(HUD.Page.Element) && this.arrayList.isOpen() && this.moduleIcon.getValue()));
    public final BooleanSetting iconBackground = this.add(new BooleanSetting("IconBackground", true, () -> this.page.is(HUD.Page.Element) && this.arrayList.isOpen() && this.moduleIcon.getValue()));
    public final ColorSetting iconBgColor = this.add(new ColorSetting("IconBgColor", new Color(30, 30, 30, 150), () -> this.page.is(HUD.Page.Element) && this.arrayList.isOpen() && this.moduleIcon.getValue() && this.iconBackground.getValue()));
    public final BooleanSetting iconBorder = this.add(new BooleanSetting("IconBorder", false, () -> this.page.is(HUD.Page.Element) && this.arrayList.isOpen() && this.moduleIcon.getValue()));
    public final SliderSetting borderThickness = this.add(new SliderSetting("BorderThickness", 1.0, 0.5, 3.0, 0.1, () -> this.page.is(HUD.Page.Element) && this.arrayList.isOpen() && this.moduleIcon.getValue() && this.iconBorder.getValue()));
    public final ColorSetting borderColor = this.add(new ColorSetting("BorderColor", new Color(80, 80, 80, 200), () -> this.page.is(HUD.Page.Element) && this.arrayList.isOpen() && this.moduleIcon.getValue() && this.iconBorder.getValue()));

    public final BooleanSetting armor = this.add(new BooleanSetting("Armor", true, () -> this.page.is(HUD.Page.Element)).setParent());
    public final SliderSetting armorOffset = this.add(new SliderSetting("ArmorOffset", 1.0, 0.0, 100.0, -1.0, () -> this.page.is(HUD.Page.Element) && this.armor.isOpen()));
    public final BooleanSetting durability = this.add(new BooleanSetting("Durability", true, () -> this.page.is(HUD.Page.Element) && this.armor.isOpen()));
    public final BooleanSetting waterMark = this.add(new BooleanSetting("WaterMark", true, () -> this.page.is(HUD.Page.Element)).setParent());
    public final ColorSetting pulse = this.add(new ColorSetting("Pulse", new Color(153, 153, 255), () -> this.page.is(HUD.Page.Element) && this.waterMark.isOpen()).injectBoolean(true));
    public final StringSetting waterMarkString = this.add(new StringSetting("Title", "%hackname%", () -> this.page.is(HUD.Page.Element) && this.waterMark.isOpen()));
    public final SliderSetting waterMarkOffset = this.add(new SliderSetting("Offset", 1.0, 0.0, 100.0, -1.0, () -> this.page.is(HUD.Page.Element) && this.waterMark.isOpen()));
    public final BooleanSetting fps = this.add(new BooleanSetting("FPS", true, () -> this.page.is(HUD.Page.Element)));
    public final BooleanSetting ping = this.add(new BooleanSetting("Ping", true, () -> this.page.is(HUD.Page.Element)));
    public final BooleanSetting tps = this.add(new BooleanSetting("TPS", true, () -> this.page.is(HUD.Page.Element)));
    public final BooleanSetting packets = this.add(new BooleanSetting("Packets", false, () -> this.page.is(HUD.Page.Element)));
    public final BooleanSetting ip = this.add(new BooleanSetting("IP", false, () -> this.page.is(HUD.Page.Element)));
    public final BooleanSetting time = this.add(new BooleanSetting("Time", false, () -> this.page.is(HUD.Page.Element)));
    public final BooleanSetting speed = this.add(new BooleanSetting("Speed", true, () -> this.page.is(HUD.Page.Element)));
    public final BooleanSetting brand = this.add(new BooleanSetting("Brand", false, () -> this.page.is(HUD.Page.Element)));
    public final BooleanSetting potions = this.add(new BooleanSetting("Potions", true, () -> this.page.is(HUD.Page.Element)));
    public final BooleanSetting coords = this.add(new BooleanSetting("Coords", true, () -> this.page.is(HUD.Page.Element)).setParent());
    public final BooleanSetting colorSync = this.add(new BooleanSetting("ColorSync", true, () -> this.page.is(HUD.Page.Element) && this.coords.isOpen()));
    public final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 255, 255), () -> this.page.is(HUD.Page.Color)));
    private final EnumSetting<HUD.ColorMode> colorMode = this.add(new EnumSetting("ColorMode", HUD.ColorMode.Pulse, () -> this.page.is(HUD.Page.Color)));
    private final SliderSetting rainbowSpeed = this.add(new SliderSetting("RainbowSpeed", 4.0, 1.0, 10.0, 0.1, () -> this.page.is(HUD.Page.Color) && this.colorMode.getValue() == HUD.ColorMode.Rainbow));
    private final SliderSetting saturation = this.add(new SliderSetting("Saturation", 130.0, 1.0, 255.0, () -> this.page.is(HUD.Page.Color) && this.colorMode.getValue() == HUD.ColorMode.Rainbow));
    private final SliderSetting rainbowDelay = this.add(new SliderSetting("Delay", 350, 0, 1000, () -> this.page.is(HUD.Page.Color) && this.colorMode.getValue() == HUD.ColorMode.Rainbow));
    private final ColorSetting endColor = this.add(new ColorSetting("SecondColor", new Color(255, 255, 255, 255), () -> this.page.is(HUD.Page.Color) && this.colorMode.getValue() == HUD.ColorMode.Pulse).injectBoolean(true));
    private final SliderSetting pulseSpeed = this.add(new SliderSetting("PulseSpeed", 1.0, 0.0, 5.0, 0.1, () -> this.page.is(HUD.Page.Color)));
    private final SliderSetting pulseCounter = this.add(new SliderSetting("Counter", 10, 1, 50, () -> this.page.is(HUD.Page.Color)));
    public final BooleanSetting blur = this.add(new BooleanSetting("Blur", false, () -> this.page.is(HUD.Page.Color)).setParent());
    public final SliderSetting radius = this.add(new SliderSetting("Radius", 10.0, 0.0, 100.0, () -> this.page.is(HUD.Page.Color) && this.blur.isOpen()));
    public final BooleanSetting backGround = this.add(new BooleanSetting("BackGround", false, () -> this.page.is(HUD.Page.Color)).setParent());
    public final SliderSetting width = this.add(new SliderSetting("Width", 0.0, 0.0, 15.0, () -> this.page.is(HUD.Page.Color) && this.backGround.isOpen()));
    public final SliderSetting borderRadius = this.add(new SliderSetting("BorderRadius", 2.7, 0.0, 20.0, 0.1, () -> this.page.is(HUD.Page.Color) && this.backGround.isOpen()));
    public final ColorSetting bgColor = this.add(new ColorSetting("BGColor", new Color(0, 0, 0, 100), () -> this.page.is(HUD.Page.Color) && this.backGround.isOpen()));
    public final ColorSetting rect = this.add(new ColorSetting("Rect", new Color(208, 0, 0), () -> this.page.is(HUD.Page.Color)).injectBoolean(false));
    public final ColorSetting glow = this.add(new ColorSetting("Glow", new Color(208, 0, 100), () -> this.page.is(HUD.Page.Color)).injectBoolean(false));
    private final DecimalFormat decimal = new DecimalFormat("0.0");
    private final ArrayList<HUD.Info> infoList = new ArrayList();
    private final ArrayList<HUD.Info> moduleList = new ArrayList();

    // 模块图标资源
    private static final Identifier DEFAULT_ICON = Identifier.of("fentanyl", "textures/icon.png");

    public HUD() {
        super("HUD", Module.Category.Client);
        this.setChinese("界面");
        INSTANCE = this;
        fentanyl.EVENT_BUS.subscribe(new HUD.InitHandler());

        for (StatusEffect potionEffect : Registries.STATUS_EFFECT) {
            try {
                RegistryEntry<StatusEffect> effectRegistryEntry = (RegistryEntry<StatusEffect>)((ISimpleRegistry)Registries.STATUS_EFFECT)
                        .getValueToEntry()
                        .get(potionEffect);
                this.infoList.add(new HUD.Info(() -> {
                    StatusEffectInstance effect = mc.player.getStatusEffect(effectRegistryEntry);
                    if (effect != null) {
                        String s = potionEffect.getName().getString() + " " + (effect.getAmplifier() + 1);
                        String s2 = getDuration(effect);
                        return s + " §f" + s2;
                    } else {
                        return "";
                    }
                }, () -> mc.player.hasStatusEffect(effectRegistryEntry) && this.potions.getValue()));
            } catch (Exception var4) {
            }
        }

        this.infoList
                .add(
                        new HUD.Info(
                                () -> "ServerBrand §f" + (mc.isInSingleplayer() ? "Vanilla" : mc.getNetworkHandler().getBrand().replaceAll("\\(.*?\\)", "")),
                                this.brand::getValue
                        )
                );
        this.infoList.add(new HUD.Info(() -> "Server §f" + (mc.isInSingleplayer() ? "SinglePlayer" : mc.getCurrentServerEntry().address), this.ip::getValue));
        this.infoList.add(new HUD.Info(() -> "TPS §f" + fentanyl.SERVER.getTPS() + " [" + fentanyl.SERVER.getCurrentTPS() + "]", this.tps::getValue));
        this.infoList.add(new HUD.Info(() -> {
            double x = mc.player.getX() - mc.player.prevX;
            double z = mc.player.getZ() - mc.player.prevZ;
            double dist = Math.sqrt(x * x + z * z) / 1000.0;
            double div = 1.388888888888889E-5;
            float timer = fentanyl.TIMER.get();
            double playerSpeed = dist / div * timer;
            return String.format("Speed §f%skm/h", this.decimal.format(playerSpeed));
        }, this.speed::getValue));
        this.infoList.add(new HUD.Info(() -> "Time §f" + new SimpleDateFormat("h:mm a", Locale.ENGLISH).format(new Date()), this.time::getValue));
        this.infoList.add(new HUD.Info(() -> {
            PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            String playerPing;
            if (playerListEntry == null) {
                playerPing = "Unknown";
            } else {
                playerPing = playerListEntry.getLatency() + "ms";
            }

            return "Ping §f" + playerPing;
        }, this.ping::getValue));
        this.infoList.add(new HUD.Info(() -> "FPS §f" + fentanyl.FPS.getFps(), this.fps::getValue));
        this.infoList.add(new HUD.Info(() -> {
            int outgoing = fentanyl.NETWORK.getOutgoingPPS();
            int incoming = fentanyl.NETWORK.getIncomingPPS();
            return "Packets §f" + incoming + "<-" + outgoing;
        }, this.packets::getValue));
    }

    public static String getDuration(StatusEffectInstance pe) {
        if (pe.isInfinite()) {
            return "∞";
        } else {
            int var1 = pe.getDuration();
            int mins = var1 / 1200;
            int sec = var1 % 1200 / 20;
            return String.format("%d:%02d", mins, sec);
        }
    }

    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) {
        if (this.waterMark.getValue()) {
            if (this.pulse.booleanValue) {
                TextUtil.drawStringPulse(
                        drawContext,
                        this.waterMarkString.getValue().replaceAll("%hackname%", "fent@nyl " + Formatting.WHITE + VERSION),
                        this.waterMarkOffset.getValueInt(),
                        this.waterMarkOffset.getValueInt(),
                        this.color.getValue(),
                        this.pulse.getValue(),
                        this.pulseSpeed.getValue(),
                        this.pulseCounter.getValueInt(),
                        this.font.getValue(),
                        this.shadow.getValue()
                );
            } else {
                TextUtil.drawString(
                        drawContext,
                        this.waterMarkString.getValue().replaceAll("%hackname%", "fent@nyl " + Formatting.WHITE + VERSION),
                        this.waterMarkOffset.getValueInt(),
                        this.waterMarkOffset.getValueInt(),
                        this.color.getValue().getRGB(),
                        this.font.getValue(),
                        this.shadow.getValue()
                );
            }
        }

        int fontHeight = this.getHeight();
        if (this.coords.getValue()) {
            String coordsString = getCoords();
            this.drawCoord(drawContext, coordsString, mc.getWindow().getScaledHeight() - fontHeight - (mc.currentScreen instanceof ChatScreen ? 15 : 0));
        }

        HUD.Info.onRender(drawContext, this.infoList, this.renderingUp.getValue());
        if (this.arrayList.getValue()) {
            HUD.Info.onRender(drawContext, this.moduleList, !this.renderingUp.getValue());
        }
    }

    @EventListener(priority = -999)
    public void onUpdate(ClientTickEvent event) {
        if (!nullCheck()) {
            if (event.isPost()) {
                HUD.Info.onUpdate(this.infoList, this.sort.getValue());
                if (this.arrayList.getValue()) {
                    HUD.Info.onUpdate(this.moduleList, this.listSort.getValue());
                }
            }
        }
    }

    private static String getCoords() {
        boolean inNether = mc.world.getRegistryKey().equals(World.NETHER);
        int posX = mc.player.getBlockX();
        int posY = mc.player.getBlockY();
        int posZ = mc.player.getBlockZ();
        float factor = !inNether ? 0.125F : 8.0F;
        int anotherWorldX = (int)(mc.player.getX() * factor);
        int anotherWorldZ = (int)(mc.player.getZ() * factor);
        return "XYZ §f"
                + (
                inNether
                        ? posX + ", " + posY + ", " + posZ + " §7[§f" + anotherWorldX + ", " + anotherWorldZ + "§7]§f"
                        : posX + ", " + posY + ", " + posZ + "§7 [§f" + anotherWorldX + ", " + anotherWorldZ + "§7]"
        );
    }

    public int getWidth(String s) {
        if (this.lowerCase.getValue()) {
            s = s.toLowerCase();
        }

        return this.font.getValue() ? (int)FontManager.ui.getWidth(s) : mc.textRenderer.getWidth(s);
    }

    private int getHeight() {
        return this.font.getValue() ? (int)FontManager.ui.getFontHeight() : 9;
    }

    private void drawCoord(DrawContext drawContext, String s, int y) {
        if (this.colorSync.getValue()) {
            if (this.lowerCase.getValue()) {
                s = s.toLowerCase();
            }

            TextUtil.drawString(drawContext, s, 2.0, y, this.getColor(20.0), this.font.getValue(), this.shadow.getValue());
        } else if (this.pulse.booleanValue) {
            TextUtil.drawStringPulse(
                    drawContext,
                    s,
                    2.0,
                    y,
                    this.color.getValue(),
                    this.pulse.getValue(),
                    this.pulseSpeed.getValue(),
                    this.pulseCounter.getValueInt(),
                    this.font.getValue(),
                    this.shadow.getValue()
            );
        } else {
            TextUtil.drawString(drawContext, s, 2.0, y, this.color.getValue().getRGB(), this.font.getValue(), this.shadow.getValue());
        }
    }

    public int getColor(double counter) {
        return this.colorMode.getValue() != HUD.ColorMode.Custom ? this.rainbow(counter).getRGB() : this.color.getValue().getRGB();
    }

    private Color rainbow(double delay) {
        if (this.colorMode.getValue() == HUD.ColorMode.Pulse) {
            return this.endColor.booleanValue
                    ? ColorUtil.pulseColor(this.color.getValue(), this.endColor.getValue(), delay, this.pulseCounter.getValueInt(), this.pulseSpeed.getValue())
                    : ColorUtil.pulseColor(this.color.getValue(), delay, this.pulseCounter.getValueInt(), this.pulseSpeed.getValue());
        } else if (this.colorMode.getValue() == HUD.ColorMode.Rainbow) {
            double rainbowState = Math.ceil((System.currentTimeMillis() * this.rainbowSpeed.getValue() + delay * this.rainbowDelay.getValue()) / 20.0);
            return Color.getHSBColor((float)(rainbowState % 360.0 / 360.0), this.saturation.getValueFloat() / 255.0F, 1.0F);
        } else {
            return this.color.getValue();
        }
    }

    public int getFontHeight() {
        return this.font.getValue() ? (int)FontManager.ui.getFontHeight() : 9;
    }

    private static enum ColorMode {
        Custom,
        Pulse,
        Rainbow;
    }

    public class Info {
        public final Callable<String> info;
        public String string;
        public final BooleanSupplier drawn;
        public double currentX = 0.0;
        public boolean isOn;
        public final Animation animation = new Animation();
        public final Animation fadeAnimation = new Animation();
        static double fontHeight;
        static double currentY;
        static int windowWidth;
        static boolean fromUp;
        static double counter;

        public Info(Callable<String> info, BooleanSupplier drawn) {
            this.info = info;
            this.drawn = drawn;

            try {
                this.string = (String)this.info.call();
            } catch (Exception var5) {
            }
        }

        public static void onRender(DrawContext context, List<HUD.Info> list, boolean fromUp) {
            counter = 20.0;
            HUD.Info.fromUp = fromUp;
            fontHeight = HUD.INSTANCE.getFontHeight();
            currentY = fromUp
                    ? 1.0 + HUD.INSTANCE.yOffset.getValue()
                    : Wrapper.mc.getWindow().getScaledHeight()
                    - fontHeight
                    - 1.0
                    - (
                    Wrapper.mc.currentScreen instanceof ChatScreen && HUD.INSTANCE.yOffset.getValue() < 12.0
                            ? 12.0 - HUD.INSTANCE.yOffset.getValue() + HUD.INSTANCE.interval.getValue() / 2.0
                            : 0.0
            )
                    - HUD.INSTANCE.yOffset.getValue();
            windowWidth = Wrapper.mc.getWindow().getScaledWidth();

            for (HUD.Info s : list) {
                s.draw(context);
            }
        }

        public static void onUpdate(List<HUD.Info> list, boolean sort) {
            for (HUD.Info s : list) {
                s.onUpdate();
            }

            if (sort) {
                list.sort(Comparator.comparingInt(info -> info.string == null ? 0 : -HUD.INSTANCE.getWidth(info.string)));
            }
        }

        public void onUpdate() {
            this.isOn = this.drawn.getAsBoolean();
            if (this.isOn) {
                try {
                    this.string = HUD.this.lowerCase.getValue() ? ((String)this.info.call()).toLowerCase() : (String)this.info.call();
                } catch (Exception var2) {
                    var2.printStackTrace();
                }
            }
        }

        public void draw(DrawContext context) {
            if (this.currentX > 0.0 || this.isOn) {
                this.currentX = this.animation
                        .get(
                                this.isOn ? HUD.this.getWidth(this.string) + 1 : 0.0,
                                this.isOn ? HUD.this.enableLength.getValueInt() : HUD.this.disableLength.getValueInt(),
                                (Easing)HUD.this.easing.getValue()
                        );
                double width = this.currentX + HUD.this.xOffset.getValueFloat();
                double fade = this.fadeAnimation.get(this.isOn ? 1.0 : 0.0, HUD.this.fadeLength.getValueInt(), (Easing)HUD.this.easing.getValue());
                if (fade > 0.04) {
                    counter = counter + (fromUp ? fade : -fade);
                    int c = ColorUtil.injectAlpha(HUD.this.getColor(counter), (int)(HUD.this.color.getValue().getAlpha() * fade));

                    // 判断当前是否是moduleList
                    boolean isModuleList = HUD.INSTANCE.moduleList.contains(this);

                    if (isModuleList) {
                        // moduleList的绘制逻辑 - 功能文字和图标分开，各自有背景
                        float iconSize = HUD.this.moduleIcon.getValue() ? HUD.this.iconSize.getValueFloat() : 0;
                        float iconSpacing = HUD.this.moduleIcon.getValue() ? HUD.this.iconSpacing.getValueFloat() : 0;
                        float textWidth = HUD.this.getWidth(this.string);

                        // 计算功能文字背景位置（右侧对齐）
                        float textBgX = windowWidth - textWidth - HUD.this.width.getValueFloat() - HUD.this.xOffset.getValueFloat();
                        float textBgY = (float)currentY - HUD.this.interval.getValueFloat() / 2.0F;
                        float textBgWidth = textWidth + HUD.this.width.getValueFloat() * 2;
                        float textBgHeight = (float)fontHeight + HUD.this.interval.getValueFloat();

                        // 计算图标背景位置（在功能文字背景右侧，有间距）
                        float iconBgX = textBgX + textBgWidth + iconSpacing;
                        float iconBgY = textBgY;
                        float iconBgWidth = iconSize + HUD.this.width.getValueFloat() * 2;
                        float iconBgHeight = textBgHeight;

                        // 计算文字位置（在功能文字背景内居中）
                        float textX = textBgX + HUD.this.width.getValueFloat() / 2.0F;
                        float textY = (float) (textBgY + (textBgHeight - fontHeight) / 2.0F);

                        // 计算图标位置（在图标背景内居中）
                        float iconX = iconBgX + HUD.this.width.getValueFloat() / 2.0F;
                        float iconY = iconBgY + (iconBgHeight - iconSize) / 2.0F;

                        // 应用模糊效果 - 功能文字背景
                        if (HUD.this.blur.getValue()) {
                            fentanyl.BLUR
                                    .applyBlur(
                                            (float)(HUD.this.radius.getValue() * fade),
                                            textBgX,
                                            textBgY,
                                            textBgWidth,
                                            textBgHeight
                                    );
                            // 图标背景也应用模糊
                            fentanyl.BLUR
                                    .applyBlur(
                                            (float)(HUD.this.radius.getValue() * fade),
                                            iconBgX,
                                            iconBgY,
                                            iconBgWidth,
                                            iconBgHeight
                                    );
                        }

                        // 绘制功能文字背景
                        if (HUD.this.backGround.getValue()) {
                            int bgColorValue = ColorUtil.injectAlpha(
                                    HUD.this.bgColor.sync ? c : HUD.this.bgColor.getValue().getRGB(),
                                    (int)(HUD.this.bgColor.getValue().getAlpha() * fade)
                            );

                            Render2DUtil.drawRoundedRect(
                                    context.getMatrices(),
                                    textBgX,
                                    textBgY,
                                    textBgWidth,
                                    textBgHeight,
                                    HUD.this.borderRadius.getValueFloat(),
                                    new Color(bgColorValue, true)
                            );

                            // 绘制图标背景
                            Render2DUtil.drawRoundedRect(
                                    context.getMatrices(),
                                    iconBgX,
                                    iconBgY,
                                    iconBgWidth,
                                    iconBgHeight,
                                    HUD.this.borderRadius.getValueFloat(),
                                    new Color(bgColorValue, true)
                            );
                        }

                        // 绘制发光效果 - 功能文字背景
                        if (HUD.this.glow.booleanValue) {
                            Render2DUtil.drawGlow(
                                    context.getMatrices(),
                                    textBgX,
                                    textBgY,
                                    textBgWidth,
                                    textBgHeight,
                                    ColorUtil.injectAlpha(HUD.this.glow.sync ? c : HUD.this.glow.getValue().getRGB(),
                                            (int)(HUD.this.glow.getValue().getAlpha() * fade))
                            );
                            // 图标背景也发光
                            Render2DUtil.drawGlow(
                                    context.getMatrices(),
                                    iconBgX,
                                    iconBgY,
                                    iconBgWidth,
                                    iconBgHeight,
                                    ColorUtil.injectAlpha(HUD.this.glow.sync ? c : HUD.this.glow.getValue().getRGB(),
                                            (int)(HUD.this.glow.getValue().getAlpha() * fade))
                            );
                        }

                        // 绘制功能文字
                        TextUtil.drawString(
                                context,
                                this.string,
                                textX,
                                textY,
                                c,
                                HUD.this.font.getValue(),
                                HUD.this.shadow.getValue()
                        );

                        // 绘制图标
                        if (HUD.this.moduleIcon.getValue()) {
                            float iconRoundness = HUD.this.iconRoundness.getValueFloat();

                            // 绘制图标内容背景（在图标背景内部）
                            if (HUD.this.iconBackground.getValue()) {
                                int iconContentBgColorValue = ColorUtil.injectAlpha(
                                        HUD.this.iconBgColor.getValue().getRGB(),
                                        (int)(HUD.this.iconBgColor.getValue().getAlpha() * fade)
                                );

                                Render2DUtil.drawRoundedRect(
                                        context.getMatrices(),
                                        iconX + 1,
                                        iconY + 1,
                                        iconSize - 2,
                                        iconSize - 2,
                                        iconRoundness,
                                        new Color(iconContentBgColorValue, true)
                                );
                            }

                            // 绘制图标边框
                            if (HUD.this.iconBorder.getValue()) {
                                float borderThickness = HUD.this.borderThickness.getValueFloat();
                                int borderColorValue = ColorUtil.injectAlpha(
                                        HUD.this.borderColor.getValue().getRGB(),
                                        (int)(HUD.this.borderColor.getValue().getAlpha() * fade)
                                );

                                Color borderColorObj = new Color(borderColorValue, true);
                                Render2DUtil.drawRoundedStroke(
                                        context.getMatrices(),
                                        iconX,
                                        iconY,
                                        iconSize,
                                        iconSize,
                                        iconRoundness,
                                        borderColorObj,
                                        16
                                );
                            }

                            try {
                                // 加载图片 - 这里假设你的图片资源路径，请根据实际情况修改
                                Identifier textureId = Identifier.of("fentanyl", "textures/icon/enabled.png");

                                // 计算图片绘制位置和大小
                                float imageX = iconX + 2;  // 稍微向内偏移，使图片更居中
                                float imageY = iconY + 2;
                                float imageSize = iconSize - 4;  // 稍微小于图标背景，留出边距

                                // 使用DrawContext的drawTexture方法绘制图片
                                // 设置透明度
                                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, (float)fade);

                                // 绘制图片
                                context.drawTexture(
                                        textureId,
                                        (int)imageX,
                                        (int)imageY,
                                        0, 0,                    // 纹理的u, v坐标
                                        (int)imageSize,          // 宽度
                                        (int)imageSize,          // 高度
                                        (int)imageSize, (int)imageSize  // 纹理尺寸
                                );

                                // 重置颜色
                                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                            } catch (Exception e) {
                                // 如果图片加载失败，回退到原来的正方形图标
                                int iconColorValue = ColorUtil.injectAlpha(c, (int)(255 * fade));
                                Render2DUtil.drawRoundedRect(
                                        context.getMatrices(),
                                        iconX + 3,
                                        iconY + 3,
                                        iconSize - 6,
                                        iconSize - 6,
                                        Math.max(0, iconRoundness - 2),
                                        new Color(iconColorValue)
                                );
                                e.printStackTrace(); // 可选：打印错误信息
                            }
                        }

                        // 更新Y位置（每个功能独立一行）
                        float totalHeight = Math.max(textBgHeight, iconBgHeight);
                        currentY = currentY + (fromUp ? (totalHeight + HUD.this.interval.getValue()) * fade :
                                -(totalHeight + HUD.this.interval.getValue()) * fade);

                    } else {
                        // infoList的绘制逻辑（原有的连在一起效果）
                        // 计算背景位置和尺寸
                        float bgX = (float)(windowWidth - width - HUD.this.width.getValueFloat() / 2.0F);
                        float bgY = (float)currentY - 1.0F - HUD.this.interval.getValueFloat() / 2.0F;
                        float bgWidth = (float)width + HUD.this.width.getValueFloat() - HUD.this.xOffset.getValueFloat();
                        float bgHeight = (float)fontHeight + HUD.this.interval.getValueFloat();

                        // 应用模糊效果
                        if (HUD.this.blur.getValue()) {
                            fentanyl.BLUR
                                    .applyBlur(
                                            (float)(HUD.this.radius.getValue() * fade),
                                            bgX,
                                            bgY,
                                            bgWidth,
                                            bgHeight
                                    );
                        }

                        // 绘制圆角矩形背景
                        if (HUD.this.backGround.getValue()) {
                            int bgColorValue = ColorUtil.injectAlpha(
                                    HUD.this.bgColor.sync ? c : HUD.this.bgColor.getValue().getRGB(),
                                    (int)(HUD.this.bgColor.getValue().getAlpha() * fade)
                            );

                            Render2DUtil.drawRoundedRect(
                                    context.getMatrices(),
                                    bgX,
                                    bgY,
                                    bgWidth,
                                    bgHeight,
                                    HUD.this.borderRadius.getValueFloat(),
                                    new Color(bgColorValue, true)
                            );
                        }

                        if (HUD.this.glow.booleanValue) {
                            Render2DUtil.drawGlow(
                                    context.getMatrices(),
                                    bgX,
                                    bgY,
                                    bgWidth,
                                    bgHeight,
                                    ColorUtil.injectAlpha(HUD.this.glow.sync ? c : HUD.this.glow.getValue().getRGB(),
                                            (int)(HUD.this.glow.getValue().getAlpha() * fade))
                            );
                        }

                        TextUtil.drawString(
                                context,
                                this.string,
                                windowWidth - width,
                                currentY + HUD.this.textOffset.getValueFloat(),
                                c,
                                HUD.this.font.getValue(),
                                HUD.this.shadow.getValue()
                        );

                        if (HUD.this.rect.booleanValue) {
                            Render2DUtil.drawRect(
                                    context.getMatrices(),
                                    windowWidth + HUD.this.width.getValueFloat() / 2.0F - HUD.this.xOffset.getValueFloat(),
                                    (float)currentY - 1.0F - HUD.this.interval.getValueFloat() / 2.0F,
                                    1.0F,
                                    (float)fontHeight + HUD.this.interval.getValueFloat(),
                                    HUD.this.rect.sync ? c : ColorUtil.injectAlpha(HUD.this.rect.getValue(),
                                            (int)(HUD.this.rect.getValue().getAlpha() * fade)).getRGB()
                            );
                        }

                        currentY = currentY + (fromUp ? (fontHeight + HUD.this.interval.getValue()) * fade :
                                -(fontHeight + HUD.this.interval.getValue()) * fade);
                    }
                }
            }
        }
    }

    public class InitHandler {
        @EventListener
        public void onInit(InitEvent event) {
            for (Module module : fentanyl.MODULE.getModules()) {
                HUD.this.moduleList.add(HUD.this.new Info(module::getArrayName, () -> module.isOn() && module.drawn.getValue()));
            }
            fentanyl.EVENT_BUS.unsubscribe(this);
        }
    }

    public static enum Page {
        General,
        Element,
        Color;
    }
}