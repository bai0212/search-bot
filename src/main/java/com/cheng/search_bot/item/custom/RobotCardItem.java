package com.cheng.search_bot.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 插件卡（精妙背包式升级，不装在躯干上，装在机器人的 GUI 里）。
 *
 * <p>每张卡的功能说明用悬浮提示（tooltip）展示，中文直接写死在构造参数里（与项目内
 * 其它中文提示一致）。插槽数量由传感器等级决定（见 {@code RobotStats#SENSOR_CARD_SLOTS}）。
 */
public class RobotCardItem extends Item {
    private final String description;

    public RobotCardItem(Properties properties, String description) {
        super(properties);
        this.description = description;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(description).withStyle(ChatFormatting.GRAY));
    }
}
