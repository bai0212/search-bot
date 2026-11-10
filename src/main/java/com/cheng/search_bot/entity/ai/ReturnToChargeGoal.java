package com.cheng.search_bot.entity.ai;

import com.cheng.search_bot.block.entity.custom.ChargingStationBlockEntity;
import com.cheng.search_bot.entity.custom.SearchRobotEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 低电量 / 没电返航 Goal：主动寻路到充电站并存入充电。
 *
 * <p>不限制电量：没电（0 电）也照常返航。实体 tick 里已把 0 电速度降到 0.02f，
 * 因此 0 电机器人会非常缓慢地移动到充电站（最初设定）。
 */
public class ReturnToChargeGoal extends Goal {
    private static final double ARRIVE_DIST_SQR = 4.0; // 距充电站中心 2 格内视为到达

    private final SearchRobotEntity robot;

    public ReturnToChargeGoal(SearchRobotEntity robot) {
        this.robot = robot;
    }

    @Override
    public boolean canUse() {
        return robot.getRobotState() == SearchRobotEntity.RobotState.RETURNING_TO_CHARGE
                && robot.getTargetChargingStation() != null;
    }

    @Override
    public void tick() {
        BlockPos target = robot.getTargetChargingStation();
        if (target == null) return;

        // 到达充电站附近 → 直接存入充电站，进入充电流程
        if (robot.distanceToSqr(
                target.getX() + 0.5,
                target.getY() + 0.5,
                target.getZ() + 0.5
        ) < ARRIVE_DIST_SQR) {
            BlockEntity be = robot.level().getBlockEntity(target);
            if (be instanceof ChargingStationBlockEntity station && station.insertRobot(robot)) {
                robot.setRobotState(SearchRobotEntity.RobotState.CHARGING);
            }
            return;
        }

        // 寻路返航：0 电由实体 tick 统一降到 0.02f 缓慢移动
        robot.getNavigation().moveTo(
                target.getX() + 0.5,
                target.getY(),
                target.getZ() + 0.5,
                robot.getFollowSpeed() * 1.5
        );
    }

    @Override
    public void stop() {
        robot.getNavigation().stop();
    }
}
