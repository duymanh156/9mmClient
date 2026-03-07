package dev.ninemmteam.mod.modules.impl.combat;

import dev.ninemmteam.fentanyl;
import dev.ninemmteam.api.events.eventbus.EventListener;
import dev.ninemmteam.api.events.eventbus.EventPriority;
import dev.ninemmteam.api.events.impl.*;
import dev.ninemmteam.api.utils.math.Timer;
import dev.ninemmteam.api.utils.player.EntityUtil;
import dev.ninemmteam.api.utils.player.InventoryUtil;
import dev.ninemmteam.api.utils.render.Render3DUtil;
import dev.ninemmteam.api.utils.world.BlockUtil;
import dev.ninemmteam.api.utils.world.CrystalUtil;
import dev.ninemmteam.api.utils.world.RaytraceUtils;
import dev.ninemmteam.core.impl.HitboxManager;
import dev.ninemmteam.core.impl.PacketManager;
import dev.ninemmteam.core.impl.PriorityManager;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.impl.client.ClientSetting;
import dev.ninemmteam.mod.modules.impl.exploit.Blink;
import dev.ninemmteam.mod.modules.impl.exploit.Phase;
import dev.ninemmteam.mod.modules.impl.movement.ElytraFly;
import dev.ninemmteam.mod.modules.impl.movement.Velocity;
import dev.ninemmteam.mod.modules.impl.player.SpeedMine;
import dev.ninemmteam.mod.modules.settings.enums.SwingSide;
import dev.ninemmteam.mod.modules.settings.impl.BooleanSetting;
import dev.ninemmteam.mod.modules.settings.impl.ColorSetting;
import dev.ninemmteam.mod.modules.settings.impl.EnumSetting;
import dev.ninemmteam.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.EndCrystalItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class CatAura extends Module {
    public static CatAura INSTANCE;

    private final EnumSetting<Page> page = this.add(new EnumSetting<>("Page", Page.Render));

    private final EnumSetting<SortMode> targetSorting = this.add(new EnumSetting<>("Sort", SortMode.Damage, () -> page.is(Page.Calc)));
    private final SliderSetting targetRange = this.add(new SliderSetting("TargetRange", 7.0, 3.0, 20.0, () -> page.is(Page.Calc)));

    private final SliderSetting placeRange = this.add(new SliderSetting("PlaceRange", 6.0, 1.0, 6.0, () -> page.is(Page.Place)));
    private final SliderSetting placeWallsRange = this.add(new SliderSetting("PlaceWallsRange", 3.0, 1.0, 6.0, () -> page.is(Page.Place)));
    private final SliderSetting placeDelay = this.add(new SliderSetting("PlaceDelay", 0, 0, 1000, () -> page.is(Page.Timing)).setSuffix("ms"));
    public final BooleanSetting onePointTwelve = this.add(new BooleanSetting("1.12", false, () -> page.is(Page.Place)));
    private final EnumSetting<AutoSwitchMode> autoSwitch = this.add(new EnumSetting<>("AutoSwitch", AutoSwitchMode.None, () -> page.is(Page.Place)));
    private final BooleanSetting pauseOnEat = this.add(new BooleanSetting("Pause", false, () -> page.is(Page.Place) && autoSwitch.getValue() != AutoSwitchMode.None));
    private final BooleanSetting strictDirection = this.add(new BooleanSetting("StrictDirection", false, () -> page.is(Page.Place)));
    private final BooleanSetting limit = this.add(new BooleanSetting("Limit", false, () -> page.is(Page.Place)));

    public final SliderSetting minDamage = this.add(new SliderSetting("MinDamage", 2.0, 0.0, 36.0, () -> page.is(Page.Calc)));
    private final SliderSetting maxSelfDamage = this.add(new SliderSetting("MaxSelfDamage", 8.0, 0.0, 36.0, () -> page.is(Page.Calc)));
    private final BooleanSetting antiSuicide = this.add(new BooleanSetting("AntiSuicide", true, () -> page.is(Page.Calc)));
    private final SliderSetting lethalCrystals = this.add(new SliderSetting("LethalCrystals", 0, 0, 5, () -> page.is(Page.Calc)));
    private final EnumSetting<MultiTaskMode> multiTask = this.add(new EnumSetting<>("MultiTask", MultiTaskMode.None, () -> page.is(Page.Calc)));
    private final BooleanSetting antiFeetPlace = this.add(new BooleanSetting("AntiFeetPlace", false, () -> page.is(Page.Calc)));
    public final BooleanSetting terrain = this.add(new BooleanSetting("Terrain", false, () -> page.is(Page.Calc)));
    public final BooleanSetting armorAssume = this.add(new BooleanSetting("ArmorAssume", false, () -> page.is(Page.Calc)));
    private final EnumSetting<MiningIgnoreMode> miningIgnore = this.add(new EnumSetting<>("MiningIgnore", MiningIgnoreMode.None, () -> page.is(Page.Calc)));

    private final SliderSetting breakRange = this.add(new SliderSetting("BreakRange", 5.0, 1.0, 6.0, () -> page.is(Page.Break)));
    private final SliderSetting breakWallsRange = this.add(new SliderSetting("BreakWallsRange", 3.0, 1.0, 6.0, () -> page.is(Page.Break)));
    private final SliderSetting breakDelay = this.add(new SliderSetting("BreakDelay", 0, 0, 1000, () -> page.is(Page.Timing)).setSuffix("ms"));
    private final SliderSetting ticksExisted = this.add(new SliderSetting("TicksExisted", 0, 0, 20, () -> page.is(Page.Break)).setSuffix("tick"));
    private final BooleanSetting predict = this.add(new BooleanSetting("Boost", true, () -> page.is(Page.Break)));
    private final BooleanSetting lethalTick = this.add(new BooleanSetting("LethalTick", false, () -> page.is(Page.Break)));
    private final BooleanSetting autoHit = this.add(new BooleanSetting("AutoHit", false, () -> page.is(Page.Break) && lethalTick.getValue()));
    private final SliderSetting dtapDelay = this.add(new SliderSetting("CrystalDelay", 300, 100, 500, () -> page.is(Page.Break) && lethalTick.getValue()).setSuffix("ms"));
    private final BooleanSetting inhibit = this.add(new BooleanSetting("Inhibit", false, () -> page.is(Page.Break)));
    private final EnumSetting<AntiWeaknessMode> antiWeakness = this.add(new EnumSetting<>("AntiWeakness", AntiWeaknessMode.None, () -> page.is(Page.Break)));
    private final EnumSetting<SetDeadMode> setDeadMode = this.add(new EnumSetting<>("SetDead", SetDeadMode.None, () -> page.is(Page.Break)));

    private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true, () -> page.is(Page.Misc)).setParent());
    private final BooleanSetting yawStep = this.add(new BooleanSetting("YawStep", true, () -> page.is(Page.Misc) && rotate.isOpen()).setParent());
    private final BooleanSetting whenElytra = this.add(new BooleanSetting("FallFlying", true, () -> page.is(Page.Misc) && rotate.isOpen() && yawStep.isOpen()));
    private final BooleanSetting checkFov = this.add(new BooleanSetting("OnlyLooking", true, () -> page.is(Page.Misc) && rotate.isOpen() && yawStep.isOpen()));
    private final SliderSetting fov = this.add(new SliderSetting("Fov", 20.0, 0.0, 360.0, 0.1, () -> page.is(Page.Misc) && rotate.isOpen() && yawStep.isOpen() && checkFov.getValue()));
    private final SliderSetting steps = this.add(new SliderSetting("Steps", 0.05, 0.0, 1.0, 0.01, () -> page.is(Page.Misc) && rotate.isOpen() && yawStep.isOpen()));
    private final SliderSetting priority = this.add(new SliderSetting("Priority", 10, 0, 100, () -> page.is(Page.Misc) && rotate.isOpen() && yawStep.isOpen()));
    private final EnumSetting<RotationsType> rotationsType = this.add(new EnumSetting<>("RotationsType", RotationsType.Simple, () -> page.is(Page.Misc) && rotate.getValue() && !yawStep.getValue()));
    private final EnumSetting<SequenceMode> sequence = this.add(new EnumSetting<>("Sequence", SequenceMode.None, () -> page.is(Page.Misc)));
    private final EnumSetting<SwapWaitMode> swapWait = this.add(new EnumSetting<>("SwapDelay", SwapWaitMode.None, () -> page.is(Page.Misc)));
    private final SliderSetting swapWaitDelay = this.add(new SliderSetting("SwapWaitDelay", 300, 50, 500, () -> page.is(Page.Misc) && swapWait.getValue() != SwapWaitMode.None).setSuffix("ms"));
    private final EnumSetting<TimingMode> timing = this.add(new EnumSetting<>("Timing", TimingMode.Soft, () -> page.is(Page.Timing)));

    private final BooleanSetting render = this.add(new BooleanSetting("Render", true, () -> page.is(Page.Render)));
    private final ColorSetting fillColor = this.add(new ColorSetting("FillColor", new Color(0, 0, 0, 100), () -> page.is(Page.Render) && render.getValue()));
    private final ColorSetting outlineColor = this.add(new ColorSetting("OutlineColor", new Color(255, 255, 255, 255), () -> page.is(Page.Render) && render.getValue()));
    private final BooleanSetting renderDamageText = this.add(new BooleanSetting("DamageText", true, () -> page.is(Page.Render) && render.getValue()));
    private final SliderSetting textScale = this.add(new SliderSetting("TextScale", 1.4, 1.0, 2.0, () -> page.is(Page.Render) && render.getValue() && renderDamageText.getValue()));
    private final EnumSetting<RenderMode> renderMode = this.add(new EnumSetting<>("Type", RenderMode.Normal, () -> page.is(Page.Render) && render.getValue()));
    private final SliderSetting fadeTime = this.add(new SliderSetting("FadeTime", 1000, 100, 2000, () -> page.is(Page.Render) && render.getValue() && renderMode.is(RenderMode.Fade)).setSuffix("ms"));
    private final BooleanSetting futureFade = this.add(new BooleanSetting("Future", false, () -> page.is(Page.Render) && render.getValue() && renderMode.is(RenderMode.Fade)));
    private final SliderSetting glideSpeed = this.add(new SliderSetting("GlideSpeed", 5, 1, 40, () -> page.is(Page.Render) && render.getValue() && renderMode.is(RenderMode.Glide)));
    private final SliderSetting openSpeed = this.add(new SliderSetting("OpenSpeed", 5, 1, 40, () -> page.is(Page.Render) && render.getValue() && renderMode.is(RenderMode.Glide)));
    private final SliderSetting boxHeight = this.add(new SliderSetting("BoxHeight", 0.0, -1.0, 2.0, 0.01, () -> page.is(Page.Render) && render.getValue()));
    private final SliderSetting boxSize = this.add(new SliderSetting("BoxSize", 1.0, 0.1, 2.0, 0.01, () -> page.is(Page.Render) && render.getValue()));
    private final SliderSetting boxFlatness = this.add(new SliderSetting("BoxFlatness", 1.0, 0.1, 2.0, 0.01, () -> page.is(Page.Render) && render.getValue()));

    private final Timer placeTimer = new Timer();
    private final Timer breakTimer = new Timer();
    private final Timer swapTimer = new Timer();
    private final Timer autoDtapTimer = new Timer();
    private final Timer cooldownTimer = new Timer();
    private final Timer noPosTimer = new Timer();

    public PlayerEntity target;
    public BlockPos renderPos;
    public BlockPos calcPos;
    public BlockPos lastPos;
    public Entity calcCrystal;
    public double renderDMG;
    public boolean offhand;

    public final Map<Integer, Long> hitCrystals = new ConcurrentHashMap<>();
    private final List<RenderPosTime> oldPlacements = new ArrayList<>();
    protected final LinkedList<Long> brokeCrystals = new LinkedList<>();

    private float[] placeTargetRot = null;
    private float[] breakTargetRot = null;
    private Vec3d directionVec = null;
    private boolean doingAutoDtap = false;
    private boolean didAutoDtapAttack = false;
    private boolean didAutoDtapCrystal = false;
    private boolean hasRotated = false;
    private boolean placeFlag = false;
    private boolean breakFlag = false;
    private int CRYSTALS_PER_SECOND = 0;
    private float lastTime = 0;

    private Box renderBB;
    private BlockPos lastRenderPos;
    private float timePassed;
    private long startTime = 0;

    private final DecimalFormat df = new DecimalFormat("0.##");

    public CatAura() {
        super("9mmAura", Category.Combat);
        this.setChinese("9mmAura");
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (nullCheck()) return;
        renderPos = null;
        target = null;
        swapTimer.setDelay(swapWaitDelay.getValueInt());
        calcPos = null;
        calcCrystal = null;
        breakTargetRot = null;
        placeTargetRot = null;
        hitCrystals.clear();
        oldPlacements.clear();
        brokeCrystals.clear();
        placeTimer.resetDelay();
        breakTimer.resetDelay();
        doingAutoDtap = false;
        didAutoDtapAttack = false;
        didAutoDtapCrystal = false;
        hasRotated = false;
        renderBB = null;
        lastRenderPos = null;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (nullCheck()) return;
        renderPos = null;
        target = null;
        swapTimer.setDelay(swapWaitDelay.getValueInt());
        calcPos = null;
        calcCrystal = null;
        breakTargetRot = null;
        placeTargetRot = null;
        directionVec = null;
        if (hasRotated) {
            fentanyl.ROTATION.snapBack();
            hasRotated = false;
        }
    }

    @Override
    public String getInfo() {
        if (target == null) return null;
        return df.format(renderDMG);
    }

    String lastTargetName = null;

    public String getHudInfo() {
        if (target == null && lastTargetName == null)
            return "0.0ms, 1, " + df.format(renderDMG) + ", " + CRYSTALS_PER_SECOND;

        if (target != null)
            lastTargetName = target.getName().getString();
        return df.format(lastTime) + "ms, " + (calcPos != null ? breakTimer.getMs() : 1) + ", " + df.format(renderDMG) + ", " + CRYSTALS_PER_SECOND;
    }

    @EventListener
    public void onEntityRemove(RemoveEntityEvent event) {
        if (event.getEntity() instanceof EndCrystalEntity) {
            if (hitCrystals.containsKey(event.getEntity().getId())) {
                brokeCrystals.add(System.currentTimeMillis());
                hitCrystals.remove(event.getEntity().getId());
            }
        }
    }

    @EventListener
    public void onRotate(RotationEvent event) {
        if (rotate.getValue()
            && shouldYawStep()
            && directionVec != null
            && target != null
            && !noPosTimer.passed(1000L)
            && !shouldReturn()) {
            event.setTarget(directionVec, steps.getValueFloat(), priority.getValueFloat());
        }
    }

    private boolean shouldReturn() {
        if (!Blink.INSTANCE.isOn()) {
            return false;
        }
        return true;
    }

    private boolean shouldYawStep() {
        return (whenElytra.getValue() || !mc.player.isFallFlying() && (!ElytraFly.INSTANCE.isOn() || !ElytraFly.INSTANCE.isFallFlying())) && yawStep.getValue() && !Velocity.INSTANCE.noRotation();
    }

    private boolean faceVector(Vec3d directionVec) {
        if (directionVec == null) {
            return false;
        } else if (!shouldYawStep()) {
            fentanyl.ROTATION.lookAt(directionVec);
            return true;
        } else {
            this.directionVec = directionVec;
            return fentanyl.ROTATION.inFov(directionVec, fov.getValueFloat()) ? true : !checkFov.getValue();
        }
    }

    @EventListener
    public void onEntityAdd(EntitySpawnedEvent event) {
        if (nullCheck()) return;
        if (rotate.getValue() && PriorityManager.INSTANCE.isUsageLocked()) return;
        if (isMultiTask()) return;
        if (predict.getValue() && event.getEntity() instanceof EndCrystalEntity entity) {
            doPredict(entity);
        }
    }

    @EventListener
    public void onPacketSend(PacketEvent.Send event) {
        if (nullCheck()) return;

        if (event.getPacket() instanceof UpdateSelectedSlotC2SPacket packet) {
            if (swapWait.getValue() != SwapWaitMode.None) {
                StatusEffectInstance weaknessEffect = mc.player.getStatusEffect(StatusEffects.WEAKNESS);
                StatusEffectInstance strengthEffect = mc.player.getStatusEffect(StatusEffects.STRENGTH);

                if (antiWeakness.getValue() != AntiWeaknessMode.None && 
                    (weaknessEffect != null && (strengthEffect == null || strengthEffect.getAmplifier() <= weaknessEffect.getAmplifier()))) {
                    return;
                }
                swapTimer.resetDelay();
            }
        }
    }

    public void doPredict(EndCrystalEntity entity) {
        noPosTimer.reset();
        if (mc.player.squaredDistanceTo(entity.getX(), entity.getY(), entity.getZ()) <= breakRange.getValue() * breakRange.getValue()) {
            if (lethalTick.getValue() && isDtapping()) return;

            if (target != null) {
                final float targetDamage = calculateDamage(target, new Vec3d(entity.getX(), entity.getY(), entity.getZ()), terrain.getValue(), false);
                if ((targetDamage > minDamage.getValueFloat()) || 
                    (lethalCrystals.getValueInt() != 0 && (targetDamage * lethalCrystals.getValueFloat() >= target.getHealth() + target.getAbsorptionAmount()))) {

                    if (rotate.getValue() && !shouldYawStep()) {
                        Vec3d breakVec = new Vec3d(entity.getX(), entity.getEyeY(), entity.getZ());
                        if (!faceVector(breakVec)) {
                            if (rotationsType.is(RotationsType.NCP)) {
                                Vec3d adjustedBreakVec = entity.getPos().add(0, 0.5, 0);
                                if (!faceVector(adjustedBreakVec)) {
                                    return;
                                }
                            } else {
                                return;
                            }
                        }
                    }

                    PlayerInteractEntityC2SPacket interactentity = PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking());
                    PacketManager.INSTANCE.sendPacket(interactentity);

                    mc.player.swingHand(offhand ? Hand.OFF_HAND : Hand.MAIN_HAND);
                    mc.player.resetLastAttackedTicks();
                    hitCrystals.put(entity.getId(), System.currentTimeMillis());
                    breakTimer.resetDelay();
                    noPosTimer.reset();

                    if (doingAutoDtap) {
                        didAutoDtapCrystal = true;
                    }

                    if (rotate.getValue() && !shouldYawStep()) {
                        fentanyl.ROTATION.snapBack();
                    }

                    if (sequence.getValue() == SequenceMode.Strong) {
                        int crystalSlot = InventoryUtil.findItem(Items.END_CRYSTAL);
                        if (crystalSlot == -1 && !offhand) {
                            return;
                        }

                        if (calcPos != null) {
                            placeCrystal();
                        }
                    }
                }
            }
        }
    }

    boolean isMultiTask() {
        if (mc.player.isUsingItem()) {
            switch (multiTask.getValue()) {
                case Soft:
                    if (!mc.player.getActiveHand().equals(Hand.OFF_HAND) && !offhand) {
                        return true;
                    }
                    break;
                case Strong:
                    return true;
            }
        }
        return false;
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        if (nullCheck()) return;
        if (!render.getValue()) return;

        if (!renderMode.is(RenderMode.Fade)) {
            if (target == null || renderPos == null) {
                return;
            }
        }

        if (!renderMode.is(RenderMode.Fade) && !canRender()) return;

        Color fillColorVal = fillColor.getValue();
        Color lineColorVal = outlineColor.getValue();
        
        double size = boxSize.getValue();
        double height = boxHeight.getValue();
        double flatness = boxFlatness.getValue();

        switch (renderMode.getValue()) {
            case Normal:
                Box normalBox = new Box(renderPos);
                normalBox = normalBox.expand(size - 1, 0, size - 1);
                double centerY = (normalBox.minY + normalBox.maxY) / 2.0;
                double halfHeight = (normalBox.maxY - normalBox.minY) / 2.0 * flatness;
                normalBox = new Box(normalBox.minX, centerY - halfHeight, normalBox.minZ, normalBox.maxX, centerY + halfHeight, normalBox.maxZ);
                normalBox = normalBox.offset(0, height, 0);
                Render3DUtil.drawFill(matrixStack, normalBox, fillColorVal);
                Render3DUtil.drawBox(matrixStack, normalBox, lineColorVal, 1.5f);
                if (renderDamageText.getValue() && renderDMG > 0) {
                    Render3DUtil.drawText3D(String.format("%.1f", renderDMG), normalBox.getCenter(), Color.WHITE);
                }
                break;
            case Glide:
                if (lastRenderPos == null || mc.player.squaredDistanceTo(renderBB.minX, renderBB.minY, renderBB.minZ) > targetRange.getValue() * targetRange.getValue()) {
                    lastRenderPos = renderPos;
                    renderBB = new Box(renderPos);
                    timePassed = 0.0f;
                    startTime = System.currentTimeMillis();
                }
                if (!lastRenderPos.equals(renderPos)) {
                    lastRenderPos = renderPos;
                    timePassed = 0.0f;
                }

                final double xDiff = renderPos.getX() - renderBB.minX;
                final double yDiff = renderPos.getY() - renderBB.minY;
                final double zDiff = renderPos.getZ() - renderBB.minZ;
                float movespeed = glideSpeed.getValueInt() * 200;
                float decellerate = 0.8f;
                float multiplier = timePassed / movespeed * decellerate;
                if (multiplier > 1.0f) {
                    multiplier = 1.0f;
                }
                renderBB = renderBB.offset(xDiff * multiplier, yDiff * multiplier, zDiff * multiplier);
                
                Box glideBox = renderBB;
                glideBox = glideBox.expand(size - 1, 0, size - 1);
                double glideCenterY = (glideBox.minY + glideBox.maxY) / 2.0;
                double glideHalfHeight = (glideBox.maxY - glideBox.minY) / 2.0 * flatness;
                glideBox = new Box(glideBox.minX, glideCenterY - glideHalfHeight, glideBox.minZ, glideBox.maxX, glideCenterY + glideHalfHeight, glideBox.maxZ);
                glideBox = glideBox.offset(0, height, 0);

                Render3DUtil.drawFill(matrixStack, glideBox, fillColorVal);
                Render3DUtil.drawBox(matrixStack, glideBox, lineColorVal, 1.5f);

                if (renderDamageText.getValue() && renderDMG > 0) {
                    Render3DUtil.drawText3D(String.format("%.1f", renderDMG), glideBox.getCenter(), Color.WHITE);
                }

                if (renderBB.equals(new Box(renderPos))) {
                    timePassed = 0.0f;
                } else {
                    timePassed += 50.0f;
                }
                break;
        }
    }

    public Result getTargetResult() {
        switch (targetSorting.getValue()) {
            case Damage:
                List<PlayerEntity> targets = new ArrayList<>();
                for (Entity entity : mc.world.getEntities()) {
                    if (entity instanceof PlayerEntity player) {
                        if (isValidTarget(player)) {
                            targets.add(player);
                        }
                    }
                }
                Result bestResult = null;
                for (PlayerEntity t : targets) {
                    Result currentResult = getResult(t);
                    if (bestResult == null) {
                        bestResult = currentResult;
                        continue;
                    }
                    if (currentResult != null && currentResult.getDamage() > bestResult.getDamage()) {
                        bestResult = currentResult;
                    }
                }
                if (bestResult == null) {
                    reset();
                    return null;
                }
                return bestResult;
            case Range:
                PlayerEntity closestTarget = null;
                double closestDistance = Double.MAX_VALUE;
                for (Entity entity : mc.world.getEntities()) {
                    if (entity instanceof PlayerEntity player) {
                        if (isValidTarget(player)) {
                            double distance = mc.player.distanceTo(player);
                            if (distance < closestDistance) {
                                closestDistance = distance;
                                closestTarget = player;
                            }
                        }
                    }
                }
                if (closestTarget == null) {
                    reset();
                    return null;
                }
                target = closestTarget;
                return getResult(target);
        }
        return null;
    }

    public void prepare() {
        offhand = mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL;

        placeTimer.setDelay(placeDelay.getValueInt());
        breakTimer.setDelay(breakDelay.getValueInt());
        autoDtapTimer.setDelay(dtapDelay.getValueInt());
        swapTimer.setDelay(swapWaitDelay.getValueInt());
        cooldownTimer.setDelay(800L);

        placeTargetRot = null;
        breakTargetRot = null;

        long nanoPre = System.nanoTime();
        Result result = getTargetResult();
        long nanoPost = System.nanoTime() - nanoPre;
        lastTime = (float) (nanoPost / 1E6);

        calcPos = null;
        calcCrystal = null;
        load(result);

        if (inhibit.getValue() && calcCrystal != null) {
            if (hitCrystals.containsKey(calcCrystal.getId())) {
                breakTimer.setDelay(breakDelay.getValueInt() + 100);
            }
        }
        if (!rotate.getValue()) return;

        if (shouldYawStep()) return;

        if (autoSwitch.getValue() == AutoSwitchMode.None && breakTargetRot == null) {
            if (!offhand && mc.player.getMainHandStack().getItem() != Items.END_CRYSTAL) {
                return;
            }
        }

        if (target != null && EntityUtil.isBoostedByFirework() && !target.isFallFlying()) {
            return;
        }

        if (!rotationsType.is(RotationsType.Silent)) {
            if (placeTargetRot != null && breakTargetRot == null) {
                fentanyl.ROTATION.snapAt(placeTargetRot[0], placeTargetRot[1]);
            } else if (placeTargetRot == null && breakTargetRot != null) {
                fentanyl.ROTATION.snapAt(breakTargetRot[0], breakTargetRot[1]);
            } else if (placeTargetRot != null) {
                handleSequence();
            }
        }
    }

    public boolean breakCrystal() {
        noPosTimer.reset();
        if (breakFlag) {
            breakFlag = false;
            return true;
        }

        if (lethalTick.getValue() && target != null && (calcCrystal != null || calcPos != null)) {
            if (doingAutoDtap) {
                if (didAutoDtapAttack && !didAutoDtapCrystal && !autoDtapTimer.passed()) {
                    return true;
                }
                if (target.hurtTime != 0 && didAutoDtapCrystal && didAutoDtapAttack) {
                    return true;
                } else if (didAutoDtapAttack && didAutoDtapCrystal) {
                    doingAutoDtap = false;
                    didAutoDtapAttack = false;
                    didAutoDtapCrystal = false;
                }
            }
        } else {
            if (autoDtapTimer.passed()) {
                doingAutoDtap = false;
                didAutoDtapAttack = false;
                didAutoDtapCrystal = false;
            }
        }

        if (calcCrystal != null) {
            if (breakTimer.passed()) {
                if (swapWait.getValue() != SwapWaitMode.None && !swapTimer.passed()) return true;

                StatusEffectInstance weaknessEffect = mc.player.getStatusEffect(StatusEffects.WEAKNESS);
                StatusEffectInstance strengthEffect = mc.player.getStatusEffect(StatusEffects.STRENGTH);
                boolean swapBack = false;
                int curSlot = mc.player.getInventory().selectedSlot;
                if (antiWeakness.getValue() != AntiWeaknessMode.None && 
                    (weaknessEffect != null && (strengthEffect == null || strengthEffect.getAmplifier() <= weaknessEffect.getAmplifier()))) {
                    int bestWeapon = InventoryUtil.getSwordSlot();
                    if (bestWeapon != -1 && mc.player.getInventory().selectedSlot != bestWeapon) {
                        switch (antiWeakness.getValue()) {
                            case Normal:
                                InventoryUtil.switchToSlot(bestWeapon);
                                break;
                            case Silent:
                                InventoryUtil.switchToSlot(bestWeapon);
                                swapBack = true;
                                break;
                        }
                    }
                }

                if (rotate.getValue() && !shouldYawStep()) {
                    Vec3d breakVec = new Vec3d(calcCrystal.getX(), calcCrystal.getEyeY(), calcCrystal.getZ());
                    if (!faceVector(breakVec)) {
                        if (rotationsType.is(RotationsType.NCP)) {
                            Vec3d adjustedBreakVec = calcCrystal.getPos().add(0, 0.5, 0);
                            if (!faceVector(adjustedBreakVec)) {
                                return false;
                            }
                        } else {
                            return false;
                        }
                    }
                }

                if (shouldYawStep()) {
                    Vec3d breakVec = new Vec3d(calcCrystal.getX(), calcCrystal.getEyeY(), calcCrystal.getZ());
                    this.directionVec = breakVec;
                }

                PlayerInteractEntityC2SPacket packet = PlayerInteractEntityC2SPacket.attack(calcCrystal, mc.player.isSneaking());
                PacketManager.INSTANCE.sendPacket(packet);
                mc.player.swingHand(offhand ? Hand.OFF_HAND : Hand.MAIN_HAND);
                mc.player.resetLastAttackedTicks();
                if (!mc.player.isOnGround()) {
                    mc.player.addCritParticles(calcCrystal);
                }

                hitCrystals.put(calcCrystal.getId(), System.currentTimeMillis());
                noPosTimer.reset();

                if (swapBack) {
                    InventoryUtil.switchToSlot(curSlot);
                }

                breakTimer.reset();
                if (setDeadMode.getValue() == SetDeadMode.Ghost) {
                    mc.executeSync(() -> {
                        mc.world.removeEntity(calcCrystal.getId(), Entity.RemovalReason.KILLED);
                    });
                }

                if (doingAutoDtap && didAutoDtapAttack && !didAutoDtapCrystal) {
                    didAutoDtapCrystal = true;
                }

                if (rotate.getValue() && !shouldYawStep()) {
                    fentanyl.ROTATION.snapBack();
                }

                return false;
            }
            return true;
        } else {
            return true;
        }
    }

    boolean isDtapping() {
        if (doingAutoDtap) {
            if (didAutoDtapAttack && didAutoDtapCrystal && autoDtapTimer.passed()) {
                return false;
            }
            return true;
        }
        return false;
    }

    boolean placedOnspawn = false;

    public void placeCrystal() {
        noPosTimer.reset();
        if (placeFlag) {
            placeFlag = false;
            return;
        }

        if (placedOnspawn) return;

        if (calcPos != null) {
            if (placeTimer.passed(placeDelay.getValueInt())) {
                if (swapWait.getValue() == SwapWaitMode.Full && !swapTimer.passed(swapWaitDelay.getValueInt())) return;

                boolean doSilent = false;
                int crystalSlot = InventoryUtil.findItem(Items.END_CRYSTAL);

                if (crystalSlot == -1 && !offhand) {
                    renderPos = null;
                    return;
                }

                if (!offhand && autoSwitch.getValue() == AutoSwitchMode.Normal) {
                    if (pauseOnEat.getValue()) {
                        if (!isEatingGap()) {
                            InventoryUtil.switchToSlot(crystalSlot);
                        }
                    } else {
                        InventoryUtil.switchToSlot(crystalSlot);
                    }
                }

                if (!offhand && (autoSwitch.getValue() == AutoSwitchMode.Silent || autoSwitch.getValue() == AutoSwitchMode.SilentBypass)) {
                    if (!(mc.player.getMainHandStack().getItem() instanceof EndCrystalItem))
                        doSilent = true;
                }

                if (autoSwitch.getValue() != AutoSwitchMode.Silent && autoSwitch.getValue() != AutoSwitchMode.SilentBypass) {
                    if (!(mc.player.getMainHandStack().getItem() instanceof EndCrystalItem) && !offhand) {
                        renderPos = null;
                        return;
                    }
                }

                int oldSlot = mc.player.getInventory().selectedSlot;

                if (doSilent) {
                    if (autoSwitch.getValue() == AutoSwitchMode.SilentBypass) {
                        InventoryUtil.switchToBypass(InventoryUtil.hotbarToInventory(crystalSlot), false);
                    } else {
                        InventoryUtil.switchToSlot(crystalSlot);
                    }
                }

                Direction side = getPlaceableSideCrystal(calcPos, strictDirection.getValue());

                if (side != null) {
                    Vec3d vec = calcPos.toCenterPos().add(side.getVector().getX() * 0.5, side.getVector().getY() * 0.5, side.getVector().getZ() * 0.5);
                    if (side.getAxis().isHorizontal()) {
                        vec = vec.add(0, 0.45, 0);
                    }

                    if (rotate.getValue() && !shouldYawStep()) {
                        if (!faceVector(vec)) {
                            if (rotationsType.is(RotationsType.NCP)) {
                                Vec3d adjustedVec = calcPos.toCenterPos().add(0, 0.5, 0);
                                if (!faceVector(adjustedVec)) {
                                    return;
                                }
                            } else {
                                return;
                            }
                        }
                    }

                    if (shouldYawStep()) {
                        this.directionVec = vec;
                    }

                    BlockHitResult result = new BlockHitResult(vec, side, calcPos, false);

                    Module.sendSequencedPacket(id -> new PlayerInteractBlockC2SPacket(offhand ? Hand.OFF_HAND : Hand.MAIN_HAND, result, id));
                    mc.player.swingHand(offhand ? Hand.OFF_HAND : Hand.MAIN_HAND);
                    noPosTimer.reset();
                    if (doSilent) {
                        if (autoSwitch.getValue() == AutoSwitchMode.SilentBypass) {
                            InventoryUtil.switchToBypass(InventoryUtil.hotbarToInventory(crystalSlot), true);
                        } else {
                            InventoryUtil.switchToSlot(oldSlot);
                        }
                    }

                    lastPos = calcPos;
                    placeTimer.resetDelay();

                    if (rotate.getValue() && !shouldYawStep()) {
                        fentanyl.ROTATION.snapBack();
                    }
                }
            }
            renderPos = calcPos;
            if (renderPos != null && renderMode.is(RenderMode.Fade)) {
                oldPlacements.removeIf(pair -> pair.pos.equals(renderPos));
                oldPlacements.add(new RenderPosTime(renderPos, System.currentTimeMillis(), renderDMG));
            }
        }
    }

    public PlaceResult getBestPlaceResult(PlayerEntity targetPlayer, Entity currentCalcCrystal) {
        BlockPos bestPos = null;
        BlockPos bestAntiFeetPlacePos = null;
        double bestAntiFeetPlaceDamage = 0.5D;

        final List<BlockPos> sphere = BlockUtil.sphere(placeRange.getValue() + 1.0f, mc.player.getBlockPos(), true, false);
        double bestDMG = 0.5D;

        for (final BlockPos pos : sphere) {
            if (CrystalUtil.canPlaceCrystal(pos, onePointTwelve.getValue())) {
                AntiFeetPlaceResult feetPlaceResult = handleBBCrystal(pos, lastPos, currentCalcCrystal);

                if (!feetPlaceResult.isPlaceAvailable()) continue;

                if (!checkPlace(pos, false)) continue;

                boolean doMiningIgnore = getMiningIgnore();
                final float targetDamage = calculateDamage(targetPlayer, crystalDamageVec(pos), terrain.getValue(), doMiningIgnore);
                if (bestDMG < targetDamage && targetDamage > minDamage.getValueFloat()) {
                    final float selfDamage = calculateDamage(mc.player, crystalDamageVec(pos), terrain.getValue(), doMiningIgnore);

                    if (selfDamage > maxSelfDamage.getValueFloat()) continue;

                    if ((selfDamage + 2 > mc.player.getHealth() + mc.player.getAbsorptionAmount() && antiSuicide.getValue())) {
                        continue;
                    }

                    if (feetPlaceResult.isAntiFeetPlace()) {
                        bestAntiFeetPlaceDamage = targetDamage;
                        bestAntiFeetPlacePos = pos;
                    } else {
                        bestDMG = targetDamage;
                        bestPos = pos;
                    }
                }
            }
        }
        if (bestDMG == 0.5D) {
            bestDMG = 0;
        }

        if (bestPos == null && bestAntiFeetPlacePos != null) {
            bestPos = bestAntiFeetPlacePos;
            bestDMG = bestAntiFeetPlaceDamage;
        }
        return new PlaceResult(targetPlayer, bestDMG, bestPos, getPlaceRot(bestPos));
    }

    public boolean getMiningIgnore() {
        return switch (miningIgnore.getValue()) {
            case Ignore -> true;
            case StrictIgnore -> offhand || (mc.player.getMainHandStack().getItem() instanceof EndCrystalItem);
            default -> false;
        };
    }

    public Result getResult(PlayerEntity player) {
        BreakResult breakResult = getBestBreakResult(player);
        Entity currentCalcCrystal = breakResult.calcCrystal;
        return new Result(breakResult, getBestPlaceResult(player, currentCalcCrystal));
    }

    public boolean checkPlace(BlockPos pos, boolean ignore) {
        Direction side = getPlaceableSideCrystal(pos, strictDirection.getValue());

        if (side == null) return false;

        Vec3d vec = pos.toCenterPos().add(side.getVector().getX() * 0.5, side.getVector().getY() * 0.5, side.getVector().getZ() * 0.5);

        double distanceSq = vec.squaredDistanceTo(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        if (distanceSq > placeRange.getValue() * placeRange.getValue()) return false;

        boolean trace = isBehindWall(pos, vec);
        if (trace && distanceSq > placeWallsRange.getValue() * placeWallsRange.getValue()) return false;

        double crystalDistance = mc.player.getEyePos().distanceTo(pos.toCenterPos().add(0, 2.20000004768, 0));
        if (crystalDistance > breakRange.getValue()) return false;

        if (crystalDistance > breakWallsRange.getValue()) {
            Vec3d raytrace = pos.toCenterPos().add(0, 2.20000004768, 0);
            BlockHitResult result = mc.world.raycast(new RaycastContext(mc.player.getEyePos(), raytrace, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
            return result.getType() == HitResult.Type.MISS;
        }

        return true;
    }

    public void load(Result result) {
        if (result == null) {
            target = null;
            return;
        }

        target = result.target();

        if (InventoryUtil.findItem(Items.END_CRYSTAL) != -1 || offhand) {
            if (result.calcPos() != null) {
                calcPos = result.calcPos();
                placeTargetRot = result.placeRots();
                renderDMG = result.bestPlaceDMG();
            } else {
                if (result.bestPlaceDMG() == 0) {
                    renderPos = null;
                }
            }
        } else {
            calcPos = null;
            renderPos = null;
            placeTargetRot = null;
            renderDMG = 0;
        }
        if (result.calcCrystal() != null) {
            calcCrystal = result.calcCrystal();
            breakTargetRot = result.breakRots();
        }
    }

    public void strongSequence() {
        boolean breakPassed = breakTimer.passed(breakDelay.getValueInt());
        boolean placePassed = placeTimer.passed(placeDelay.getValueInt());

        if (calcPos == null && calcCrystal == null) {
            return;
        }

        if (breakPassed && !placePassed && breakTargetRot != null) {
            fentanyl.ROTATION.snapAt(breakTargetRot[0], breakTargetRot[1]);
        } else if (placePassed && !breakPassed && placeTargetRot != null) {
            fentanyl.ROTATION.snapAt(placeTargetRot[0], placeTargetRot[1]);
        } else {
            if (calcPos == null && breakTargetRot != null) {
                fentanyl.ROTATION.snapAt(breakTargetRot[0], breakTargetRot[1]);
                return;
            }

            if (calcCrystal == null || !placePassed) {
                if (placeTargetRot != null) {
                    fentanyl.ROTATION.snapAt(placeTargetRot[0], placeTargetRot[1]);
                }
                return;
            }

            switch (rotationsType.getValue()) {
                case NCP:
                    if (mc.world.getEntitiesByClass(Entity.class, new Box(calcPos).expand(0, 1, 0), e -> true).contains(calcCrystal)) {
                        if (RaytraceUtils.isLookingResult(mc.player, calcCrystal, mc.player.getEyePos(), placeTargetRot, 6.0f) != null) {
                            fentanyl.ROTATION.snapAt(placeTargetRot[0], placeTargetRot[1]);
                        } else {
                            if (breakTargetRot != null) {
                                fentanyl.ROTATION.snapAt(breakTargetRot[0], breakTargetRot[1]);
                            }
                        }
                    } else {
                        if (breakTargetRot != null) {
                            fentanyl.ROTATION.snapAt(breakTargetRot[0], breakTargetRot[1]);
                        }
                        if (placeTargetRot != null) {
                            fentanyl.ROTATION.snapAt(placeTargetRot[0], placeTargetRot[1]);
                        }
                    }
                    break;
                case GrimAbuse:
                    if (breakTargetRot != null) {
                        fentanyl.ROTATION.snapAt(breakTargetRot[0], breakTargetRot[1]);
                    }
                    break;
                case Simple:
                case MultiPoint:
                    if (mc.world.getEntitiesByClass(Entity.class, new Box(calcPos).expand(0, 1, 0), e -> true).contains(calcCrystal)) {
                        fentanyl.ROTATION.snapAt(placeTargetRot[0], placeTargetRot[1]);
                    } else if (calcCrystal.getBlockPos().down().equals(calcPos)) {
                        fentanyl.ROTATION.snapAt(placeTargetRot[0], placeTargetRot[1]);
                    } else if (breakTargetRot != null) {
                        fentanyl.ROTATION.snapAt(breakTargetRot[0], breakTargetRot[1]);
                    }
                    break;
                case Silent:
                    break;
            }
        }
    }

    public Vec3d crystalDamageVec(BlockPos pos) {
        return Vec3d.of(pos).add(0.5, 1.0, 0.5);
    }

    public float[] getPlaceRot(BlockPos pos) {
        if (pos == null) return null;
        if (!rotate.getValue()) return null;

        Direction side = getPlaceableSideCrystal(pos, strictDirection.getValue());
        if (side == null) return null;

        Vec3d vec = pos.toCenterPos().add(side.getVector().getX() * 0.5, side.getVector().getY() * 0.5, side.getVector().getZ() * 0.5);
        if (side.getAxis().isHorizontal()) {
            vec = vec.add(0, 0.45, 0);
        }

        return getRotationsTo(mc.player.getEyePos(), vec);
    }

    public void handleSequence() {
        switch (sequence.getValue()) {
            case None:
                if (breakTargetRot != null) {
                    fentanyl.ROTATION.snapAt(breakTargetRot[0], breakTargetRot[1]);
                }
                if (rotationsType.is(RotationsType.NCP) && placeTargetRot != null) {
                    fentanyl.ROTATION.snapAt(placeTargetRot[0], placeTargetRot[1]);
                }
                break;
            case Soft, Strong:
                strongSequence();
                break;
        }
    }

    public void reset() {
        calcCrystal = null;
        calcPos = null;
        breakTargetRot = null;
        placeTargetRot = null;
    }

    public AntiFeetPlaceResult handleBBCrystal(BlockPos pos, BlockPos lastPos, Entity currentCalcCrystal) {
        Box bb = new Box(0.0, 0.0, 0.0, 1.0, 2.0, 1.0);

        BlockPos pos2 = pos.up();
        double d = pos2.getX();
        double e = pos2.getY();
        double f = pos2.getZ();
        bb = new Box(d, e, f, d + bb.maxX, e + bb.maxY, f + bb.maxZ);

        boolean isAntiFeetPlace = false;
        if (lastPos != null && lastPos.equals(pos)) {
            for (Entity entity : mc.world.getNonSpectatingEntities(Entity.class, bb)) {
                if (entity instanceof EndCrystalEntity) {
                    if (!entity.isAlive() || entity.getBlockPos().down().equals(pos) || entity.equals(currentCalcCrystal)) {
                        continue;
                    }
                }

                if (antiFeetPlace.getValue() && entity instanceof net.minecraft.entity.ItemEntity) {
                    isAntiFeetPlace = true;
                    continue;
                }

                if (entity instanceof PlayerEntity player) {
                    if (HitboxManager.INSTANCE.isServerCrawling(player)) {
                        Box serverBB = HitboxManager.INSTANCE.getCrawlingBoundingBox(player);
                        if (!bb.intersects(serverBB)) continue;
                    }
                }
                if (!entity.isAlive()) continue;

                return new AntiFeetPlaceResult(false, isAntiFeetPlace);
            }
        } else {
            for (Entity entity : mc.world.getNonSpectatingEntities(Entity.class, bb)) {
                if (entity instanceof PlayerEntity player) {
                    if (HitboxManager.INSTANCE.isServerCrawling(player)) {
                        Box serverBB = HitboxManager.INSTANCE.getCrawlingBoundingBox(player);
                        if (!bb.intersects(serverBB)) continue;
                    }
                }
                if (antiFeetPlace.getValue() && entity instanceof net.minecraft.entity.ItemEntity) {
                    isAntiFeetPlace = true;
                    continue;
                }

                if (!entity.isAlive() || entity.equals(currentCalcCrystal)) continue;

                return new AntiFeetPlaceResult(false, isAntiFeetPlace);
            }
        }
        return new AntiFeetPlaceResult(true, isAntiFeetPlace);
    }

    public BreakResult getBestBreakResult(PlayerEntity targetPlayer) {
        Entity bestCrystal = null;
        double maxDamage = 0.5D;
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity crystal) {
                if (canBreakCrystal(crystal)) {
                    final float targetDamage = calculateDamage(targetPlayer, entity.getPos(), terrain.getValue(), false);

                    if ((maxDamage < targetDamage && targetDamage > minDamage.getValueFloat()) || 
                        (lethalCrystals.getValueInt() != 0 && (targetDamage * lethalCrystals.getValueFloat() >= targetPlayer.getHealth() + targetPlayer.getAbsorptionAmount()))) {
                        final float selfDamage = calculateDamage(mc.player, entity.getPos(), terrain.getValue(), false);

                        if (selfDamage > maxSelfDamage.getValueFloat()) continue;

                        if ((selfDamage + 2 > mc.player.getHealth() + mc.player.getAbsorptionAmount() && antiSuicide.getValue())) {
                            continue;
                        }

                        maxDamage = targetDamage;
                        bestCrystal = entity;
                    }
                }
            }
        }

        float[] breakRots = null;
        if (bestCrystal != null) {
            if (rotate.getValue()) {
                breakRots = getRotationsTo(mc.player.getEyePos(), new Vec3d(bestCrystal.getX(), bestCrystal.getEyeY(), bestCrystal.getZ()));
            }
        }
        return new BreakResult(targetPlayer, maxDamage, bestCrystal, breakRots);
    }

    public boolean canRender() {
        if (calcPos == null) return false;
        int crystalSlot = InventoryUtil.findItem(Items.END_CRYSTAL);
        return crystalSlot != -1 || offhand;
    }

    public boolean canBreakCrystal(EndCrystalEntity entity) {
        double distance = mc.player.getEyePos().distanceTo(entity.getPos().add(0, 1.700000047683716, 0));

        if (rotationsType.is(RotationsType.MultiPoint)) {
            if (entity.getBlockPos().getY() > mc.player.getBlockPos().getY()) {
                distance = mc.player.getEyePos().distanceTo(entity.getPos().add(0, 0.5f, 0));
            }
        }

        if (ticksExisted.getValueInt() != 0) {
            if (entity.age <= ticksExisted.getValueInt()) {
                return false;
            }
        }

        if (distance > breakRange.getValue()) return false;

        return (canEntityBeSeen(entity) || distance <= breakWallsRange.getValue());
    }

    private boolean canEntityBeSeen(Entity entity) {
        Vec3d vec;
        if (entity instanceof EndCrystalEntity) {
            vec = entity.getPos().add(0, 1.700000047683716, 0);
        } else {
            vec = new Vec3d(entity.getX(), entity.getEyeY(), entity.getZ());
        }
        BlockHitResult result = mc.world.raycast(new RaycastContext(mc.player.getEyePos(), vec, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        return result.getType() == HitResult.Type.MISS;
    }

    @EventListener
    public void onFrameFlip(TickEvent event) {
        if (nullCheck()) return;
        if (!event.isPost()) return;
        
        long currentTimeMs = System.currentTimeMillis();

        if (!brokeCrystals.isEmpty()) {
            try {
                while (true) {
                    if (brokeCrystals.isEmpty()) break;

                    long firstCrystal = brokeCrystals.getFirst();
                    final long second = 1000L;
                    if (currentTimeMs - firstCrystal > second) {
                        brokeCrystals.remove();
                    } else {
                        break;
                    }
                }
            } catch (Exception e) {
                CRYSTALS_PER_SECOND = 0;
            }
        }
        CRYSTALS_PER_SECOND = brokeCrystals.size();
    }

    @EventListener(priority = EventPriority.LOWEST)
    public void onPlayerUpdatePre(ClientTickEvent event) {
        if (nullCheck()) return;
        if (!event.isPre()) return;

        if (Phase.INSTANCE.isOn()) return;

        if (rotate.getValue() && PriorityManager.INSTANCE.isUsageLocked()) return;

        if (Blink.INSTANCE.isOn()) return;

        if (rotate.getValue() && SpeedMine.INSTANCE.isOn() && SpeedMine.ghost) return;

        if (isMultiTask()) {
            reset();
            return;
        }

        prepare();

        if (lethalTick.getValue() && autoHit.getValue() && calcPos == null && !mc.player.isUsingItem() && cooldownTimer.passed()) {
            tryAutoHit();
        }

        if (timing.getValue() == TimingMode.Soft) {
            interact();
        }
    }

    @EventListener(priority = EventPriority.LOWEST)
    public void onPlayerUpdatePost(ClientTickEvent event) {
        if (nullCheck()) return;
        if (!event.isPost()) return;

        if (Phase.INSTANCE.isOn()) return;

        if (rotate.getValue() && PriorityManager.INSTANCE.isUsageLocked()) return;

        if (Blink.INSTANCE.isOn()) return;

        if (rotate.getValue() && SpeedMine.INSTANCE.isOn() && SpeedMine.ghost) return;

        if (isMultiTask()) return;

        if (timing.getValue() == TimingMode.Strict) {
            interact();
        }
    }

    public void interact() {
        boolean breakCrystalResult = breakCrystal();

        int crystalSlot = InventoryUtil.findItem(Items.END_CRYSTAL);
        if (crystalSlot != -1 || offhand) {
            if (!limit.getValue() || (!breakCrystalResult || calcCrystal == null)) {
                placeCrystal();
            }
        }

        if (placedOnspawn) {
            placedOnspawn = false;
        }
    }

    private void tryAutoHit() {
        if (target == null) return;
        if (!dev.ninemmteam.api.utils.world.HoleUtils.isSurrounded(target.getBlockPos())) return;
        if (dev.ninemmteam.api.utils.world.HoleUtils.isSurrounded(target.getBlockPos().up())) return;
        if (!mc.world.getBlockState(target.getBlockPos().up(2)).isAir()) return;
        if (!didAutoDtapAttack && target.hurtTime == 0 && !doingAutoDtap && Aura.INSTANCE.isInAttackRange(mc.player.getEyePos(), target)) {
            int oldSlot = mc.player.getInventory().selectedSlot;
            int bestWeapon = InventoryUtil.getSwordSlot();
            boolean switched = false;
            if (bestWeapon != -1 && mc.player.getInventory().selectedSlot != bestWeapon) {
                InventoryUtil.switchToSlot(bestWeapon);
                switched = true;
            }
            if (rotate.getValue() && !shouldYawStep()) {
                Vec3d attackVec = Aura.INSTANCE.getAttackRotateVec(target);
                if (!faceVector(attackVec)) {
                    return;
                }
            }
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
            if (switched) {
                InventoryUtil.switchToSlot(oldSlot);
            }
            beginAutoDtap();
            cooldownTimer.reset();

            if (rotate.getValue() && !shouldYawStep()) {
                fentanyl.ROTATION.snapBack();
            }
        }
    }

    public void beginAutoDtap() {
        if (!lethalTick.getValue()) return;

        doingAutoDtap = true;
        didAutoDtapAttack = true;
        didAutoDtapCrystal = false;
        autoDtapTimer.resetDelay();
        breakFlag = true;
    }

    private boolean isValidTarget(PlayerEntity player) {
        if (player == mc.player) return false;
        if (fentanyl.FRIEND.isFriend(player.getName().getString())) return false;
        if (!player.isAlive()) return false;
        if (player.getHealth() <= 0.0f) return false;
        if (mc.player.distanceTo(player) > targetRange.getValue()) return false;
        return true;
    }

    public boolean canPlaceCrystal(BlockPos pos, boolean is112) {
        if (mc.world.getBlockState(pos).getBlock() != Blocks.BEDROCK &&
            mc.world.getBlockState(pos).getBlock() != Blocks.OBSIDIAN) {
            return false;
        }

        if (!mc.world.isAir(pos.up())) return false;
        if (!is112 && !ClientSetting.INSTANCE.lowVersion.getValue() && !mc.world.isAir(pos.up(2))) return false;

        return true;
    }

    public Direction getPlaceableSideCrystal(BlockPos pos, boolean strict) {
        if (strict) {
            return BlockUtil.getClickSideStrict(pos);
        }
        return BlockUtil.getClickSide(pos);
    }

    public boolean isBehindWall(BlockPos pos, Vec3d raytrace) {
        BlockHitResult result = mc.world.raycast(new RaycastContext(mc.player.getEyePos(), raytrace, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        return result.getType() != HitResult.Type.MISS;
    }

    public float[] getRotationsTo(Vec3d eyesPos, Vec3d vec) {
        double diffX = vec.x - eyesPos.x;
        double diffY = vec.y - eyesPos.y;
        double diffZ = vec.z - eyesPos.z;
        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        float pitch = (float) (-Math.toDegrees(Math.atan2(diffY, diffXZ)));
        return new float[]{yaw, pitch};
    }

    public float calculateDamage(LivingEntity target, Vec3d pos, boolean terrainCalc, boolean miningIgnore) {
        return CrystalUtil.calculateDamage(target, pos, terrainCalc, miningIgnore);
    }

    private boolean isEatingGap() {
        return mc.player.isUsingItem() &&
            mc.player.getActiveItem().getItem() == Items.ENCHANTED_GOLDEN_APPLE;
    }

    private record Result(BreakResult breakResult, PlaceResult placeResult) {
        public PlayerEntity target() { return breakResult.target; }
        public double getDamage() { return Math.max(breakResult.bestDMG, placeResult.bestDMG); }
        public BlockPos calcPos() { return placeResult.calcPos; }
        public Entity calcCrystal() { return breakResult.calcCrystal; }
        public float[] placeRots() { return placeResult.rots; }
        public float[] breakRots() { return breakResult.rots; }
        public double bestPlaceDMG() { return placeResult.bestDMG; }
    }

    private record BreakResult(PlayerEntity target, double bestDMG, Entity calcCrystal, float[] rots) {}
    private record PlaceResult(PlayerEntity target, double bestDMG, BlockPos calcPos, float[] rots) {}
    private record RenderPosTime(BlockPos pos, long time, double damage) {}
    private record AntiFeetPlaceResult(boolean isPlaceAvailable, boolean isAntiFeetPlace) {}

    public enum Page {
        Render, Calc, Place, Break, Misc, Timing
    }

    public enum SortMode {
        Damage, Range
    }

    public enum RotationsType {
        Simple, NCP, GrimAbuse, MultiPoint, Silent
    }

    public enum RenderMode {
        Normal, Fade, Glide
    }

    public enum AutoSwitchMode {
        None, Normal, Silent, SilentBypass
    }

    public enum MultiTaskMode {
        None, Soft, Strong
    }

    public enum AntiWeaknessMode {
        None, Normal, Silent
    }

    public enum SetDeadMode {
        None, Ghost
    }

    public enum SequenceMode {
        None, Soft, Strong
    }

    public enum SwapWaitMode {
        None, Semi, Full
    }

    public enum TimingMode {
        Soft, Strict
    }

    public enum MiningIgnoreMode {
        None, Ignore, StrictIgnore
    }
}
