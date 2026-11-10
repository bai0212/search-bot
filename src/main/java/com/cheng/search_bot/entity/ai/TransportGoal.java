package com.cheng.search_bot.entity.ai;

import com.cheng.search_bot.entity.custom.SearchRobotEntity;
import com.cheng.search_bot.entity.custom.SearchRobotEntity.TransportWaypoint;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 运输模式 AI 目标：就近贪心 + 遍历，走成一笔画闭环。
 *
 * <p>规则：
 * <ul>
 *   <li>把所有运输点位（起点、终点）放进同一个环里：从机器人当前位置出发，每次选「最近的未访问点位」，
 *       生成一个固定遍历顺序（就近贪心），之后按这个顺序循环走，每个点位每圈只走一次，形成闭环。</li>
 *   <li>起点 = 从容器搬入背包；终点 = 从背包搬入容器。到达点位后按实际情况搬运，没货可搬就空跑一次。</li>
 *   <li>若一整圈没有任何实际搬运，说明暂时无活，原地等 2 秒再重扫。</li>
 *   <li>停留时长：起点 {@link #DWELL_START}（0.5 秒）；终点 = (起点数 × DWELL_START) ÷ 终点数（向下取整，至少 1）。</li>
 * </ul>
 */
public class TransportGoal extends Goal {
    private static final int DWELL_START = 10;       // 起点停留 0.5 秒
    private static final int RECHECK_TICKS = 40;     // 一整圈无搬运时每 2 秒重扫一次

    private final SearchRobotEntity robot;
    private TransportWaypoint currentTarget = null;
    private boolean dwelling = false;
    private int dwellTimer = 0;
    private int repathCooldown = 0;
    private int recheckCooldown = 0;

    // 就近贪心生成的固定遍历顺序（环）；点位变化时重建
    private final List<TransportWaypoint> traversalOrder = new ArrayList<>();
    private int orderIndex = 0;
    private boolean loopJustCompleted = false;   // 是否刚走完一整圈
    private boolean movedThisRound = false;      // 这一圈是否真的搬运过物品

    public TransportGoal(SearchRobotEntity robot) {
        this.robot = robot;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (robot.getMode() != SearchRobotEntity.RobotMode.TRANSPORT) return false;
        if (robot.getRobotState() != SearchRobotEntity.RobotState.WORKING) return false;
        if (robot.getEnergy() <= 0) return false;
        if (robot.isRedstonePaused()) return false;
        return !robot.getTransportWaypoints().isEmpty();
    }

    @Override
    public boolean canContinueToUse() {
        if (robot.getRobotState() != SearchRobotEntity.RobotState.WORKING) return false;
        if (robot.getEnergy() <= 0) return false;
        if (robot.isRedstonePaused()) return false;
        return !robot.getTransportWaypoints().isEmpty();
    }

    @Override
    public void start() {
        currentTarget = null;
        dwelling = false;
        repathCooldown = 0;
        recheckCooldown = 0;
        traversalOrder.clear();
        orderIndex = 0;
        loopJustCompleted = false;
        movedThisRound = false;
    }

    @Override
    public void stop() {
        dwelling = false;
        currentTarget = null;
        robot.getNavigation().stop();
    }

    @Override
    public void tick() {
        List<TransportWaypoint> waypoints = robot.getTransportWaypoints();
        if (waypoints.isEmpty()) return;

        // 一整圈无搬运后的等待：每 2 秒重扫一次，期间原地不动
        if (recheckCooldown > 0) {
            recheckCooldown--;
            return;
        }

        if (currentTarget == null) {
            currentTarget = pickNextTarget(waypoints);
            if (currentTarget == null) {
                // 一整圈遍历完且无搬运 → 原地等待重扫
                robot.getNavigation().stop();
                recheckCooldown = RECHECK_TICKS;
                return;
            }
            repathCooldown = 0;
        }

        BlockPos pos = currentTarget.pos;
        // 有目标时主动看着当前要去的点位
        robot.getLookControl().setLookAt(
                pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                robot.getMaxHeadYRot(), robot.getMaxHeadXRot());

        if (!dwelling) {
            double dist = robot.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            // 到达判定：机械臂半径 + 半格补偿，至少 2 格，避免寻路停在容器旁仍判未到达而罚站
            double reach = Math.max(robot.getInteractionRadius() + 0.5, 2.0);
            if (dist <= reach * reach) {
                robot.smoothStop();
                dwelling = true;
                dwellTimer = currentTarget.isStart ? DWELL_START : computeEndDwell();
                boolean moved = doTransfer(currentTarget); // 到达即执行一次搬运
                if (moved) movedThisRound = true;
            } else {
                moveTo(pos);
            }
        } else {
            dwellTimer--;
            if (dwellTimer <= 0) {
                dwelling = false;
                currentTarget = null;
            }
        }
    }

    /** 终点停留时长 = (起点数 × DWELL_START) ÷ 终点数，向下取整，至少 1 tick */
    private int computeEndDwell() {
        int starts = robot.getTransportStarts().size();
        int ends = robot.getTransportEnds().size();
        if (ends <= 0) {
            return 1;
        }
        return Math.max(1, (starts * DWELL_START) / ends);
    }

    /**
     * 取下一个要去的点位：维护一个「就近贪心」生成的固定遍历顺序，循环走。
     * 每个点位每圈只出现一次，走完一整圈后从头开始（闭环），不会出现「当前点被重复取」的往返跑。
     */
    private TransportWaypoint pickNextTarget(List<TransportWaypoint> waypoints) {
        if (!sameOrder(traversalOrder, waypoints)) {
            traversalOrder.clear();
            traversalOrder.addAll(buildNearestNeighborOrder(waypoints));
            orderIndex = 0;
            loopJustCompleted = false;
            movedThisRound = false;
        }
        if (traversalOrder.isEmpty()) return null;

        // 上一圈刚走完：若这一圈没有实际搬运 → 返回 null 进入原地等待重扫；否则继续下一圈
        if (loopJustCompleted) {
            loopJustCompleted = false;
            if (!movedThisRound) {
                return null;
            }
            movedThisRound = false;
        }

        TransportWaypoint wp = traversalOrder.get(orderIndex);
        orderIndex = (orderIndex + 1) % traversalOrder.size();
        if (orderIndex == 0) {
            loopJustCompleted = true; // 走完一整圈
        }
        return wp;
    }

    /** 就近贪心：从机器人当前位置出发，每次选最近的未访问点位，生成固定遍历顺序 */
    private List<TransportWaypoint> buildNearestNeighborOrder(List<TransportWaypoint> waypoints) {
        List<TransportWaypoint> remaining = new ArrayList<>(waypoints);
        List<TransportWaypoint> order = new ArrayList<>();
        double cx = robot.getX();
        double cy = robot.getY();
        double cz = robot.getZ();
        while (!remaining.isEmpty()) {
            TransportWaypoint best = null;
            double bestDist = Double.MAX_VALUE;
            for (TransportWaypoint wp : remaining) {
                double dx = wp.pos.getX() + 0.5 - cx;
                double dy = wp.pos.getY() - cy;
                double dz = wp.pos.getZ() + 0.5 - cz;
                double d = dx * dx + dy * dy + dz * dz;
                if (d < bestDist) {
                    bestDist = d;
                    best = wp;
                }
            }
            order.add(best);
            remaining.remove(best);
            cx = best.pos.getX() + 0.5;
            cy = best.pos.getY();
            cz = best.pos.getZ() + 0.5;
        }
        return order;
    }

    /** 判断缓存的遍历顺序对应的点位集合是否与当前点位一致（增/删/改都会触发重建） */
    private boolean sameOrder(List<TransportWaypoint> order, List<TransportWaypoint> waypoints) {
        if (order.size() != waypoints.size()) return false;
        Set<String> a = new HashSet<>();
        Set<String> b = new HashSet<>();
        for (TransportWaypoint w : order) a.add(sig(w));
        for (TransportWaypoint w : waypoints) b.add(sig(w));
        return a.equals(b);
    }

    private String sig(TransportWaypoint w) {
        return w.pos.getX() + "," + w.pos.getY() + "," + w.pos.getZ() + ":" + w.isStart;
    }

    /** 在当前点位搬运：起点搬入背包，终点搬入容器。物品与流体都会搬。返回是否真的搬动了东西。 */
    private boolean doTransfer(TransportWaypoint wp) {
        if (wp.isStart) {
            boolean itemMoved = robot.transferFromContainerToInventory(wp.pos);
            boolean fluidMoved = robot.transferFluidFromContainerToTank(wp.pos);
            return itemMoved || fluidMoved;
        } else {
            boolean itemMoved = robot.transferFromInventoryToContainer(wp.pos);
            boolean fluidMoved = robot.transferFluidFromTankToContainer(wp.pos);
            return itemMoved || fluidMoved;
        }
    }

    private void moveTo(BlockPos pos) {
        // 装有传送装置：距离目标超过 10 格时直接瞬移（本 tick 由传送接管，不再寻路）；10 格内走常规寻路
        if (robot.teleportIfFar(pos)) return;
        if (--repathCooldown <= 0) {
            repathCooldown = 25;
            robot.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.0);
        }
    }
}
