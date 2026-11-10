package com.cheng.search_bot.entity.ai;

import com.cheng.search_bot.entity.custom.SearchRobotEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * 跟随主人的 AI 目标。
 *
 * <h3>停止条件</h3>
 * - 飞行：3D 距离
 * - 步行（车轮）：2D 距离
 */
public class FollowOwnerGoal extends Goal {
    private final SearchRobotEntity robot;
    private final double maxSpeed;
    private final float stopDistance;
    private final float startDistance;
    private LivingEntity owner;
    private int timeToRecalcPath = 0;

    private double currentSpeed = 0.0;
    private static final double SPEED_LERP = 0.25;
    private static final double DECEL_LERP = 0.6;
    private static final float HYSTERESIS = 1.0f;
    private static final float FULL_SPEED_ZONE = 5.5f;

    public FollowOwnerGoal(SearchRobotEntity robot, double maxSpeed, float startDist, float stopDist) {
        this.robot = robot;
        this.maxSpeed = maxSpeed;
        this.startDistance = startDist;
        this.stopDistance = stopDist;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    // ==================== 距离计算 ====================

    private double distSqr3D() {
        if (owner == null) return Double.MAX_VALUE;
        return robot.distanceToSqr(owner);
    }

    private double distSqr2D() {
        if (owner == null) return Double.MAX_VALUE;
        double dx = robot.getX() - owner.getX();
        double dz = robot.getZ() - owner.getZ();
        return dx * dx + dz * dz;
    }

    /** 启动/停止条件用的距离：飞行用 3D，步行用 2D */
    private double targetDistSqr() {
        if (robot.isFlyingActive()) return distSqr3D();
        return distSqr2D();
    }

    private float speedDist() {
        return (float) Math.sqrt(distSqr3D());
    }

    // ==================== Goal 生命周期 ====================

    @Override
    public boolean canUse() {
        LivingEntity owner = robot.getOwner();
        if (owner == null) return false;
        if (owner.isSpectator()) return false;

        SearchRobotEntity.RobotState state = robot.getRobotState();
        if (state == SearchRobotEntity.RobotState.RETURNING_TO_CHARGE
                || state == SearchRobotEntity.RobotState.CHARGING) {
            return false;
        }

        if (state == SearchRobotEntity.RobotState.OUT_OF_POWER) {
            this.owner = owner;
            return true;
        }

        if (robot.getEnergy() <= 0) return false;

        // 标准距离检查
        float triggerDist = startDistance + HYSTERESIS;
        if (targetDistSqr() < (double) (triggerDist * triggerDist)) return false;

        this.owner = owner;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (owner == null || owner.isSpectator()) return false;

        SearchRobotEntity.RobotState state = robot.getRobotState();
        if (state == SearchRobotEntity.RobotState.RETURNING_TO_CHARGE
                || state == SearchRobotEntity.RobotState.CHARGING) {
            return false;
        }

        if (state == SearchRobotEntity.RobotState.OUT_OF_POWER) {
            return targetDistSqr() > (double) (stopDistance * stopDistance);
        }

        if (robot.getEnergy() <= 0) return false;

        return targetDistSqr() > (double) (stopDistance * stopDistance);
    }

    @Override
    public void start() {
        timeToRecalcPath = 0;
        currentSpeed = 0.0;
    }

    @Override
    public void stop() {
        this.owner = null;
        robot.getNavigation().stop();
        robot.getMoveControl().setWantedPosition(robot.getX(), robot.getY(), robot.getZ(), 0);
        currentSpeed = 0.0;
    }

    // ==================== 每 tick 执行 ====================

    @Override
    public void tick() {
        if (owner == null) return;

        robot.getLookControl().setLookAt(owner, robot.getMaxHeadYRot(), robot.getMaxHeadXRot());

        float dist = speedDist();

        // 传送装置：距离主人超过 10 格时直接瞬移（本 tick 由传送接管，不再寻路）
        if (robot.teleportIfFar(owner.blockPosition())) return;

        double targetSpeed;
        if (dist <= stopDistance) {
            targetSpeed = 0;
        } else if (dist <= FULL_SPEED_ZONE) {
            float t = (dist - stopDistance) / (FULL_SPEED_ZONE - stopDistance);
            targetSpeed = maxSpeed * t;
        } else {
            targetSpeed = maxSpeed;
        }

        if (robot.getRobotState() == SearchRobotEntity.RobotState.OUT_OF_POWER) {
            targetSpeed *= 0.5;
        }

        // 惯性修正：到达停止距离后立即刹停（清速度 + 停寻路），
        // 避免高速逼近时「刹不住」冲过头；减速用更快的 lerp（DECEL_LERP > SPEED_LERP），
        // 加速保持平缓，减少追人时的过冲振荡。
        if (targetSpeed <= 0.0) {
            currentSpeed = 0.0;
            robot.getNavigation().stop();
            robot.getMoveControl().setWantedPosition(robot.getX(), robot.getY(), robot.getZ(), 0);
            return;
        }
        double lerp = (targetSpeed < currentSpeed) ? DECEL_LERP : SPEED_LERP;
        currentSpeed += (targetSpeed - currentSpeed) * lerp;
        if (Math.abs(currentSpeed) < 0.001) currentSpeed = 0;

        if (--timeToRecalcPath <= 0) {
            timeToRecalcPath = 5;

            if (robot.isFlyingActive()) {
                // 飞行：直线飞向主人头顶上方并悬停，固定高度避免一上一下
                robot.getMoveControl().setWantedPosition(
                        owner.getX(), owner.getY() + 1.0, owner.getZ(), currentSpeed);
            } else {
                robot.getNavigation().moveTo(owner, currentSpeed);
            }
        }
    }
}
