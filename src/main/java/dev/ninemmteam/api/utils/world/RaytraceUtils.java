package dev.ninemmteam.api.utils.world;

import dev.ninemmteam.api.utils.Wrapper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;

import java.util.Optional;

public class RaytraceUtils implements Wrapper {

    public static BlockHitResult getBlockHitResult(float yaw, float pitch) {
        return getBlockHitResult(yaw, pitch, (float) mc.player.getAttributes().getValue(EntityAttributes.PLAYER_BLOCK_INTERACTION_RANGE));
    }

    public static BlockHitResult getBlockHitResult(float yaw, float pitch, float distance) {
        return getBlockHitResult(yaw, pitch, distance, mc.player, RaycastContext.ShapeType.VISUAL);
    }

    public static BlockHitResult getBlockHitResult(float yaw, float pitch, float d, Entity from, RaycastContext.ShapeType shapeType) {
        Vec3d vec3d = mc.player.getEyePos();
        Vec3d lookVec = getRotationVector(yaw, pitch);
        Vec3d rotations = vec3d.add(lookVec.x * d, lookVec.y * d, lookVec.z * d);

        return Optional.ofNullable(
                        mc.world.raycast(new RaycastContext(vec3d, rotations, shapeType, RaycastContext.FluidHandling.NONE, from)))
                .orElseGet(() ->
                        new BlockHitResult(new Vec3d(0.5, 1.0, 0.5), Direction.UP, BlockPos.ORIGIN, false));
    }

    public static HitResult isLookingResult(Entity camera, Entity target, Vec3d position, final float[] angles, float reach) {
        double d = reach;
        double e = MathHelper.square(d);

        Vec3d vec3d = position;
        Vec3d vec3d2 = getRotationVector(angles[0], angles[1]);
        Vec3d vec3d3 = vec3d.add(vec3d2.x * d, vec3d2.y * d, vec3d2.z * d);
        Box box = camera.getBoundingBox().stretch(vec3d2.multiply(d)).expand(1.0, 1.0, 1.0);
        return ProjectileUtil.raycast(camera, vec3d, vec3d3, box, (entity) -> entity.canHit() && entity.equals(target), e);
    }

    public static Vec3d getRotationVector(float pitch, float yaw) {
        float f = pitch * ((float)Math.PI / 180);
        float g = -yaw * ((float)Math.PI / 180);
        float h = MathHelper.cos(g);
        float i = MathHelper.sin(g);
        float j = MathHelper.cos(f);
        float k = MathHelper.sin(f);
        return new Vec3d(i * j, -k, h * j);
    }
}
