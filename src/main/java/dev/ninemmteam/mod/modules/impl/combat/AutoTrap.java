package dev.ninemmteam.mod.modules.impl.combat;

import dev.ninemmteam.fentanyl;
import dev.ninemmteam.api.events.eventbus.EventListener;
import dev.ninemmteam.api.events.impl.UpdateEvent;
import dev.ninemmteam.api.utils.combat.CombatUtil;
import dev.ninemmteam.api.utils.math.PredictUtil;
import dev.ninemmteam.api.utils.math.Timer;
import dev.ninemmteam.api.utils.player.EntityUtil;
import dev.ninemmteam.api.utils.player.InventoryUtil;
import dev.ninemmteam.api.utils.render.EntityVisualUtil;
import dev.ninemmteam.api.utils.render.ModelPlayer;
import dev.ninemmteam.api.utils.world.BlockPosX;
import dev.ninemmteam.api.utils.world.BlockUtil;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.impl.exploit.Blink;
import dev.ninemmteam.mod.modules.impl.render.LogoutSpots;
import dev.ninemmteam.mod.modules.settings.enums.SwingSide;
import dev.ninemmteam.mod.modules.settings.enums.Timing;
import dev.ninemmteam.mod.modules.settings.impl.BooleanSetting;
import dev.ninemmteam.mod.modules.settings.impl.ColorSetting;
import dev.ninemmteam.mod.modules.settings.impl.EnumSetting;
import dev.ninemmteam.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.ArrayList;
import net.minecraft.block.Blocks;
import net.minecraft.block.ConcretePowderBlock;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AutoTrap extends Module {
    public static AutoTrap INSTANCE;

    // General Settings
    public final SliderSetting delay = this.add(new SliderSetting("Delay", 100, 0, 500).setSuffix("ms"));
    private final EnumSetting<TargetMode> targetMod = this.add(new EnumSetting("TargetMode", TargetMode.Single));
    private final EnumSetting<Mode> headMode = this.add(new EnumSetting("BlockForHead", Mode.Anchor));
    final ArrayList<BlockPos> trapList = new ArrayList();
    final ArrayList<BlockPos> placeList = new ArrayList();
    private final Timer timer = new Timer();
    private final SliderSetting placeRange = this.add(new SliderSetting("PlaceRange", 4.0, 1.0, 6.0).setSuffix("m"));
    private final SliderSetting blocksPer = this.add(new SliderSetting("BlocksPer", 1, 1, 8));
    public final SliderSetting predictTicks = this.add(new SliderSetting("PredictTicks", 2.0, 0.0, 50.0, 1.0));
    private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true));
    private final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", true));
    private final SliderSetting range = this.add(new SliderSetting("Range", 5.0, 1.0, 8.0).setSuffix("m"));
    private final BooleanSetting checkMine = this.add(new BooleanSetting("DetectMining", false));
    private final BooleanSetting helper = this.add(new BooleanSetting("Helper", true));
    private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));
    private final BooleanSetting onlyCrawling = this.add(new BooleanSetting("OnlyCrawling", false));
    private final BooleanSetting checkElytra = this.add(new BooleanSetting("CheckElytra", false));
    private final BooleanSetting extend = this.add(new BooleanSetting("Extend", true));
    private final BooleanSetting antiStep = this.add(new BooleanSetting("AntiStep", false));
    private final BooleanSetting onlyBreak = this.add(new BooleanSetting("OnlyBreak", false, this.antiStep::getValue));
    private final BooleanSetting head = this.add(new BooleanSetting("Head", true));
    private final BooleanSetting headExtend = this.add(new BooleanSetting("HeadExtend", true));
    private final BooleanSetting chestUp = this.add(new BooleanSetting("ChestUp", true));
    private final BooleanSetting onlyBreaking = this.add(new BooleanSetting("OnlyBreaking", false, this.chestUp::getValue));
    private final BooleanSetting chest = this.add(new BooleanSetting("Chest", true));
    private final BooleanSetting onlyGround = this.add(new BooleanSetting("OnlyGround", false, this.chest::getValue));
    private final BooleanSetting ignoreCrawling = this.add(new BooleanSetting("IgnoreCrawling", false, this.chest::getValue));
    private final BooleanSetting legs = this.add(new BooleanSetting("Legs", false));
    private final BooleanSetting legAnchor = this.add(new BooleanSetting("LegAnchor", true));
    private final BooleanSetting down = this.add(new BooleanSetting("Down", false));
    private final BooleanSetting onlyHole = this.add(new BooleanSetting("OnlyHole", false));
    private final BooleanSetting breakCrystal = this.add(new BooleanSetting("Break", true));
    private final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", true));
    private final BooleanSetting selfGround = this.add(new BooleanSetting("SelfGround", true));
    private final BooleanSetting logoutSpots = this.add(new BooleanSetting("LogoutSpots", false));
    private final BooleanSetting logoutSpotsOnly = this.add(new BooleanSetting("LogoutSpotsOnly", false, this.logoutSpots::getValue));

    // Page Settings
    private final EnumSetting<Page> page = this.add(new EnumSetting("Page", Page.General));

    // Image Visual Settings (new page)
    private final BooleanSetting enableImageVisual = this.add(
            new BooleanSetting("ImageVisual", true, () -> this.page.getValue() == Page.ImageVisual)
    );

    private final EnumSetting<ImageVisualStyle> imageStyle = this.add(
            new EnumSetting("Style", ImageVisualStyle.TARGET_HIGHLIGHT, () -> this.page.getValue() == Page.ImageVisual)
    );

    private final ColorSetting imageColor = this.add(
            new ColorSetting("ImageColor", new Color(255, 50, 50, 200), () -> this.page.getValue() == Page.ImageVisual)
    );

    private final SliderSetting imageScale = this.add(
            new SliderSetting("Scale", 1.2f, 0.5f, 3.0f, 0.1f, () -> this.page.getValue() == Page.ImageVisual)
    );

    private final SliderSetting imageOpacity = this.add(
            new SliderSetting("Opacity", 1.0f, 0.0f, 1.0f, 0.05f, () -> this.page.getValue() == Page.ImageVisual)
    );

    private final BooleanSetting enableRotation = this.add(
            new BooleanSetting("Rotation", true, () -> this.page.getValue() == Page.ImageVisual)
    );

    private final SliderSetting rotationSpeed = this.add(
            new SliderSetting("RotSpeed", 0.25f, 0.0f, 5.0f, 0.05f, () -> this.page.getValue() == Page.ImageVisual && this.enableRotation.getValue())
    );

    private final EnumSetting<EntityVisualUtil.RotationDirection> rotationDirection = this.add(
            new EnumSetting("RotDirection", EntityVisualUtil.RotationDirection.CLOCKWISE,
                    () -> this.page.getValue() == Page.ImageVisual && this.enableRotation.getValue())
    );

    private final BooleanSetting colorCycle = this.add(
            new BooleanSetting("ColorCycle", true, () -> this.page.getValue() == Page.ImageVisual)
    );

    private final SliderSetting cycleSpeed = this.add(
            new SliderSetting("CycleSpeed", 2.0f, 0.0f, 10.0f, 0.1f, () -> this.page.getValue() == Page.ImageVisual && this.colorCycle.getValue())
    );

    private final SliderSetting xOffset = this.add(
            new SliderSetting("XOffset", 0.0f, -50.0f, 50.0f, 1.0f, () -> this.page.getValue() == Page.ImageVisual)
    );

    private final SliderSetting yOffset = this.add(
            new SliderSetting("YOffset", 0.0f, -50.0f, 50.0f, 1.0f, () -> this.page.getValue() == Page.ImageVisual)
    );

    private final BooleanSetting enableTint = this.add(
            new BooleanSetting("CustomTint", false, () -> this.page.getValue() == Page.ImageVisual)
    );

    private final SliderSetting redTint = this.add(
            new SliderSetting("Red", 1.0f, 0.0f, 1.0f, 0.01f, () -> this.page.getValue() == Page.ImageVisual && this.enableTint.getValue())
    );

    private final SliderSetting greenTint = this.add(
            new SliderSetting("Green", 0.3f, 0.0f, 1.0f, 0.01f, () -> this.page.getValue() == Page.ImageVisual && this.enableTint.getValue())
    );

    private final SliderSetting blueTint = this.add(
            new SliderSetting("Blue", 0.3f, 0.0f, 1.0f, 0.01f, () -> this.page.getValue() == Page.ImageVisual && this.enableTint.getValue())
    );

    // Variables
    public PlayerEntity target;
    int progress = 0;

    // Image visual config instance
    private EntityVisualUtil.ImageVisualConfig imageConfig = new EntityVisualUtil.ImageVisualConfig();

    // Multi-target tracking
    private final ArrayList<PlayerEntity> trackedTargets = new ArrayList<>();

    public AutoTrap() {
        super("AutoTrap", Module.Category.Combat);
        this.setChinese("自动困住");
        INSTANCE = this;
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        this.trapList.clear();
        this.placeList.clear();
        this.progress = 0;
        this.target = null;
        this.trackedTargets.clear(); // Clear tracked targets each update

        if (!this.selfGround.getValue() || mc.player.isOnGround()) {
            if (!this.inventory.getValue() || EntityUtil.inInventory()) {
                if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
                    if (!this.usingPause.getValue() || !mc.player.isUsingItem()) {
                        if (this.timer.passed((long)this.delay.getValue())) {
                            boolean found = false;

                            // 处理正常敌人
                            if (!this.logoutSpotsOnly.getValue()) {
                                if (this.targetMod.getValue() == TargetMode.Single) {
                                    this.target = CombatUtil.getClosestEnemy(this.range.getValue());
                                    if (this.target != null) {
                                        found = true;
                                        this.trackedTargets.add(this.target);
                                        this.trapTarget(this.target);
                                    }
                                } else if (this.targetMod.getValue() == TargetMode.Multi) {
                                    for (PlayerEntity player : CombatUtil.getEnemies(this.range.getValue())) {
                                        found = true;
                                        this.trackedTargets.add(player);
                                        this.target = player;
                                        this.trapTarget(this.target);
                                    }
                                }
                            }

                            // 处理登出位置
                            if (this.logoutSpots.getValue()) {
                                LogoutSpots logoutSpotsModule = null;
                                for (Module module : fentanyl.MODULE.getModules()) {
                                    if (module instanceof LogoutSpots) {
                                        logoutSpotsModule = (LogoutSpots) module;
                                        break;
                                    }
                                }
                                if (logoutSpotsModule != null && logoutSpotsModule.isOn()) {
                                    for (ModelPlayer modelPlayer : logoutSpotsModule.getLogoutCache().values()) {
                                        PlayerEntity player = modelPlayer.player;
                                        if (player != null) {
                                            found = true;
                                            this.trackedTargets.add(player);
                                            this.trapTarget(player);
                                        }
                                    }
                                }
                            }

                            // 自动禁用逻辑
                            if (!found) {
                                if (this.autoDisable.getValue()) {
                                    this.disable();
                                }
                                this.target = null;
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onRender2D(DrawContext context, float tickDelta) {
        // 只在有目标且启用图片视觉时才渲染
        if (this.enableImageVisual.getValue() && !this.trackedTargets.isEmpty()) {
            updateImageConfig();

            // 渲染所有追踪的目标
            for (PlayerEntity target : this.trackedTargets) {
                if (target != null && target.isAlive()) {
                    EntityVisualUtil.drawImageOnEntity(context, target, imageConfig);
                }
            }
        }
    }

    private void trapTarget(PlayerEntity target) {
        if (!this.onlyHole.getValue() || fentanyl.HOLE.isHole(EntityUtil.getEntityPos(target))) {
            if (!this.onlyCrawling.getValue()
                    || target.isCrawling()
                    || this.checkElytra.getValue()
                    && ((ItemStack)target.getInventory().armor.get(2)).getItem() instanceof ElytraItem
                    && (!(mc.player.getY() < target.getY() + 1.0) || target.isFallFlying())) {
                Vec3d playerPos = this.predictTicks.getValue() > 0.0 ? PredictUtil.getPos(target, this.predictTicks.getValueInt()) : target.getPos();
                this.doTrap(target, new BlockPosX(playerPos.getX(), playerPos.getY(), playerPos.getZ()));
            }
        }
    }

    private void doTrap(PlayerEntity player, BlockPos pos) {
        if (pos != null) {
            if (!this.trapList.contains(pos)) {
                this.trapList.add(pos);
                int headOffset = player.isCrawling() ? 1 : 2;
                int chestOffset = player.isCrawling() ? 0 : 1;
                if (this.legs.getValue()) {
                    for (Direction i : Direction.values()) {
                        if (i != Direction.DOWN && i != Direction.UP) {
                            BlockPos offsetPos = pos.offset(i);
                            this.tryPlaceBlock(offsetPos, this.legAnchor.getValue(), false, false);
                            if (BlockUtil.getPlaceSide(offsetPos) == null
                                    && BlockUtil.clientCanPlace(offsetPos, this.breakCrystal.getValue())
                                    && this.getHelper(offsetPos) != null) {
                                this.tryPlaceObsidian(this.getHelper(offsetPos));
                            }
                        }
                    }
                }

                if (this.headExtend.getValue()) {
                    for (int x : new int[]{1, 0, -1}) {
                        for (int z : new int[]{1, 0, -1}) {
                            BlockPos offsetPos = pos.add(z, 0, x);
                            if (this.checkEntity(new BlockPos(offsetPos))) {
                                this.tryPlaceBlock(
                                        offsetPos.up(headOffset),
                                        this.headMode.getValue() == Mode.Anchor,
                                        this.headMode.getValue() == Mode.Concrete,
                                        this.headMode.getValue() == Mode.Web
                                );
                            }
                        }
                    }
                }

                if (this.head.getValue() && BlockUtil.clientCanPlace(pos.up(headOffset), this.breakCrystal.getValue())) {
                    if (BlockUtil.getPlaceSide(pos.up(headOffset)) == null) {
                        boolean trapChest = this.helper.getValue();
                        if (this.getHelper(pos.up(headOffset)) != null) {
                            this.tryPlaceObsidian(this.getHelper(pos.up(headOffset)));
                            trapChest = false;
                        }

                        if (trapChest) {
                            for (Direction ix : Direction.values()) {
                                if (ix != Direction.DOWN && ix != Direction.UP) {
                                    BlockPos offsetPos = pos.offset(ix).up(chestOffset);
                                    if (BlockUtil.isStrictDirection(pos.offset(ix).up(), ix.getOpposite())
                                            && BlockUtil.clientCanPlace(offsetPos.up(chestOffset), this.breakCrystal.getValue())
                                            && BlockUtil.canPlace(offsetPos, this.placeRange.getValue(), this.breakCrystal.getValue())) {
                                        this.tryPlaceObsidian(offsetPos);
                                        trapChest = false;
                                        break;
                                    }
                                }
                            }

                            if (trapChest) {
                                for (Direction ixx : Direction.values()) {
                                    if (ixx != Direction.DOWN && ixx != Direction.UP) {
                                        BlockPos offsetPos = pos.offset(ixx).up(chestOffset);
                                        if (BlockUtil.isStrictDirection(pos.offset(ixx).up(), ixx.getOpposite())
                                                && BlockUtil.clientCanPlace(offsetPos.up(chestOffset), this.breakCrystal.getValue())
                                                && BlockUtil.getPlaceSide(offsetPos) == null
                                                && BlockUtil.clientCanPlace(offsetPos, this.breakCrystal.getValue())
                                                && this.getHelper(offsetPos) != null) {
                                            this.tryPlaceObsidian(this.getHelper(offsetPos));
                                            trapChest = false;
                                            break;
                                        }
                                    }
                                }

                                if (trapChest) {
                                    for (Direction ixxx : Direction.values()) {
                                        if (ixxx != Direction.DOWN && ixxx != Direction.UP) {
                                            BlockPos offsetPos = pos.offset(ixxx).up(chestOffset);
                                            if (BlockUtil.isStrictDirection(pos.offset(ixxx).up(), ixxx.getOpposite())
                                                    && BlockUtil.clientCanPlace(offsetPos.up(chestOffset), this.breakCrystal.getValue())
                                                    && BlockUtil.getPlaceSide(offsetPos) == null
                                                    && BlockUtil.clientCanPlace(offsetPos, this.breakCrystal.getValue())
                                                    && this.getHelper(offsetPos) != null
                                                    && BlockUtil.getPlaceSide(offsetPos.down()) == null
                                                    && BlockUtil.clientCanPlace(offsetPos.down(), this.breakCrystal.getValue())
                                                    && this.getHelper(offsetPos.down()) != null) {
                                                this.tryPlaceObsidian(this.getHelper(offsetPos.down()));
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    this.tryPlaceBlock(
                            pos.up(headOffset),
                            this.headMode.getValue() == Mode.Anchor,
                            this.headMode.getValue() == Mode.Concrete,
                            this.headMode.getValue() == Mode.Web
                    );
                }

                if (this.antiStep.getValue() && (fentanyl.BREAK.isMining(pos.up(headOffset)) || !this.onlyBreak.getValue())) {
                    if (BlockUtil.getPlaceSide(pos.up(3)) == null
                            && BlockUtil.clientCanPlace(pos.up(3), this.breakCrystal.getValue())
                            && this.getHelper(pos.up(3), Direction.DOWN) != null) {
                        this.tryPlaceObsidian(this.getHelper(pos.up(3)));
                    }
                    this.tryPlaceObsidian(pos.up(3));

                    if (BlockUtil.getPlaceSide(pos.up(4)) == null
                            && BlockUtil.clientCanPlace(pos.up(4), this.breakCrystal.getValue())
                            && this.getHelper(pos.up(4), Direction.DOWN) != null) {
                        this.tryPlaceObsidian(this.getHelper(pos.up(4)));
                    }
                    this.tryPlaceObsidian(pos.up(4));
                }

                if (this.down.getValue()) {
                    BlockPos offsetPos = pos.down();
                    this.tryPlaceObsidian(offsetPos);
                    if (BlockUtil.getPlaceSide(offsetPos) == null
                            && BlockUtil.clientCanPlace(offsetPos, this.breakCrystal.getValue())
                            && this.getHelper(offsetPos) != null) {
                        this.tryPlaceObsidian(this.getHelper(offsetPos));
                    }
                }

                if (this.chestUp.getValue()) {
                    for (Direction ixxxx : Direction.values()) {
                        if (ixxxx != Direction.DOWN && ixxxx != Direction.UP) {
                            BlockPos offsetPos = pos.offset(ixxxx).up(headOffset);
                            if (!this.onlyBreaking.getValue() || fentanyl.BREAK.isMining(pos.up(headOffset))) {
                                this.tryPlaceObsidian(offsetPos);
                                if (BlockUtil.getPlaceSide(offsetPos) == null && BlockUtil.clientCanPlace(offsetPos, this.breakCrystal.getValue())) {
                                    if (this.getHelper(offsetPos) != null) {
                                        this.tryPlaceObsidian(this.getHelper(offsetPos));
                                    } else if (BlockUtil.getPlaceSide(offsetPos.down()) == null
                                            && BlockUtil.clientCanPlace(offsetPos.down(), this.breakCrystal.getValue())
                                            && this.getHelper(offsetPos.down()) != null) {
                                        this.tryPlaceObsidian(this.getHelper(offsetPos.down()));
                                    }
                                }
                            }
                        }
                    }
                }

                if (this.chest.getValue()
                        && (!this.onlyGround.getValue() || this.target.isOnGround())
                        && (!this.ignoreCrawling.getValue() || !this.target.isCrawling())) {
                    for (Direction ixxxxx : Direction.values()) {
                        if (ixxxxx != Direction.DOWN && ixxxxx != Direction.UP) {
                            BlockPos offsetPos = pos.offset(ixxxxx).up(chestOffset);
                            this.tryPlaceObsidian(offsetPos);
                            if (BlockUtil.getPlaceSide(offsetPos) == null && BlockUtil.clientCanPlace(offsetPos, this.breakCrystal.getValue())) {
                                if (this.getHelper(offsetPos) != null) {
                                    this.tryPlaceObsidian(this.getHelper(offsetPos));
                                } else if (BlockUtil.getPlaceSide(offsetPos.down()) == null
                                        && BlockUtil.clientCanPlace(offsetPos.down(), this.breakCrystal.getValue())
                                        && this.getHelper(offsetPos.down()) != null) {
                                    this.tryPlaceObsidian(this.getHelper(offsetPos.down()));
                                }
                            }
                        }
                    }
                }

                if (this.extend.getValue()) {
                    for (int x : new int[]{1, 0, -1}) {
                        for (int zx : new int[]{1, 0, -1}) {
                            BlockPos offsetPos = pos.add(x, 0, zx);
                            if (this.checkEntity(new BlockPos(offsetPos))) {
                                this.doTrap(player, offsetPos);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public String getInfo() {
        return this.target != null ? this.target.getName().getString() : null;
    }

    public BlockPos getHelper(BlockPos pos) {
        if (!this.helper.getValue()) {
            return null;
        } else {
            for (Direction i : Direction.values()) {
                if ((!this.checkMine.getValue() || !fentanyl.BREAK.isMining(pos.offset(i)))
                        && BlockUtil.isStrictDirection(pos.offset(i), i.getOpposite())
                        && BlockUtil.canPlace(pos.offset(i), this.placeRange.getValue(), this.breakCrystal.getValue())) {
                    return pos.offset(i);
                }
            }

            return null;
        }
    }

    public BlockPos getHelper(BlockPos pos, Direction ignore) {
        if (!this.helper.getValue()) {
            return null;
        } else {
            for (Direction i : Direction.values()) {
                if (i != ignore
                        && (!this.checkMine.getValue() || !fentanyl.BREAK.isMining(pos.offset(i)))
                        && BlockUtil.isStrictDirection(pos.offset(i), i.getOpposite())
                        && BlockUtil.canPlace(pos.offset(i), this.placeRange.getValue(), this.breakCrystal.getValue())) {
                    return pos.offset(i);
                }
            }

            return null;
        }
    }

    private boolean checkEntity(BlockPos pos) {
        if (mc.player.getBoundingBox().intersects(new Box(pos))) {
            return false;
        } else {
            for (Entity entity : fentanyl.THREAD.getPlayers()) {
                if (entity.getBoundingBox().intersects(new Box(pos)) && entity.isAlive()) {
                    return true;
                }
            }

            return false;
        }
    }

    private void tryPlaceBlock(BlockPos pos, boolean anchor, boolean sand, boolean web) {
        if (!this.placeList.contains(pos)) {
            if (!fentanyl.BREAK.isMining(pos)) {
                if (BlockUtil.canPlace(pos, 6.0, this.breakCrystal.getValue())) {
                    if (this.progress < this.blocksPer.getValue()) {
                        if (!(MathHelper.sqrt((float)mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos())) > this.placeRange.getValue())) {
                            int old = mc.player.getInventory().selectedSlot;
                            int block = sand
                                    ? this.getConcrete()
                                    : (
                                    web
                                            ? (this.getWeb() != -1 ? this.getWeb() : this.getBlock())
                                            : (anchor && this.getAnchor() != -1 ? this.getAnchor() : this.getBlock())
                            );
                            if (block != -1) {
                                this.placeList.add(pos);
                                CombatUtil.attackCrystal(pos, this.rotate.getValue(), this.usingPause.getValue());
                                this.doSwap(block);
                                BlockUtil.placeBlock(pos, this.rotate.getValue());
                                if (this.inventory.getValue()) {
                                    this.doSwap(block);
                                    EntityUtil.syncInventory();
                                } else {
                                    this.doSwap(old);
                                }

                                this.timer.reset();
                                this.progress++;
                            }
                        }
                    }
                }
            }
        }
    }

    private void tryPlaceObsidian(BlockPos pos) {
        if (pos != null) {
            if (!this.placeList.contains(pos)) {
                if (!fentanyl.BREAK.isMining(pos)) {
                    if (BlockUtil.canPlace(pos, 6.0, this.breakCrystal.getValue())) {
                        if (this.progress < this.blocksPer.getValue()) {
                            if (!(MathHelper.sqrt((float)mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos())) > this.placeRange.getValue())) {
                                int old = mc.player.getInventory().selectedSlot;
                                int block = this.getBlock();
                                if (block != -1) {
                                    BlockUtil.placedPos.add(pos);
                                    this.placeList.add(pos);
                                    CombatUtil.attackCrystal(pos, this.rotate.getValue(), this.usingPause.getValue());
                                    this.doSwap(block);
                                    BlockUtil.placeBlock(pos, this.rotate.getValue());
                                    if (this.inventory.getValue()) {
                                        this.doSwap(block);
                                        EntityUtil.syncInventory();
                                    } else {
                                        this.doSwap(old);
                                    }

                                    this.timer.reset();
                                    this.progress++;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void doSwap(int slot) {
        if (this.inventory.getValue()) {
            InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
        } else {
            InventoryUtil.switchToSlot(slot);
        }
    }

    private int getBlock() {
        // 优先级：哭泣黑曜石 > 黑曜石
        int cryingObsidian = this.inventory.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.CRYING_OBSIDIAN) : InventoryUtil.findBlock(Blocks.CRYING_OBSIDIAN);
        if (cryingObsidian != -1) {
            return cryingObsidian;
        }

        return this.inventory.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN) : InventoryUtil.findBlock(Blocks.OBSIDIAN);
    }

    private int getConcrete() {
        return this.inventory.getValue() ? InventoryUtil.findClassInventorySlot(ConcretePowderBlock.class) : InventoryUtil.findClass(ConcretePowderBlock.class);
    }

    private int getWeb() {
        return this.inventory.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.COBWEB) : InventoryUtil.findBlock(Blocks.COBWEB);
    }

    private int getAnchor() {
        return this.inventory.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.RESPAWN_ANCHOR) : InventoryUtil.findBlock(Blocks.RESPAWN_ANCHOR);
    }

    // Update image visual configuration
    private void updateImageConfig() {
        // Apply style
        ImageVisualStyle style = this.imageStyle.getValue();
        switch (style) {
            case TARGET_HIGHLIGHT:
                imageConfig = EntityVisualUtil.PresetStyles.TARGET_HIGHLIGHT;
                break;
            case SIMPLE_BOX:
                imageConfig = EntityVisualUtil.PresetStyles.SIMPLE_BOX;
                break;
            case ENEMY:
                imageConfig = EntityVisualUtil.PresetStyles.ENEMY;
                break;
            case IMPORTANT_TARGET:
                imageConfig = EntityVisualUtil.PresetStyles.IMPORTANT_TARGET;
                break;
            case CUSTOM:
                // Keep current config for customization
                break;
        }

        // Apply custom settings
        imageConfig.scale = this.imageScale.getValueFloat();
        imageConfig.opacity = this.imageOpacity.getValueFloat();
        imageConfig.color = this.imageColor.getValue();
        imageConfig.xOffset = this.xOffset.getValueFloat();
        imageConfig.yOffset = this.yOffset.getValueFloat();
        imageConfig.enableRotation = this.enableRotation.getValue();
        imageConfig.rotationSpeed = this.rotationSpeed.getValueFloat();
        imageConfig.rotationDirection = this.rotationDirection.getValue();
        imageConfig.colorCycle = this.colorCycle.getValue();
        imageConfig.cycleSpeed = this.cycleSpeed.getValueFloat();
        imageConfig.enableTint = this.enableTint.getValue();

        if (this.enableTint.getValue()) {
            imageConfig.redTint = this.redTint.getValueFloat();
            imageConfig.greenTint = this.greenTint.getValueFloat();
            imageConfig.blueTint = this.blueTint.getValueFloat();
        }
    }

    // Page enum
    public enum Page {
        General,
        ImageVisual;
    }

    // Image visual style enum
    public enum ImageVisualStyle {
        TARGET_HIGHLIGHT("Target Highlight"),
        SIMPLE_BOX("Simple Box"),
        ENEMY("Enemy"),
        IMPORTANT_TARGET("Important Target"),
        CUSTOM("Custom");

        private final String name;

        ImageVisualStyle(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private enum Mode {
        Obsidian,
        Anchor,
        Web,
        Concrete;
    }

    public enum TargetMode {
        Single,
        Multi;
    }
}