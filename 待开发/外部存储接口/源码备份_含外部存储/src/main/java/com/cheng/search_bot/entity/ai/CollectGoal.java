package com.cheng.search_bot.entity.ai;

import com.cheng.search_bot.entity.custom.SearchRobotEntity;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import org.slf4j.Logger;

import java.util.EnumSet;

/**
 * 收集模式 AI 目标：把目标容器搜索半径（传感器）内「其他容器」的物品，
 * 集中搬进目标容器（collectTargetPos）。
 *
 * <p>状态机与运输模式类似：未携带货物时找一个有物品的源容器取物，
 * 取到后去目标容器卸货，卸空后继续找下一个源容器，直到范围内没有可收集的容器。
 */
public class CollectGoal extends Goal {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final SearchRobotEntity robot;
    private boolean carrying = false;
    private BlockPos currentSource = null;
    private int idleCooldown = 0;
    private int repathCooldown = 0;
    private int diagCooldown = 0;
    private static final int IDLE_COOLDOWN = 40;        // 无源容器时等待 2 秒

    public CollectGoal(SearchRobotEntity robot) {
        this.robot = robot;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (robot.getMode() != SearchRobotEntity.RobotMode.COLLECT) return false;
        if (robot.getRobotState() != SearchRobotEntity.RobotState.WORKING) return false;
        if (robot.getEnergy() <= 0) return false;
        if (idleCooldown > 0) {
            idleCooldown--;
            return false;
        }
        return robot.getCollectTargetPos() != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (robot.getRobotState() != SearchRobotEntity.RobotState.WORKING) return false;
        if (robot.getEnergy() <= 0) return false;
        return robot.getCollectTargetPos() != null;
    }

    @Override
    public void start() {
        repathCooldown = 0;
    }

    @Override
    public void stop() {
        carrying = false;
        currentSource = null;
        robot.getNavigation().stop();
    }

    @Override
    public void tick() {
        BlockPos target = robot.getCollectTargetPos();
        if (target == null) return;

        // 诊断：每 5 秒打印一次收集状态，便于排查“不收集”问题
        if (--diagCooldown <= 0) {
            diagCooldown = 100;
            LOGGER.info("[CollectGoal] 目标={}, 携带中={}, 当前源={}, 背包满={}, 背包空={}, 搜索半径={}",
                    target, carrying, currentSource, robot.isInventoryFull(), robot.isInventoryEmpty(),
                    robot.getSearchRadius());
        }

        if (!carrying) {
            // 更高优先判断：背包满了必须返回目标容器（可能上一轮没取到就满了，兜底）
            if (robot.isInventoryFull()) {
                carrying = true;
                currentSource = null;
                return;
            }
            // 未携带货物：找一个就近的有物品源容器
            if (currentSource == null || !robot.containerHasItems(currentSource)) {
                currentSource = findSourceContainer(target);
            }
            if (currentSource == null) {
                // 范围内没有可收集的源容器：回到收集目标容器附近等待（不打扰主人）
                double dist = robot.distanceToSqr(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
                double reach = Math.max(robot.getInteractionRadius() + 0.5, 2.0);
                if (dist > reach * reach) {
                    moveTo(target); // 还没到收集容器附近，先走过去
                } else {
                    robot.smoothStop(); // 已在附近，原地等待
                    idleCooldown = IDLE_COOLDOWN; // 节流重扫，避免每 tick 全半径扫描
                }
                return;
            }

            // 有目标时主动看着当前要去的源容器
            robot.getLookControl().setLookAt(
                    currentSource.getX() + 0.5, currentSource.getY(), currentSource.getZ() + 0.5,
                    robot.getMaxHeadYRot(), robot.getMaxHeadXRot());

            double dist = robot.distanceToSqr(
                    currentSource.getX() + 0.5, currentSource.getY(), currentSource.getZ() + 0.5);
            // 到达判定：机械臂半径 + 半格补偿，至少 2 格，避免寻路停在容器旁仍判未到达而罚站
            double reach = Math.max(robot.getInteractionRadius() + 0.5, 2.0);
            if (dist <= reach * reach) {
                robot.smoothStop(); // 交互半径内不再移动，就地取物（一次取到源空或背包满）
                boolean moved = robot.transferFromContainerToInventory(currentSource);
                if (moved || robot.isInventoryFull()) {
                    carrying = true;
                }
                currentSource = null;
            } else {
                moveTo(currentSource);
            }
        } else {
            // 携带货物：去目标容器卸货，同时看着目标容器
            robot.getLookControl().setLookAt(
                    target.getX() + 0.5, target.getY(), target.getZ() + 0.5,
                    robot.getMaxHeadYRot(), robot.getMaxHeadXRot());

            double dist = robot.distanceToSqr(
                    target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
            // 到达判定：机械臂半径 + 半格补偿，至少 2 格，避免寻路停在容器旁仍判未到达而罚站
            double reach = Math.max(robot.getInteractionRadius() + 0.5, 2.0);
            if (dist <= reach * reach) {
                robot.smoothStop(); // 交互半径内不再移动
                // 目标容器已满：原地不动等待（等玩家腾空目标容器）
                if (robot.containerIsFull(target)) {
                    return;
                }
                robot.transferFromInventoryToContainer(target);
                if (robot.isInventoryEmpty()) {
                    carrying = false;
                }
            } else {
                moveTo(target);
            }
        }
    }

    /** 在目标容器半径内搜索最近的有物品源容器（排除目标容器本身） */
    private BlockPos findSourceContainer(BlockPos target) {
        BlockPos closest = null;
        double closestDist = Double.MAX_VALUE;
        int radius = robot.getSearchRadius();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = target.offset(x, y, z);
                    if (pos.equals(target)) continue;
                    if (robot.containerHasItems(pos)) {
                        double dist = target.distSqr(pos);
                        if (dist < closestDist) {
                            closestDist = dist;
                            closest = pos;
                        }
                    }
                }
            }
        }
        return closest;
    }

    private void moveTo(BlockPos pos) {
        // 装有传送装置：距离目标超过 10 格时直接瞬移（本 tick 由传送接管，不再寻路）；10 格内走常规寻路
        if (robot.teleportIfFar(pos)) return;
        if (--repathCooldown <= 0) {
            repathCooldown = 15;
            robot.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.0);
        }
    }
}
