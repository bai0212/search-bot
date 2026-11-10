package com.cheng.search_bot.item.custom;

import com.cheng.search_bot.util.RobotNameCounter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 机器人核心（组装机器人的必选核心组件）。
 *
 * <p>命名规则：
 * <ul>
 *   <li>核心合成出来时自动分配编号，物品名显示「机器人NN」；</li>
 *   <li>玩家在铁砧上给核心改名后，核心显示自定义名，组装出的机器人才会「显现」该名字（类似命名牌）。</li>
 * </ul>
 */
public class RobotCoreItem extends Item {
    public RobotCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        super.onCraftedBy(stack, level, player);
        // 仅在服务端合成时分配一次编号（写入核心 NBT）
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            assignNumber(stack, serverLevel);
        }
    }

    /** 物品显示名：有编号显示「机器人NN」，否则回退到默认名「机器人核心」。铁砧改名后由 hasCustomHoverName 优先覆盖。 */
    @Override
    public Component getName(ItemStack stack) {
        int number = getNumber(stack);
        if (number > 0) {
            return Component.literal(RobotNameCounter.format(number));
        }
        return super.getName(stack);
    }

    /** 给核心分配编号（若还没有）。返回编号。 */
    public static int assignNumber(ItemStack stack, ServerLevel level) {
        var tag = stack.getOrCreateTag();
        if (tag.contains("RobotNumber")) {
            return tag.getInt("RobotNumber");
        }
        int number = RobotNameCounter.get(level).allocate();
        tag.putInt("RobotNumber", number);
        return number;
    }

    /** 读取核心已分配的编号，未分配返回 0。 */
    public static int getNumber(ItemStack stack) {
        var tag = stack.getTag();
        return (tag != null && tag.contains("RobotNumber")) ? tag.getInt("RobotNumber") : 0;
    }
}
