package dev.ninemmteam.mod.modules.impl.combat;

import com.mojang.authlib.GameProfile;
import dev.ninemmteam.fentanyl;
import dev.ninemmteam.api.events.eventbus.EventListener;
import dev.ninemmteam.api.events.impl.UpdateEvent;
import dev.ninemmteam.api.utils.Wrapper;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.settings.impl.BooleanSetting;
import dev.ninemmteam.mod.modules.settings.impl.SliderSetting;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class AutoTK extends Module {
    public static AutoTK INSTANCE;
    
    private final SliderSetting range = this.add(new SliderSetting("Range", 10.0, 1.0, 50.0, 0.1));
    private final BooleanSetting autoTotem = this.add(new BooleanSetting("AutoTotem", true));
    private final BooleanSetting onlyFriends = this.add(new BooleanSetting("OnlyFriends", false));
    private final BooleanSetting copyState = this.add(new BooleanSetting("CopyState", true));
    private final BooleanSetting invisible = this.add(new BooleanSetting("Invisible", false));
    
    private final Map<PlayerEntity, FakePlayerEntity> fakePlayers = new HashMap<>();
    private final List<PlayerEntity> processedPlayers = new ArrayList<>();
    
    public AutoTK() {
        super("Spy", Module.Category.Combat);
        this.setChinese("Spy");
        INSTANCE = this;
    }
    
    @Override
    public void onEnable() {
        if (nullCheck()) {
            this.disable();
            return;
        }
        
        fakePlayers.clear();
        processedPlayers.clear();
    }
    
    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (nullCheck()) {
            this.disable();
            return;
        }
        
        if (mc.world == null || mc.player == null) {
            return;
        }
        
        List<PlayerEntity> nearbyPlayers = getNearbyPlayers();
        
        for (PlayerEntity player : nearbyPlayers) {
            if (shouldSkipPlayer(player)) {
                continue;
            }
            
            if (!fakePlayers.containsKey(player)) {
                createFakePlayerFor(player);
            } else {
                updateFakePlayerPosition(player, fakePlayers.get(player));
            }
            
            if (!processedPlayers.contains(player)) {
                processedPlayers.add(player);
            }
        }
        
        cleanupRemovedPlayers(nearbyPlayers);
    }
    
    @Override
    public void onDisable() {
        removeAllFakePlayers();
        processedPlayers.clear();
    }
    
    private List<PlayerEntity> getNearbyPlayers() {
        List<PlayerEntity> players = new ArrayList<>();
        
        if (mc.world != null && mc.player != null) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player != mc.player && 
                    player.distanceTo(mc.player) <= range.getValue() &&
                    !player.isRemoved()) {
                    players.add(player);
                }
            }
        }
        
        return players;
    }
    
    private boolean shouldSkipPlayer(PlayerEntity player) {
        if (player == mc.player || player.isRemoved()) {
            return true;
        }
        
        if (onlyFriends.getValue() && !isFriend(player)) {
            return true;
        }
        
        if (isFakePlayer(player)) {
            return true;
        }
        
        return false;
    }
    
    private boolean isFakePlayer(PlayerEntity player) {
        return player.getName().getString().startsWith("TK_");
    }
    
    private boolean isFriend(PlayerEntity player) {
        return fentanyl.FRIEND != null &&
               fentanyl.FRIEND.isFriend(player);
    }
    
    private void createFakePlayerFor(PlayerEntity targetPlayer) {
        FakePlayerEntity fakePlayer = new FakePlayerEntity(targetPlayer, "TK_" + targetPlayer.getName().getString());
        mc.world.addEntity(fakePlayer);
        fakePlayers.put(targetPlayer, fakePlayer);
    }
    
    private void updateFakePlayerPosition(PlayerEntity targetPlayer, FakePlayerEntity fakePlayer) {
        fakePlayer.setYaw(targetPlayer.getYaw());
        fakePlayer.setPitch(targetPlayer.getPitch());
        fakePlayer.setHeadYaw(targetPlayer.getYaw());
        fakePlayer.updateTrackedPosition(targetPlayer.getX(), targetPlayer.getY(), targetPlayer.getZ());
        fakePlayer.updateTrackedPositionAndAngles(
            targetPlayer.getX(), targetPlayer.getY(), targetPlayer.getZ(), 
            targetPlayer.getYaw(), targetPlayer.getPitch(), 3
        );
        
        if (copyState.getValue()) {
            copyPlayerState(targetPlayer, fakePlayer);
        }
        
        if (autoTotem.getValue()) {
            if (fakePlayer.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
                fakePlayer.setStackInHand(Hand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
            }
            
            if (fakePlayer.getMainHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
                fakePlayer.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
            }
        }
        
        if (invisible.getValue()) {
            fakePlayer.setInvisible(true);
            fakePlayer.setCustomNameVisible(false);
        }
    }
    
    private void copyPlayerState(PlayerEntity source, FakePlayerEntity target) {
        target.setSneaking(source.isSneaking());
        target.setSprinting(source.isSprinting());
        target.setPose(source.getPose());
        
        target.handSwingProgress = source.handSwingProgress;
        target.handSwingTicks = source.handSwingTicks;
        target.lastHandSwingProgress = source.lastHandSwingProgress;
        
        target.limbAnimator.setSpeed(source.limbAnimator.getSpeed());
        target.limbAnimator.pos = source.limbAnimator.getPos();
        
        target.setStackInHand(Hand.MAIN_HAND, source.getMainHandStack());
        target.setStackInHand(Hand.OFF_HAND, source.getOffHandStack());
        
        target.setHealth(source.getHealth());
        
        target.setOnGround(source.isOnGround());
        
        if (source.getVehicle() != null) {
            target.startRiding(source.getVehicle(), true);
        } else if (target.getVehicle() != null) {
            target.stopRiding();
        }
    }
    
    private void cleanupRemovedPlayers(List<PlayerEntity> currentPlayers) {
        List<PlayerEntity> toRemove = new ArrayList<>();
        
        for (PlayerEntity player : fakePlayers.keySet()) {
            if (!currentPlayers.contains(player) || player.isRemoved()) {
                toRemove.add(player);
            }
        }
        
        for (PlayerEntity player : toRemove) {
            removeFakePlayer(player);
        }
    }
    
    private void removeFakePlayer(PlayerEntity player) {
        FakePlayerEntity fakePlayer = fakePlayers.get(player);
        if (fakePlayer != null) {
            fakePlayer.kill();
            fakePlayer.setRemoved(RemovalReason.KILLED);
            fakePlayer.onRemoved();
        }
        fakePlayers.remove(player);
        processedPlayers.remove(player);
    }
    
    private void removeAllFakePlayers() {
        for (FakePlayerEntity fakePlayer : fakePlayers.values()) {
            if (fakePlayer != null) {
                fakePlayer.kill();
                fakePlayer.setRemoved(RemovalReason.KILLED);
                fakePlayer.onRemoved();
            }
        }
        fakePlayers.clear();
    }
    
    public static class FakePlayerEntity extends OtherClientPlayerEntity {
        private final boolean onGround;
        
        public FakePlayerEntity(PlayerEntity player, String name) {
            super(Wrapper.mc.world, new GameProfile(UUID.fromString("77777777-7777-7777-7777-777777777777"), name));
            this.copyPositionAndRotation(player);
            this.prevX = player.prevX;
            this.prevZ = player.prevZ;
            this.prevY = player.prevY;
            this.bodyYaw = player.bodyYaw;
            this.headYaw = player.headYaw;
            this.handSwingProgress = player.handSwingProgress;
            this.handSwingTicks = player.handSwingTicks;
            this.limbAnimator.setSpeed(player.limbAnimator.getSpeed());
            this.limbAnimator.pos = player.limbAnimator.getPos();
            this.touchingWater = player.isTouchingWater();
            this.setSneaking(player.isSneaking());
            this.setPose(player.getPose());
            this.setFlag(7, player.isFallFlying());
            this.onGround = player.isOnGround();
            this.setOnGround(this.onGround);
            this.getInventory().clone(player.getInventory());
            this.setAbsorptionAmountUnclamped(player.getAbsorptionAmount());
            this.setHealth(player.getHealth());
            this.setBoundingBox(player.getBoundingBox());
        }
        
        public boolean isOnGround() {
            return this.onGround;
        }
        
        public boolean isSpectator() {
            return false;
        }
        
        public boolean isCreative() {
            return false;
        }
    }
}