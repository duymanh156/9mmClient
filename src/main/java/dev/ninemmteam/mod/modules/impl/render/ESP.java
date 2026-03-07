package dev.ninemmteam.mod.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.ninemmteam.fentanyl;
import dev.ninemmteam.api.events.eventbus.EventListener;
import dev.ninemmteam.api.events.impl.EntitySpawnedEvent;
import dev.ninemmteam.api.utils.math.MathUtil;
import dev.ninemmteam.api.utils.math.Timer;
import dev.ninemmteam.api.utils.render.ColorUtil;
import dev.ninemmteam.api.utils.render.Render2DUtil;
import dev.ninemmteam.api.utils.render.Render3DUtil;
import dev.ninemmteam.api.utils.render.TextUtil;
import dev.ninemmteam.api.utils.world.BlockUtil;
import dev.ninemmteam.asm.accessors.IEntity;
import dev.ninemmteam.core.impl.FontManager;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.settings.impl.BooleanSetting;
import dev.ninemmteam.mod.modules.settings.impl.ColorSetting;
import dev.ninemmteam.mod.modules.settings.impl.EnumSetting;
import dev.ninemmteam.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EndPortalBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector4d;

public class ESP extends Module {
   public static ESP INSTANCE;
   
   public final BooleanSetting box = this.add(new BooleanSetting("BoxESP", true).setParent());
   private final ColorSetting endPortalFill = this.add(new ColorSetting("EndPortalFill", new Color(255, 243, 129, 100), this.box::isOpen).injectBoolean(false));
   private final ColorSetting endPortalOutline = this.add(new ColorSetting("EndPortalOutline", new Color(255, 243, 129, 100), this.box::isOpen).injectBoolean(false));
   private final ColorSetting itemFill = this.add(new ColorSetting("ItemFill", new Color(255, 255, 255, 100), this.box::isOpen).injectBoolean(true));
   private final ColorSetting itemOutline = this.add(new ColorSetting("ItemOutline", new Color(255, 255, 255, 100), this.box::isOpen).injectBoolean(true));
   private final ColorSetting playerFill = this.add(new ColorSetting("PlayerFill", new Color(255, 255, 255, 100), this.box::isOpen).injectBoolean(true));
   private final ColorSetting playerOutline = this.add(new ColorSetting("PlayerOutline", new Color(255, 255, 255, 100), this.box::isOpen).injectBoolean(true));
   private final ColorSetting chestFill = this.add(new ColorSetting("ChestFill", new Color(255, 198, 123, 100), this.box::isOpen).injectBoolean(false));
   private final ColorSetting chestOutline = this.add(new ColorSetting("ChestOutline", new Color(255, 198, 123, 100), this.box::isOpen).injectBoolean(false));
   private final ColorSetting enderChestFill = this.add(new ColorSetting("EnderChestFill", new Color(255, 100, 255, 100), this.box::isOpen).injectBoolean(false));
   private final ColorSetting enderChestOutline = this.add(new ColorSetting("EnderChestOutline", new Color(255, 100, 255, 100), this.box::isOpen).injectBoolean(false));
   private final ColorSetting shulkerBoxFill = this.add(new ColorSetting("ShulkerBoxFill", new Color(15, 255, 255, 100), this.box::isOpen).injectBoolean(false));
   private final ColorSetting shulkerBoxOutline = this.add(new ColorSetting("ShulkerBoxOutline", new Color(15, 255, 255, 100), this.box::isOpen).injectBoolean(false));
   public final BooleanSetting item = this.add(new BooleanSetting("ItemName", false).setParent());
   public final BooleanSetting customName = this.add(new BooleanSetting("CustomName", false, this.item::isOpen));
   public final BooleanSetting count = this.add(new BooleanSetting("Count", true, this.item::isOpen));
   private final ColorSetting text = this.add(new ColorSetting("Text", new Color(255, 255, 255, 255), this.item::isOpen));
   public final BooleanSetting pearl = this.add(new BooleanSetting("PearlOwner", true));

   public final BooleanSetting hole = this.add(new BooleanSetting("HoleESP", true).setParent());
   public final SliderSetting startFade = this.add(new SliderSetting("StartFade", 5.0, 1.0, 20.0, this.hole::isOpen));
   public final SliderSetting distance = this.add(new SliderSetting("Distance", 6.0, 1.0, 20.0, this.hole::isOpen));
   public final SliderSetting airHeight = this.add(new SliderSetting("AirHeight", 1.0, -3.0, 3.0, 0.01, this.hole::isOpen));
   public final BooleanSetting airYCheck = this.add(new BooleanSetting("AirYCheck", true, this.hole::isOpen));
   public final SliderSetting height = this.add(new SliderSetting("Height", 1.0, -3.0, 3.0, 0.1, this.hole::isOpen));
   public final SliderSetting wallHeight = this.add(new SliderSetting("WallHeight", 3.0, -3.0, 3.0, 0.1, this.hole::isOpen));
   public final BooleanSetting sideCheck = this.add(new BooleanSetting("SideCheck", true, this.hole::isOpen));
   private final ColorSetting airFill = this.add(new ColorSetting("AirFill", new Color(148, 0, 0, 100), this.hole::isOpen).injectBoolean(true));
   private final ColorSetting airBox = this.add(new ColorSetting("AirBox", new Color(148, 0, 0, 100), this.hole::isOpen).injectBoolean(true));
   private final ColorSetting airFade = this.add(new ColorSetting("AirFade", new Color(148, 0, 0, 0), this.hole::isOpen).injectBoolean(true));
   private final ColorSetting normalFill = this.add(new ColorSetting("UnsafeFill", new Color(255, 0, 0, 50), this.hole::isOpen).injectBoolean(true));
   private final ColorSetting normalBox = this.add(new ColorSetting("UnsafeBox", new Color(255, 0, 0, 100), this.hole::isOpen).injectBoolean(true));
   private final ColorSetting normalFade = this.add(new ColorSetting("UnsafeFade", new Color(255, 0, 0, 0), this.hole::isOpen).injectBoolean(true));
   private final ColorSetting bedrockFill = this.add(new ColorSetting("SafeFill", new Color(8, 255, 79, 50), this.hole::isOpen).injectBoolean(true));
   private final ColorSetting bedrockBox = this.add(new ColorSetting("SafeBox", new Color(8, 255, 79, 100), this.hole::isOpen).injectBoolean(true));
   private final ColorSetting bedrockFade = this.add(new ColorSetting("SafeFade", new Color(8, 255, 79, 100), this.hole::isOpen).injectBoolean(true));
   private final ColorSetting wallFill = this.add(new ColorSetting("WallFill", new Color(0, 255, 255, 128), this.hole::isOpen).injectBoolean(true));
   private final ColorSetting wallBox = this.add(new ColorSetting("WallBox", new Color(0, 225, 255, 255), this.hole::isOpen).injectBoolean(true));
   private final ColorSetting wallFade = this.add(new ColorSetting("WallFade", new Color(0, 255, 255, 64), this.hole::isOpen).injectBoolean(true));
   private final ColorSetting wallSideFill = this.add(new ColorSetting("WallSideFill", new Color(0, 255, 255, 128), this.hole::isOpen).injectBoolean(true));
   private final ColorSetting wallSideBox = this.add(new ColorSetting("WallSideBox", new Color(0, 225, 255, 255), this.hole::isOpen).injectBoolean(true));
   private final ColorSetting wallSideFade = this.add(new ColorSetting("WallSideFade", new Color(0, 255, 255, 64), this.hole::isOpen).injectBoolean(true));
   private final SliderSetting updateDelay = this.add(new SliderSetting("UpdateDelay", 50, 0, 1000, this.hole::isOpen));

   public final BooleanSetting twoD = this.add(new BooleanSetting("2DESP", false).setParent());
   private final EnumSetting<TwoDESPMode> page = this.add(new EnumSetting<>("2DSettings", TwoDESPMode.Target, this.twoD::isOpen));
   public final ColorSetting armorDuraColor = this.add(new ColorSetting("Armor Dura Color", new Color(0x2FFF00), () -> this.twoD.isOpen() && this.page.getValue() == TwoDESPMode.Color));
   public final ColorSetting hHealth = this.add(new ColorSetting("High Health Color", new Color(0, 255, 0, 255), () -> this.twoD.isOpen() && this.page.getValue() == TwoDESPMode.Color));
   public final ColorSetting mHealth = this.add(new ColorSetting("Mid Health Color", new Color(255, 255, 0, 255), () -> this.twoD.isOpen() && this.page.getValue() == TwoDESPMode.Color));
   public final ColorSetting lHealth = this.add(new ColorSetting("Low Health Color", new Color(255, 0, 0, 255), () -> this.twoD.isOpen() && this.page.getValue() == TwoDESPMode.Color));
   private final BooleanSetting outline = this.add(new BooleanSetting("Outline", true, () -> this.twoD.isOpen() && this.page.getValue() == TwoDESPMode.Setting));
   private final BooleanSetting renderHealth = this.add(new BooleanSetting("renderHealth", true, () -> this.twoD.isOpen() && this.page.getValue() == TwoDESPMode.Setting));
   private final BooleanSetting renderArmor = this.add(new BooleanSetting("Armor Dura", true, () -> this.twoD.isOpen() && this.page.getValue() == TwoDESPMode.Setting));
   private final SliderSetting durascale = this.add(new SliderSetting("DuraScale", 1.0, 0.0, 2.0, 0.1, () -> this.twoD.isOpen() && this.renderArmor.getValue()));
   private final BooleanSetting drawItem = this.add(new BooleanSetting("draw Item Name", true, () -> this.twoD.isOpen() && this.page.getValue() == TwoDESPMode.Setting));
   private final BooleanSetting drawItemC = this.add(new BooleanSetting("draw Item Count", true, () -> this.twoD.isOpen() && this.page.getValue() == TwoDESPMode.Setting && this.drawItem.getValue()));
   public final ColorSetting countColor = this.add(new ColorSetting("Item Count Color", new Color(255, 255, 0, 255), () -> this.twoD.isOpen() && this.page.getValue() == TwoDESPMode.Color && this.drawItemC.getValue()));
   public final ColorSetting textcolor = this.add(new ColorSetting("Item Name Color", new Color(255, 255, 255, 255), () -> this.twoD.isOpen() && this.page.getValue() == TwoDESPMode.Color && this.drawItem.getValue()));
   private final BooleanSetting font = this.add(new BooleanSetting("CustomFont", true, () -> this.twoD.isOpen() && this.page.getValue() == TwoDESPMode.Setting));
   private final BooleanSetting players = this.add(new BooleanSetting("Players", true, () -> this.twoD.isOpen() && this.page.getValue() == TwoDESPMode.Target));
   private final BooleanSetting friends = this.add(new BooleanSetting("Friends", true, () -> this.twoD.isOpen() && this.page.getValue() == TwoDESPMode.Target));
   private final BooleanSetting crystals = this.add(new BooleanSetting("Crystals", true, () -> this.twoD.isOpen() && this.page.getValue() == TwoDESPMode.Target));
   private final BooleanSetting creatures = this.add(new BooleanSetting("Creatures", false, () -> this.twoD.isOpen() && this.page.getValue() == TwoDESPMode.Target));
   private final BooleanSetting monsters = this.add(new BooleanSetting("Monsters", false, () -> this.twoD.isOpen() && this.page.getValue() == TwoDESPMode.Target));
   private final BooleanSetting ambients = this.add(new BooleanSetting("Ambients", false, () -> this.twoD.isOpen() && this.page.getValue() == TwoDESPMode.Target));
   private final BooleanSetting others = this.add(new BooleanSetting("Others", false, () -> this.twoD.isOpen() && this.page.getValue() == TwoDESPMode.Target));
   private final ColorSetting playersC = this.add(new ColorSetting("PlayersBox", new Color(16749056), () -> this.twoD.isOpen() && this.page.getValue() == TwoDESPMode.Color));
   private final ColorSetting friendsC = this.add(new ColorSetting("FriendsBox", new Color(0x30FF00), () -> this.twoD.isOpen() && this.page.getValue() == TwoDESPMode.Color));
   private final ColorSetting crystalsC = this.add(new ColorSetting("CrystalsBox", new Color(48127), () -> this.twoD.isOpen() && this.page.getValue() == TwoDESPMode.Color));
   private final ColorSetting creaturesC = this.add(new ColorSetting("CreaturesBox", new Color(10527910), () -> this.twoD.isOpen() && this.page.getValue() == TwoDESPMode.Color));
   private final ColorSetting monstersC = this.add(new ColorSetting("MonstersBox", new Color(0xFF0000), () -> this.twoD.isOpen() && this.page.getValue() == TwoDESPMode.Color));
   private final ColorSetting ambientsC = this.add(new ColorSetting("AmbientsBox", new Color(8061183), () -> this.twoD.isOpen() && this.page.getValue() == TwoDESPMode.Color));
   private final ColorSetting othersC = this.add(new ColorSetting("OthersBox", new Color(16711778), () -> this.twoD.isOpen() && this.page.getValue() == TwoDESPMode.Color));

   private final List<BlockPos> tempNormalList = new ArrayList<>();
   private final List<BlockPos> tempBedrockList = new ArrayList<>();
   private final List<BlockPos> tempAirList = new ArrayList<>();
   private final List<BlockPos> tempWallList = new ArrayList<>();
   private final List<BlockPos> tempWallSideList = new ArrayList<>();
   private final Timer timer = new Timer();
   boolean drawing = false;
   private List<BlockPos> normalList = new ArrayList<>();
   private List<BlockPos> bedrockList = new ArrayList<>();
   private List<BlockPos> airList = new ArrayList<>();
   private List<BlockPos> wallList = new ArrayList<>();
   private List<BlockPos> wallSideList = new ArrayList<>();

   public ESP() {
      super("ESP", Module.Category.Render);
      this.setChinese("透视");
      INSTANCE = this;
   }

   public void onThread() {
      if (!nullCheck() && !this.isOff() && hole.getValue()) {
         if (!this.drawing && this.timer.passedMs(this.updateDelay.getValue())) {
            this.normalList = new ArrayList<>(this.tempNormalList);
            this.bedrockList = new ArrayList<>(this.tempBedrockList);
            this.airList = new ArrayList<>(this.tempAirList);
            this.wallList = new ArrayList<>(this.tempWallList);
            this.wallSideList = new ArrayList<>(this.tempWallSideList);
            this.timer.reset();
            this.tempBedrockList.clear();
            this.tempNormalList.clear();
            this.tempAirList.clear();
            this.tempWallList.clear();
            this.tempWallSideList.clear();

            for (BlockPos pos : BlockUtil.getSphere(this.distance.getValueFloat(), mc.player.getPos())) {
               if (this.isBedrock(pos) && this.isBedrock(pos.up(2)) && this.isBedrock(pos.down())) {
                  Direction side = this.getWallSide(pos);
                  if (side != null || !this.sideCheck.getValue()) {
                     this.tempWallList.add(pos);
                  }
                  if (side != null) {
                     this.tempWallSideList.add(pos.offset(side));
                  }
               }

               Type type = this.isHole(pos);
               if (type == Type.Bedrock) {
                  this.tempBedrockList.add(pos);
               } else if (type == Type.Normal) {
                  this.tempNormalList.add(pos);
               } else if (type == Type.Air) {
                  this.tempAirList.add(pos);
               }
            }
         }
      }
   }

   private Direction getWallSide(BlockPos pos) {
      double dist = Double.MAX_VALUE;
      Direction side = null;
      for (Direction direction : Direction.values()) {
         if (direction != Direction.UP && direction != Direction.DOWN) {
            BlockPos offsetPos = pos.offset(direction);
            if (BlockUtil.canCollide(new Box(offsetPos.down())) && !BlockUtil.canCollide(new Box(offsetPos)) && !BlockUtil.canCollide(new Box(offsetPos.up()))) {
               if (side == null) {
                  side = direction;
                  dist = mc.player.getEyePos().distanceTo(offsetPos.toCenterPos());
               } else if (mc.player.getEyePos().distanceTo(offsetPos.toCenterPos()) < dist) {
                  side = direction;
                  dist = mc.player.getEyePos().distanceTo(offsetPos.toCenterPos());
               }
            }
         }
      }
      return side;
   }

   private boolean isBedrock(BlockPos pos) {
      return mc.world.getBlockState(pos).getBlock() == Blocks.BEDROCK;
   }

   Type isHole(BlockPos pos) {
      if (mc.world.isAir(pos) && (!this.airYCheck.getValue() || pos.getY() == mc.player.getBlockY() - 1 || pos.getY() == mc.player.getBlockY()) && fentanyl.HOLE.isHard(pos.up())) {
         return Type.Air;
      } else {
         int blockProgress = 0;
         boolean bedRock = true;
         for (Direction i : Direction.values()) {
            if (i != Direction.UP && i != Direction.DOWN && fentanyl.HOLE.isHard(pos.offset(i))) {
               if (mc.world.getBlockState(pos.offset(i)).getBlock() != Blocks.BEDROCK) {
                  bedRock = false;
               }
               blockProgress++;
            }
         }
         if (mc.world.isAir(pos) && mc.world.isAir(pos.up()) && mc.world.isAir(pos.up(2)) && blockProgress > 3 && BlockUtil.canCollide(mc.player, new Box(pos.down()))) {
            return bedRock ? Type.Bedrock : Type.Normal;
         } else {
            return fentanyl.HOLE.isDoubleHole(pos) ? Type.Normal : Type.None;
         }
      }
   }

   @Override
   public void onRender2D(DrawContext context, float tickDelta) {
      if (!twoD.getValue()) return;
      
      Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
      Render2DUtil.setupRender();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder bufferBuilder = null;
      boolean hasVertices = false;
      for (Entity ent : mc.world.getEntities()) {
         if (!this.shouldRender2D(ent)) continue;
         if (bufferBuilder == null) {
            bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         }
         if (this.drawBox(bufferBuilder, ent, matrix, context)) {
            hasVertices = true;
         }
      }
      if (hasVertices && bufferBuilder != null) {
         BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      }
      Render2DUtil.endRender();
      for (Entity ent : mc.world.getEntities()) {
         if (!this.shouldRender2D(ent)) continue;
         this.drawText(ent, context);
      }
   }

   @Override
   public void onRender3D(MatrixStack matrixStack) {
      if (hole.getValue()) {
         this.drawing = true;
         this.drawHole(matrixStack, this.bedrockList, this.bedrockFill, this.bedrockFade, this.bedrockBox, this.height.getValue());
         this.drawHole(matrixStack, this.airList, this.airFill, this.airFade, this.airBox, this.airHeight.getValue());
         this.drawHole(matrixStack, this.normalList, this.normalFill, this.normalFade, this.normalBox, this.height.getValue());
         this.drawHole(matrixStack, this.wallList, this.wallFill, this.wallFade, this.wallBox, this.wallHeight.getValue());
         this.drawHole(matrixStack, this.wallSideList, this.wallSideFill, this.wallSideFade, this.wallSideBox, this.height.getValue());
         this.drawing = false;
      }

      if (this.item.getValue()) {
         for (Entity entity : fentanyl.THREAD.getEntities()) {
            if (entity instanceof ItemEntity itemEntity) {
               int itemCount = itemEntity.getStack().getCount();
               String s = this.count.getValue() && itemCount > 1 ? " x" + itemCount : "";
               String name = (this.customName.getValue() ? itemEntity.getStack().getName() : itemEntity.getStack().getItem().getName()).getString();
               Render3DUtil.drawText3D(name + s, ((IEntity)itemEntity).getDimensions().getBoxAt(new Vec3d(MathUtil.interpolate(itemEntity.lastRenderX, itemEntity.getX(), mc.getRenderTickCounter().getTickDelta(true)), MathUtil.interpolate(itemEntity.lastRenderY, itemEntity.getY(), mc.getRenderTickCounter().getTickDelta(true)), MathUtil.interpolate(itemEntity.lastRenderZ, itemEntity.getZ(), mc.getRenderTickCounter().getTickDelta(true)))).expand(0.0, 0.1, 0.0).getCenter().add(0.0, 0.5, 0.0), this.text.getValue());
            }
         }
      }

      if (this.box.getValue()) {
         if (this.itemFill.booleanValue || this.playerFill.booleanValue) {
            for (Entity entityx : fentanyl.THREAD.getEntities()) {
               if (!(entityx instanceof ItemEntity) || !this.itemFill.booleanValue && !this.itemOutline.booleanValue) {
                  if (entityx instanceof PlayerEntity && (this.playerFill.booleanValue || this.playerOutline.booleanValue)) {
                     Color color = this.playerFill.getValue();
                     Render3DUtil.draw3DBox(matrixStack, ((IEntity)entityx).getDimensions().getBoxAt(new Vec3d(MathUtil.interpolate(entityx.lastRenderX, entityx.getX(), mc.getRenderTickCounter().getTickDelta(true)), MathUtil.interpolate(entityx.lastRenderY, entityx.getY(), mc.getRenderTickCounter().getTickDelta(true)), MathUtil.interpolate(entityx.lastRenderZ, entityx.getZ(), mc.getRenderTickCounter().getTickDelta(true)))).expand(0.0, 0.1, 0.0), color, this.playerOutline.getValue(), this.playerOutline.booleanValue, this.playerFill.booleanValue);
                  }
               } else {
                  Color color = this.itemFill.getValue();
                  Render3DUtil.draw3DBox(matrixStack, ((IEntity)entityx).getDimensions().getBoxAt(new Vec3d(MathUtil.interpolate(entityx.lastRenderX, entityx.getX(), mc.getRenderTickCounter().getTickDelta(true)), MathUtil.interpolate(entityx.lastRenderY, entityx.getY(), mc.getRenderTickCounter().getTickDelta(true)), MathUtil.interpolate(entityx.lastRenderZ, entityx.getZ(), mc.getRenderTickCounter().getTickDelta(true)))), color, this.itemOutline.getValue(), this.itemOutline.booleanValue, this.itemFill.booleanValue);
               }
            }
         }

         for (BlockEntity blockEntity : BlockUtil.getTileEntities()) {
            if (!(blockEntity instanceof ChestBlockEntity) || !this.chestFill.booleanValue && !this.chestOutline.booleanValue) {
               if (!(blockEntity instanceof EnderChestBlockEntity) || !this.enderChestFill.booleanValue && !this.enderChestOutline.booleanValue) {
                  if (!(blockEntity instanceof ShulkerBoxBlockEntity) || !this.shulkerBoxFill.booleanValue && !this.shulkerBoxOutline.booleanValue) {
                     if (blockEntity instanceof EndPortalBlockEntity && (this.endPortalFill.booleanValue || this.endPortalOutline.booleanValue)) {
                        Box box = new Box(blockEntity.getPos());
                        Render3DUtil.draw3DBox(matrixStack, box, this.endPortalFill.getValue(), this.endPortalOutline.getValue(), this.endPortalOutline.booleanValue, this.endPortalFill.booleanValue);
                     }
                  } else {
                     Box box = new Box(blockEntity.getPos());
                     Render3DUtil.draw3DBox(matrixStack, box, this.shulkerBoxFill.getValue(), this.shulkerBoxOutline.getValue(), this.shulkerBoxOutline.booleanValue, this.shulkerBoxFill.booleanValue);
                  }
               } else {
                  Box box = new Box(blockEntity.getPos());
                  Render3DUtil.draw3DBox(matrixStack, box, this.enderChestFill.getValue(), this.enderChestOutline.getValue(), this.enderChestOutline.booleanValue, this.enderChestFill.booleanValue);
               }
            } else {
               Box box = new Box(blockEntity.getPos());
               Render3DUtil.draw3DBox(matrixStack, box, this.chestFill.getValue(), this.chestOutline.getValue(), this.chestOutline.booleanValue, this.chestFill.booleanValue);
            }
         }
      }
   }

   private void drawHole(MatrixStack matrixStack, List<BlockPos> list, ColorSetting fill, ColorSetting fade, ColorSetting box, double height) {
      for (BlockPos pos : list) {
         double dist = mc.player.getPos().distanceTo(pos.toCenterPos());
         double alpha = dist > this.startFade.getValue() ? Math.max(Math.min(1.0, 1.0 - (dist - this.startFade.getValue()) / (this.distance.getValue() - this.startFade.getValue())), 0.0) : 1.0;
         Box espBox = new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + height, pos.getZ() + 1);
         if (fill.booleanValue) {
            if (fade.booleanValue) {
               Render3DUtil.drawFadeFill(matrixStack, espBox, ColorUtil.injectAlpha(fill.getValue(), (int)(fill.getValue().getAlpha() * alpha)), ColorUtil.injectAlpha(fade.getValue(), (int)(fade.getValue().getAlpha() * alpha)));
            } else {
               Render3DUtil.drawFill(matrixStack, espBox, ColorUtil.injectAlpha(fill.getValue(), (int)(fill.getValue().getAlpha() * alpha)));
            }
         }
         if (box.booleanValue) {
            Render3DUtil.drawBox(matrixStack, espBox, ColorUtil.injectAlpha(box.getValue(), (int)(box.getValue().getAlpha() * alpha)));
         }
      }
   }

   public boolean shouldRender2D(Entity entity) {
      if (entity == null || mc.player == null) return false;
      if (entity instanceof PlayerEntity) {
         if (entity == mc.player && mc.options.getPerspective().isFirstPerson()) return false;
         if (fentanyl.FRIEND.isFriend((PlayerEntity) entity)) return this.friends.getValue();
         return this.players.getValue();
      }
      if (entity instanceof EndCrystalEntity) return this.crystals.getValue();
      return switch (entity.getType().getSpawnGroup()) {
         case CREATURE, WATER_CREATURE -> this.creatures.getValue();
         case MONSTER -> this.monsters.getValue();
         case AMBIENT, WATER_AMBIENT -> this.ambients.getValue();
         default -> this.others.getValue();
      };
   }

   public Color getEntityColor(Entity entity) {
      if (entity == null) return new Color(-1);
      if (entity instanceof PlayerEntity) {
         if (fentanyl.FRIEND.isFriend((PlayerEntity) entity)) return this.friendsC.getValue();
         return this.playersC.getValue();
      }
      if (entity instanceof EndCrystalEntity) return this.crystalsC.getValue();
      return switch (entity.getType().getSpawnGroup()) {
         case CREATURE, WATER_CREATURE -> this.creaturesC.getValue();
         case MONSTER -> this.monstersC.getValue();
         case AMBIENT, WATER_AMBIENT -> this.ambientsC.getValue();
         default -> this.othersC.getValue();
      };
   }

   public boolean drawBox(BufferBuilder bufferBuilder, @NotNull Entity ent, Matrix4f matrix, DrawContext context) {
      double x = ent.prevX + (ent.getX() - ent.prevX) * mc.getRenderTickCounter().getTickDelta(true);
      double y = ent.prevY + (ent.getY() - ent.prevY) * mc.getRenderTickCounter().getTickDelta(true);
      double z = ent.prevZ + (ent.getZ() - ent.prevZ) * mc.getRenderTickCounter().getTickDelta(true);
      Box axisAlignedBB2 = ent.getBoundingBox();
      Box axisAlignedBB = new Box(axisAlignedBB2.minX - ent.getX() + x - 0.05, axisAlignedBB2.minY - ent.getY() + y, axisAlignedBB2.minZ - ent.getZ() + z - 0.05, axisAlignedBB2.maxX - ent.getX() + x + 0.05, axisAlignedBB2.maxY - ent.getY() + y + 0.15, axisAlignedBB2.maxZ - ent.getZ() + z + 0.05);
      Vec3d[] vectors = new Vec3d[] { new Vec3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ), new Vec3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ), new Vec3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ), new Vec3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ), new Vec3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ), new Vec3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ), new Vec3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ), new Vec3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ) };
      Color col = this.getEntityColor(ent);
      Vector4d position = null;
      for (Vec3d vector : vectors) {
         vector = TextUtil.worldSpaceToScreenSpace(new Vec3d(vector.x, vector.y, vector.z));
         if (!(vector.z > 0.0) || !(vector.z < 1.0)) continue;
         if (position == null) position = new Vector4d(vector.x, vector.y, vector.z, 0.0);
         position.x = Math.min(vector.x, position.x);
         position.y = Math.min(vector.y, position.y);
         position.z = Math.max(vector.x, position.z);
         position.w = Math.max(vector.y, position.w);
      }
      if (position != null) {
         double posX = position.x, posY = position.y, endPosX = position.z, endPosY = position.w;
         if (this.outline.getValue()) {
            drawRectToBuffer(bufferBuilder, matrix, (float)(posX - 1.0), (float)posY, 1.5f, (float)(endPosY - posY + 0.5), Color.BLACK);
            drawRectToBuffer(bufferBuilder, matrix, (float)(posX - 1.0), (float)(posY - 0.5), (float)(endPosX - posX + 1.5), 1.5f, Color.BLACK);
            drawRectToBuffer(bufferBuilder, matrix, (float)(endPosX - 1.0), (float)posY, 1.5f, (float)(endPosY - posY + 0.5), Color.BLACK);
            drawRectToBuffer(bufferBuilder, matrix, (float)(posX - 1.0), (float)(endPosY - 1.0), (float)(endPosX - posX + 1.5), 1.5f, Color.BLACK);
            drawRectToBuffer(bufferBuilder, matrix, (float)(posX - 0.5), (float)posY, 0.5f, (float)(endPosY - posY), col);
            drawRectToBuffer(bufferBuilder, matrix, (float)posX, (float)(endPosY - 0.5), (float)(endPosX - posX), 0.5f, col);
            drawRectToBuffer(bufferBuilder, matrix, (float)(posX - 0.5), (float)posY, (float)(endPosX - posX + 0.5), 0.5f, col);
            drawRectToBuffer(bufferBuilder, matrix, (float)(endPosX - 0.5), (float)posY, 0.5f, (float)(endPosY - posY), col);
         }
         if (ent instanceof LivingEntity lent && lent.getHealth() != 0.0f && this.renderHealth.getValue()) {
            drawRectToBuffer(bufferBuilder, matrix, (float)(posX - 4.0), (float)posY, 1.0f, (float)(endPosY - posY), Color.BLACK);
            Color color = this.get2DColor(lent.getHealth());
            float healthHeight = (float)(endPosY - (endPosY + (posY - endPosY) * lent.getHealth() / lent.getMaxHealth()));
            drawRectToBuffer(bufferBuilder, matrix, (float)(posX - 4.0), (float)(endPosY - healthHeight), 1.0f, healthHeight, color);
         }
         if (ent instanceof PlayerEntity player && this.renderArmor.getValue()) {
            double h = (endPosY - posY) / 4.0;
            ArrayList<ItemStack> stacks = new ArrayList<>();
            stacks.add(player.getInventory().armor.get(3));
            stacks.add(player.getInventory().armor.get(2));
            stacks.add(player.getInventory().armor.get(1));
            stacks.add(player.getInventory().armor.get(0));
            int i = -1;
            for (ItemStack armor : stacks) {
               ++i;
               if (armor.isEmpty()) continue;
               float durability = armor.getMaxDamage() - armor.getDamage();
               int percent = (int)(durability / (float)armor.getMaxDamage() * 100.0f);
               double finalH = h * (percent / 100.0);
               drawRectToBuffer(bufferBuilder, matrix, (float)(endPosX + 1.5), (float)(posY + h * i + 1.2 * (i + 1)), 1.5f, (float)finalH, this.armorDuraColor.getValue());
            }
         }
         return true;
      }
      return false;
   }

   private void drawRectToBuffer(BufferBuilder bufferBuilder, Matrix4f matrix, float x, float y, float width, float height, Color color) {
      float a = color.getAlpha() / 255.0f, r = color.getRed() / 255.0f, g = color.getGreen() / 255.0f, b = color.getBlue() / 255.0f;
      bufferBuilder.vertex(matrix, x, y, 0.0f).color(r, g, b, a);
      bufferBuilder.vertex(matrix, x, y + height, 0.0f).color(r, g, b, a);
      bufferBuilder.vertex(matrix, x + width, y + height, 0.0f).color(r, g, b, a);
      bufferBuilder.vertex(matrix, x + width, y, 0.0f).color(r, g, b, a);
   }

   public void drawText(Entity ent, DrawContext context) {
      double x = ent.prevX + (ent.getX() - ent.prevX) * mc.getRenderTickCounter().getTickDelta(true);
      double y = ent.prevY + (ent.getY() - ent.prevY) * mc.getRenderTickCounter().getTickDelta(true);
      double z = ent.prevZ + (ent.getZ() - ent.prevZ) * mc.getRenderTickCounter().getTickDelta(true);
      Box axisAlignedBB2 = ent.getBoundingBox();
      Box axisAlignedBB = new Box(axisAlignedBB2.minX - ent.getX() + x - 0.05, axisAlignedBB2.minY - ent.getY() + y, axisAlignedBB2.minZ - ent.getZ() + z - 0.05, axisAlignedBB2.maxX - ent.getX() + x + 0.05, axisAlignedBB2.maxY - ent.getY() + y + 0.15, axisAlignedBB2.maxZ - ent.getZ() + z + 0.05);
      Vec3d[] vectors = new Vec3d[] { new Vec3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ), new Vec3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ), new Vec3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ), new Vec3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ), new Vec3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ), new Vec3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ), new Vec3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ), new Vec3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ) };
      Vector4d position = null;
      for (Vec3d vector : vectors) {
         vector = TextUtil.worldSpaceToScreenSpace(new Vec3d(vector.x, vector.y, vector.z));
         if (!(vector.z > 0.0) || !(vector.z < 1.0)) continue;
         if (position == null) position = new Vector4d(vector.x, vector.y, vector.z, 0.0);
         position.x = Math.min(vector.x, position.x);
         position.y = Math.min(vector.y, position.y);
         position.z = Math.max(vector.x, position.z);
         position.w = Math.max(vector.y, position.w);
      }
      if (position != null) {
         double posX = position.x, posY = position.y, endPosX = position.z, endPosY = position.w;
         if (ent instanceof ItemEntity entity && this.drawItem.getValue()) {
            float diff = (float)((endPosX - posX) / 2.0);
            float textWidth = FontManager.ui.getWidth(entity.getDisplayName().getString());
            float tagX = (float)(posX + diff - textWidth / 2.0f);
            int count = entity.getStack().getCount();
            context.drawText(mc.textRenderer, entity.getDisplayName().getString(), (int)tagX, (int)(posY - 10.0), this.textcolor.getValue().getRGB(), false);
            if (this.drawItemC.getValue()) {
               context.drawText(mc.textRenderer, "x" + count, (int)(tagX + mc.textRenderer.getWidth(entity.getDisplayName().getString() + " ")), (int)posY - 10, this.countColor.getValue().getRGB(), false);
            }
         }
         if (ent instanceof PlayerEntity player && this.renderArmor.getValue()) {
            double h = (endPosY - posY) / 4.0;
            ArrayList<ItemStack> stacks = new ArrayList<>();
            stacks.add(player.getInventory().armor.get(3));
            stacks.add(player.getInventory().armor.get(2));
            stacks.add(player.getInventory().armor.get(1));
            stacks.add(player.getInventory().armor.get(0));
            int i = -1;
            for (ItemStack armor : stacks) {
               i++;
               if (armor.isEmpty()) continue;
               float durability = armor.getMaxDamage() - armor.getDamage();
               int percent = (int)(durability / (float)armor.getMaxDamage() * 100.0f);
               double finalH = h * (percent / 100.0);
               context.drawItem(armor, (int)(endPosX + 4.0), (int)(posY + h * i + 1.2 * (i + 1) + finalH / 2.0));
            }
         }
      }
   }

   public Color get2DColor(float health) {
      if (health >= 20.0f) return this.hHealth.getValue();
      if (health > 10.0f) return this.mHealth.getValue();
      return this.lHealth.getValue();
   }

   @EventListener
   public void onReceivePacket(EntitySpawnedEvent event) {
      if (!nullCheck() && this.pearl.getValue() && event.getEntity() instanceof EnderPearlEntity pearlEntity) {
         if (pearlEntity.getOwner() != null) {
            pearlEntity.setCustomName(pearlEntity.getOwner().getName());
            pearlEntity.setCustomNameVisible(true);
         } else {
            mc.world.getPlayers().stream().min(Comparator.comparingDouble(p -> p.getPos().distanceTo(new Vec3d(pearlEntity.getX(), pearlEntity.getY(), pearlEntity.getZ())))).ifPresent(player -> {
               pearlEntity.setCustomName(player.getName());
               pearlEntity.setCustomNameVisible(true);
            });
         }
      }
   }

   public enum Type { None, Air, Normal, Bedrock }
   public enum TwoDESPMode { Setting, Target, Color }
}
