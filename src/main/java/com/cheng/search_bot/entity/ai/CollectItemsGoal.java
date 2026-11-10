package com.cheng.search_bot.entity.ai;

import com.cheng.search_bot.entity.custom.SearchRobotEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * 探索模式下的「主动收集」AI 目标。
 *
 * <p>探索模式第一要务是收集，但也要保证跟着玩家。因此：
 * <ul>
 *   <li>只在玩家附近搜索半径（传感器）格内搜索容器，不会跑远。</li>
 *   <li>找到容器后主动寻路过去，到达后取物。</li>
 *   <li>找不到容器时每 {@link #SCAN_INTERVAL} tick 重扫一次，避免每 tick 全范围扫描拖慢 TPS。</li>
 *   <li>背包满时提示玩家「仓库已满」，并自动切回跟随模式。</li>
 * </ul>
 *
 * <p>跟随模式的「路过收集」由 {@link SearchRobotEntity#tickCollectItems()} 独立模块处理，
 * 本 Goal 不参与跟随模式。
 */
public class CollectItemsGoal extends Goal {
    private final SearchRobotEntity robot;
    private static final int SCAN_INTERVAL = 20;      // 无目标时每 1 秒重扫一次
    private BlockPos targetContainer = null;
    private int scanCooldown = 0;

    public CollectItemsGoal(SearchRobotEntity robot) {
        this.robot = robot;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (robot.getMode() != SearchRobotEntity.RobotMode.EXPLORE) return false;
        if (robot.getRobotState() != SearchRobotEntity.RobotState.WORKING) return false;
        if (robot.getEnergy() <= 0) return false;
        if (robot.isRedstonePaused()) return false;
        if (robot.isInventoryFull()) {
            handleInventoryFull();
            return false;
        }

        if (scanCooldown > 0) {
            scanCooldown--;
            return targetContainer != null && isContainerValid(targetContainer);
        }

        targetContainer = findContainerNearOwner();
        if (targetContainer == null) {
            scanCooldown = SCAN_INTERVAL; // 没找到，1 秒后再扫
            return false;
        }
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (robot.getRobotState() != SearchRobotEntity.RobotState.WORKING) return false;
        if (robot.getEnergy() <= 0) return false;
        if (robot.isRedstonePaused()) return false;
        if (robot.isInventoryFull()) {
            handleInventoryFull();
            return false;
        }
        return targetContainer != null && isContainerValid(targetContainer) && isNearOwner(targetContainer);
    }

    @Override
    public void start() {
        // 首次寻路在 tick 中处理
    }

    @Override
    public void stop() {
        targetContainer = null;
    }

    @Override
    public void tick() {
        if (targetContainer == null) return;

        double distSqr = robot.distanceToSqr(
                targetContainer.getX() + 0.5, targetContainer.getY(), targetContainer.getZ() + 0.5);
        double interact = robot.getInteractionRadius();
        if (distSqr <= interact * interact) {
            robot.smoothStop(); // 交互半径内不再移动，就地收集
            robot.collectFromContainer(targetContainer);
            targetContainer = null;
        } else {
            // 装有传送装置：距离目标超过 10 格时直接瞬移（本 tick 由传送接管）；10 格内走常规寻路
            if (!robot.teleportIfFar(targetContainer)) {
                robot.getNavigation().moveTo(
                        targetContainer.getX() + 0.5, targetContainer.getY(), targetContainer.getZ() + 0.5, 1.0);
            }
        }
    }

    /** 背包满：提示玩家并切回跟随模式（探索模式特定行为） */
    private void handleInventoryFull() {
        robot.notifyOwner("机器人的仓库满了，已自动切换为跟随模式");
        robot.switchMode(SearchRobotEntity.RobotMode.FOLLOW);
    }

    /** 以玩家为中心搜索最近的有物品容器 */
    private BlockPos findContainerNearOwner() {
        Player owner = robot.getOwner();
        BlockPos center = owner != null ? owner.blockPosition() : robot.blockPosition();
        BlockPos closest = null;
        double closestDist = Double.MAX_VALUE;
        int radius = robot.getSearchRadius();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (robot.containerHasCollectibleItems(pos)) {
                        double dist = center.distSqr(pos);
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

    /** 容器仍有可拿取的物品（已空或被过滤的容器视为无效） */
    private boolean isContainerValid(BlockPos pos) {
        return robot.containerHasCollectibleItems(pos);
    }

    /** 目标容器仍在玩家附近（防止玩家走远后机器人还去追远容器） */
    private boolean isNearOwner(BlockPos pos) {
        Player owner = robot.getOwner();
        BlockPos center = owner != null ? owner.blockPosition() : robot.blockPosition();
        int radius = robot.getSearchRadius();
        return Math.abs(center.getX() - pos.getX()) <= radius
                && Math.abs(center.getZ() - pos.getZ()) <= radius
                && Math.abs(center.getY() - pos.getY()) <= 3;
    }

}
