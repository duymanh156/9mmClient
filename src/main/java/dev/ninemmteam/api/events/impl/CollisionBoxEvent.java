package dev.ninemmteam.api.events.impl;

import dev.ninemmteam.api.events.Event;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;

public class CollisionBoxEvent extends Event {
    private static final CollisionBoxEvent instance = new CollisionBoxEvent();

    private BlockPos pos;
    private BlockState state;
    private VoxelShape voxelShape;

    private CollisionBoxEvent() {
    }

    public static CollisionBoxEvent get(VoxelShape voxelShape, BlockPos pos, BlockState state) {
        instance.voxelShape = voxelShape;
        instance.pos = pos;
        instance.state = state;
        instance.setCancelled(false);
        return instance;
    }

    public BlockPos getPos() {
        return pos;
    }

    public BlockState getState() {
        return state;
    }

    public VoxelShape getVoxelShape() {
        return voxelShape;
    }

    public void setVoxelShape(VoxelShape voxelShape) {
        this.voxelShape = voxelShape;
    }
}
