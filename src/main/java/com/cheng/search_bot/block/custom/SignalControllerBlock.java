package com.cheng.search_bot.block.custom;

import com.cheng.search_bot.block.entity.ModBlockEntities;
import com.cheng.search_bot.block.entity.custom.SignalControllerBlockEntity;
import com.cheng.search_bot.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.level.storage.loot.LootParams;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * 信号控制器方块（配合红石卡）。
 *
 * <p>手持信号控制器 shift 右键机器人 → 把机器人（稳定 RobotId）记进物品 NBT；
 * 放下方块时把该 RobotId 写进方块实体。方块每 tick 把自身红石电平推送给绑定的机器人。
 *
 * <p>破坏掉落走 {@link #playerDestroy} 手动掉落，保留绑定信息（避免重放丢失绑定）。
 */
public class SignalControllerBlock extends BaseEntityBlock {
    public SignalControllerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SignalControllerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.SIGNAL_CONTROLLER.get(), SignalControllerBlockEntity::tick);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) return;
        if (level.getBlockEntity(pos) instanceof SignalControllerBlockEntity be) {
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.hasUUID("BoundRobot")) {
                be.setBoundRobot(tag.getUUID("BoundRobot"));
            }
        }
    }

    /** 创造模式中键选取：保留绑定信息。 */
    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        ItemStack stack = new ItemStack(ModItems.SIGNAL_CONTROLLER.get());
        if (level.getBlockEntity(pos) instanceof SignalControllerBlockEntity be && be.getBoundRobotUuid() != null) {
            stack.getOrCreateTag().putUUID("BoundRobot", be.getBoundRobotUuid());
        }
        return stack;
    }

    /** 掉落统一由 {@link #playerDestroy} 处理，这里返回空避免重复掉落。 */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return Collections.emptyList();
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity be, ItemStack tool) {
        if (!level.isClientSide && !player.getAbilities().instabuild) {
            ItemStack drop = new ItemStack(ModItems.SIGNAL_CONTROLLER.get());
            if (be instanceof SignalControllerBlockEntity sc && sc.getBoundRobotUuid() != null) {
                drop.getOrCreateTag().putUUID("BoundRobot", sc.getBoundRobotUuid());
            }
            net.minecraft.world.level.block.Block.popResource(level, pos, drop);
        }
        super.playerDestroy(level, player, pos, state, be, tool);
    }
}
