package com.cheng.search_bot.block.custom;

/**
 * 创造充电站
 * - 与普通充电站共用 BlockEntity，行为完全一致
 * - 唯一区别：无需供电，机器人/物品放入即秒充满（由 ChargingStationBlockEntity.tick 依据方块状态判定）
 */
public class CreativeChargingStationBlock extends ChargingStationBlock {
    public CreativeChargingStationBlock(Properties properties) {
        super(properties);
    }
}
