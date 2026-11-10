package com.cheng.search_bot.block.custom;

import com.cheng.search_bot.block.entity.custom.ChargingStationBlockEntity;
import com.cheng.search_bot.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.jetbrains.annotations.Nullable;

/**
 * 充电站方块
 * - 无 GUI
 * - 玩家手持物品形态机器人右键 → 放入充电站
 * - 玩家手持可充电物品右键 → 放入充电槽自动充电
 * - 玩家空手右键 → 取出充电槽中的物品
 * - 破坏时吐出内部机器人并掉落物品
 */
public class ChargingStationBlock extends BaseEntityBlock {
    /** 碰撞箱：贴合模型的 6 个立方体（底座 + 中层 + 三根立柱 + 前平台）。 */
    private static final VoxelShape SHAPE;
    static {
        VoxelShape s = Shapes.empty();
        s = Shapes.or(s, Shapes.box(2/16.0, 0, 2/16.0, 14/16.0, 1/16.0, 14/16.0));      // 底座
        s = Shapes.or(s, Shapes.box(4/16.0, 1/16.0, 3/16.0, 12/16.0, 2/16.0, 12/16.0));  // 中层
        s = Shapes.or(s, Shapes.box(6/16.0, 1/16.0, 2/16.0, 10/16.0, 8/16.0, 4/16.0));   // 中间立柱
        s = Shapes.or(s, Shapes.box(3/16.0, 1/16.0, 2/16.0, 5/16.0, 7/16.0, 4/16.0));    // 左立柱
        s = Shapes.or(s, Shapes.box(11/16.0, 1/16.0, 2/16.0, 13/16.0, 7/16.0, 4/16.0));  // 右立柱
        s = Shapes.or(s, Shapes.box(4/16.0, 0, 1/16.0, 12/16.0, 3/16.0, 4/16.0));        // 前平台
        SHAPE = s.optimize();
    }

    public ChargingStationBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, com.cheng.search_bot.block.entity.ModBlockEntities.CHARGING_STATION.get(),
                ChargingStationBlockEntity::tick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof ChargingStationBlockEntity station)) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);
        System.out.println("[ChargingStationBlock] use called, held=" + held + " hasRobot=" + station.hasRobot() + " hasItem=" + station.hasItemInChargeSlot());

        // 1. 手持物品形态机器人 → 放入充电站（机器人优先）
        if (held.is(ModItems.ROBOT_ITEM.get())) {
            System.out.println("[ChargingStationBlock] trying insert robot");
            if (station.insertRobotFromItem(held)) {
                held.shrink(1);
                return InteractionResult.CONSUME;
            }
            return InteractionResult.FAIL;
        }

        // 2. 手持可充电物品 → 放入充电槽
        if (!held.isEmpty() && held.getCapability(ForgeCapabilities.ENERGY).isPresent()) {
            System.out.println("[ChargingStationBlock] trying insert item to charge");
            if (station.insertItemToCharge(held)) {
                held.shrink(1);
                return InteractionResult.CONSUME;
            }
            // 充电槽已有物品或站内有机器人
            return InteractionResult.FAIL;
        }

        // 3. 空手 → 优先取出站内机器人（以物品形式给玩家），其次取出充电槽物品
        if (held.isEmpty()) {
            // 先取出站内机器人（物品形态，不生成实体）
            if (station.hasRobot()) {
                ItemStack robotItem = station.extractRobotAsItem();
                if (!robotItem.isEmpty()) {
                    if (!player.addItem(robotItem)) {
                        player.drop(robotItem, false);
                    }
                }
                return InteractionResult.CONSUME;
            }
            // 再尝试取出充电槽物品
            ItemStack charged = station.extractChargedItem();
            if (!charged.isEmpty()) {
                player.addItem(charged);
                return InteractionResult.CONSUME;
            }
        }

        return InteractionResult.PASS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChargingStationBlockEntity(pos, state);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ChargingStationBlockEntity station) {
                station.onStationDestroyed();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
