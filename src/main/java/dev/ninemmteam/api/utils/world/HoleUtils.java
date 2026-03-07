package dev.ninemmteam.api.utils.world;

import dev.ninemmteam.api.utils.Wrapper;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class HoleUtils implements Wrapper {

    public static boolean isHole(BlockPos pos) {
        int amount = 0;
        for (BlockPos p : holeOffsets) {
            if (!mc.world.getBlockState(pos.add(p)).isReplaceable()) {
                amount++;
            }
        }
        return amount == 5;
    }

    public static boolean isSurrounded(BlockPos pos) {
        int amount = 0;
        for (BlockPos p : surroundOffsets) {
            if (!mc.world.getBlockState(pos.add(p)).isReplaceable()) {
                amount++;
            }
        }
        return amount == 4;
    }

    public static boolean isBurrowed(PlayerEntity entityPlayer) {
        BlockPos blockPos = BlockPos.ofFloored(entityPlayer.getX(), entityPlayer.getY() + 0.2, entityPlayer.getZ());
        Block block = mc.world.getBlockState(blockPos).getBlock();
        return block == Blocks.ENDER_CHEST || block == Blocks.OBSIDIAN || block == Blocks.CHEST;
    }

    public static boolean isInBlock(PlayerEntity entityPlayer) {
        BlockPos blockPos = BlockPos.ofFloored(entityPlayer.getX(), entityPlayer.getY() + 0.2, entityPlayer.getZ());
        return mc.world.getBlockState(blockPos).getBlock() != Blocks.AIR;
    }

    public static boolean isObbyHole(BlockPos pos) {
        boolean isHole = true;
        int bedrock = 0;

        for (BlockPos off : holeOffsets) {
            Block b = mc.world.getBlockState(pos.add(off)).getBlock();

            if (!isSafeBlock(pos.add(off))) {
                isHole = false;
            } else {
                if (b == Blocks.OBSIDIAN || b == Blocks.ENDER_CHEST || b == Blocks.ANVIL) {
                    bedrock++;
                }
            }
        }

        if (mc.world.getBlockState(pos.add(0, 2, 0)).getBlock() != Blocks.AIR || 
            mc.world.getBlockState(pos.add(0, 1, 0)).getBlock() != Blocks.AIR) {
            isHole = false;
        }

        if (bedrock < 1) {
            isHole = false;
        }
        return isHole;
    }

    public static boolean isBedrockHole(BlockPos pos) {
        boolean isHole = true;

        for (BlockPos off : holeOffsets) {
            Block b = mc.world.getBlockState(pos.add(off)).getBlock();
            if (b != Blocks.BEDROCK) {
                isHole = false;
            }
        }

        if (mc.world.getBlockState(pos.add(0, 2, 0)).getBlock() != Blocks.AIR || 
            mc.world.getBlockState(pos.add(0, 1, 0)).getBlock() != Blocks.AIR) {
            isHole = false;
        }

        return isHole;
    }

    public static Hole isDoubleHole(BlockPos pos) {
        if (checkOffset(pos, 1, 0)) {
            return new Hole(false, true, pos, pos.add(1, 0, 0));
        }
        if (checkOffset(pos, 0, 1)) {
            return new Hole(false, true, pos, pos.add(0, 0, 1));
        }
        return null;
    }

    private static boolean checkOffset(BlockPos pos, int offX, int offZ) {
        return mc.world.getBlockState(pos).getBlock() == Blocks.AIR && 
               mc.world.getBlockState(pos.add(offX, 0, offZ)).getBlock() == Blocks.AIR && 
               isSafeBlock(pos.add(0, -1, 0)) && 
               isSafeBlock(pos.add(offX, -1, offZ)) && 
               isSafeBlock(pos.add(offX * 2, 0, offZ * 2)) && 
               isSafeBlock(pos.add(-offX, 0, -offZ)) && 
               isSafeBlock(pos.add(offZ, 0, offX)) && 
               isSafeBlock(pos.add(-offZ, 0, -offX)) && 
               isSafeBlock(pos.add(offX, 0, offZ).add(offZ, 0, offX)) && 
               isSafeBlock(pos.add(offX, 0, offZ).add(-offZ, 0, -offX));
    }

    private static boolean isSafeBlock(BlockPos pos) {
        Block block = mc.world.getBlockState(pos).getBlock();
        return block == Blocks.OBSIDIAN || block == Blocks.BEDROCK || block == Blocks.ENDER_CHEST;
    }

    public static final BlockPos[] holeOffsets = new BlockPos[]{
        new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0), 
        new BlockPos(0, 0, 1), new BlockPos(0, 0, -1), 
        new BlockPos(0, -1, 0)
    };

    public static final BlockPos[] surroundOffsets = new BlockPos[]{
        new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0), 
        new BlockPos(0, 0, 1), new BlockPos(0, 0, -1)
    };

    public static List<Hole> getHoles(double range, BlockPos playerPos, boolean doubles) {
        List<Hole> holes = new ArrayList<>();
        List<BlockPos> circle = BlockUtil.getSphere((float) range);

        for (BlockPos pos : circle) {
            if (mc.world.getBlockState(pos).getBlock() == Blocks.AIR) {
                if (isObbyHole(pos)) {
                    holes.add(new Hole(false, false, pos));
                    continue;
                }

                if (isBedrockHole(pos)) {
                    holes.add(new Hole(true, false, pos));
                    continue;
                }

                if (doubles) {
                    Hole dh = isDoubleHole(pos);
                    if (dh != null) {
                        if (mc.world.getBlockState(dh.pos1.add(0, 1, 0)).getBlock() == Blocks.AIR || 
                            mc.world.getBlockState(dh.pos2.add(0, 1, 0)).getBlock() == Blocks.AIR) {
                            holes.add(dh);
                        }
                    }
                }
            }
        }

        return holes;
    }

    public static class Hole {
        public boolean bedrock;
        public boolean doubleHole;
        public BlockPos pos1;
        public BlockPos pos2;
        public BlockPos toTarget;

        public Hole(boolean bedrock, boolean doubleHole, BlockPos pos1, BlockPos pos2) {
            this.bedrock = bedrock;
            this.doubleHole = doubleHole;
            this.pos1 = pos1;
            this.pos2 = pos2;
        }

        public Hole(boolean bedrock, boolean doubleHole, BlockPos pos1) {
            this.bedrock = bedrock;
            this.doubleHole = doubleHole;
            this.pos1 = pos1;
        }
    }
}
