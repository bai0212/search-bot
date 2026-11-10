package com.cheng.search_bot.common;

import com.cheng.search_bot.SearchBotMod;
import com.cheng.search_bot.block.custom.ChargingStationBlock;
import com.cheng.search_bot.entity.ModEntities;
import com.cheng.search_bot.entity.custom.SearchRobotEntity;
import com.cheng.search_bot.item.ModItems;
import com.cheng.search_bot.item.custom.RobotControllerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * 通用事件处理器（客户端 + 服务端）
 * 处理机器人物品右键放置、操控台右键/左键容器等特殊逻辑。
 */
@Mod.EventBusSubscriber(modid = SearchBotMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommonEventHandler {

    /**
     * 右键方块：手持操控台设容器，或手持机器人放置。
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack held = player.getItemInHand(event.getHand());

        // 手持操控台右键容器：终点(橙)/收集(绿)/取消 状态机
        if (held.is(ModItems.CONTROLLER.get())) {
            handleControllerRightClick(player, held, event);
            return;
        }

        if (!held.is(ModItems.ROBOT_ITEM.get())) return;

        Level level = player.level();
        if (level.isClientSide) return;

        BlockPos clickedPos = event.getPos();
        System.out.println("[CommonEventHandler] robot right-click at " + clickedPos + " block=" + level.getBlockState(clickedPos).getBlock());

        // 如果点击的是充电站（含创造充电站），让充电站自己处理（支持手动放入机器人）
        if (level.getBlockState(clickedPos).getBlock() instanceof ChargingStationBlock) {
            System.out.println("[CommonEventHandler] clicked charging station, skipping");
            return;
        }

        // 手持机器人右键方块：优先触发方块 GUI/功能（容器、GUI 方块），
        // 只有普通方块（石头、泥土等）才放机器人。
        if (isInteractableBlock(level, clickedPos)) {
            System.out.println("[CommonEventHandler] clicked interactable block, let block handle");
            return;
        }

        BlockPos spawnPos = clickedPos.relative(event.getFace());

        SearchRobotEntity entity = ModEntities.SEARCH_ROBOT.get().create(level);
        if (entity != null) {
            entity.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0.0f, 0.0f);

            // 先加载物品 NBT（无 Energy 时默认满电、无 Mode 时默认跟随），
            // 再设主人，避免旧 Owner 标签覆盖当前放置者。
            CompoundTag tag = held.getTag();
            entity.loadRobotData(tag != null ? tag : new CompoundTag());
            entity.setOwner(player.getUUID());

            entity.determineModeAndRegisterGoals(spawnPos, true);
            level.addFreshEntity(entity);
            held.shrink(1);

            // 阻止原方块交互
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
        }
    }

    /**
     * 左键方块：手持操控台左键容器，切换运输起点（蓝色标记，再左键取消）。
     */
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        ItemStack held = player.getItemInHand(event.getHand());

        if (!held.is(ModItems.CONTROLLER.get())) return;

        Level level = player.level();
        BlockPos clickedPos = event.getPos();
        if (!isContainer(level, clickedPos)) return;

        if (!level.isClientSide) {
            List<SearchRobotEntity> robots = RobotControllerItem.findCurrentRobots(level, held, player.blockPosition());
            if (robots.isEmpty()) {
                player.sendSystemMessage(Component.literal("当前频道未绑定机器人，请先 shift 右键机器人绑定"));
            } else {
                boolean wasStart = robots.get(0).isTransportStart(clickedPos);
                for (SearchRobotEntity robot : robots) {
                    robot.toggleTransportStart(clickedPos);
                }
                RobotControllerItem.syncMarkers(held, robots.get(0));
                player.sendSystemMessage(Component.literal(wasStart
                        ? "已取消运输起点"
                        : "已设置运输起点（蓝色标记）"));
            }
        }
        event.setCanceled(true); // 阻止破坏容器
    }

    /**
     * 手持操控台右键容器：
     * <ul>
     *   <li>shift 右键 → 切换收集目标容器（绿色标记，再 shift 右键取消）；</li>
     *   <li>普通右键 → 切换运输终点（橙色标记，再右键取消）。</li>
     * </ul>
     */
    private static void handleControllerRightClick(Player player, ItemStack held, PlayerInteractEvent.RightClickBlock event) {
        Level level = player.level();
        BlockPos clickedPos = event.getPos();
        if (!isContainer(level, clickedPos)) {
            return; // 不是容器，交给物品默认交互
        }

        if (level.isClientSide) return;

        List<SearchRobotEntity> robots = RobotControllerItem.findCurrentRobots(level, held, player.blockPosition());
        if (robots.isEmpty()) {
            player.sendSystemMessage(Component.literal("当前频道未绑定机器人，请先 shift 右键机器人绑定"));
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
            return;
        }

        String msg;
        if (player.isShiftKeyDown()) {
            // shift 右键：切换收集目标
            msg = robots.get(0).toggleCollectTarget(clickedPos);
            for (int i = 1; i < robots.size(); i++) {
                robots.get(i).toggleCollectTarget(clickedPos);
            }
        } else {
            // 普通右键：切换运输终点
            msg = robots.get(0).handleControllerRightClick(clickedPos);
            for (int i = 1; i < robots.size(); i++) {
                robots.get(i).handleControllerRightClick(clickedPos);
            }
        }
        RobotControllerItem.syncMarkers(held, robots.get(0));
        player.sendSystemMessage(Component.literal(msg));
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.CONSUME);
    }

    /** 判断方块是否可交互（有容器能力或 GUI），供手持机器人右键时优先让方块处理 */
    private static boolean isInteractableBlock(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null) {
            if (be.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent()) return true;
            if (be instanceof MenuProvider) return true;
        }
        return state.getMenuProvider(level, pos) != null;
    }

    private static boolean isContainer(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return be != null && be.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent();
    }
}
