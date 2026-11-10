package com.cheng.search_bot.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 信号控制器物品（方块物品形态）。
 *
 * <p>手持它 shift 右键机器人 → 绑定（把机器人稳定 RobotId 写进物品 NBT {@code BoundRobot}）。
 * 放下后由方块实体读取绑定并把红石电平推送给机器人。
 */
public class SignalControllerItem extends BlockItem {
    public SignalControllerItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.hasUUID("BoundRobot")) {
            tooltip.add(Component.literal("已绑定机器人").withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.literal("未绑定（shift 右键机器人绑定）").withStyle(ChatFormatting.GRAY));
        }
    }
}
