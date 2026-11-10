package com.cheng.search_bot.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.TreeSet;

/**
 * 机器人编号分配器（世界级持久化，每个存档独立计数）。
 *
 * <p>规则：
 * <ul>
 *   <li>制作机器人核心时分配一个编号（1、2、3…），核心显示「机器人NN」；</li>
 *   <li>机器人实体死亡时释放编号，之后制作的核心优先复用空缺编号（先补空、再往后排）。</li>
 * </ul>
 */
public class RobotNameCounter extends SavedData {
    private static final String NAME = "search_bot_robot_names";

    private int nextNumber = 1;
    private final TreeSet<Integer> freed = new TreeSet<>();

    public static RobotNameCounter get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(RobotNameCounter::load, RobotNameCounter::new, NAME);
    }

    public static RobotNameCounter load(CompoundTag tag) {
        RobotNameCounter counter = new RobotNameCounter();
        counter.nextNumber = Math.max(1, tag.getInt("NextNumber"));
        ListTag freedTag = tag.getList("Freed", Tag.TAG_INT);
        for (int i = 0; i < freedTag.size(); i++) {
            counter.freed.add(freedTag.getInt(i));
        }
        return counter;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("NextNumber", nextNumber);
        ListTag freedTag = new ListTag();
        for (int n : freed) {
            freedTag.add(IntTag.valueOf(n));
        }
        tag.put("Freed", freedTag);
        return tag;
    }

    /** 分配一个编号：优先复用空缺，否则取下一个递增编号。 */
    public int allocate() {
        int number;
        if (!freed.isEmpty()) {
            number = freed.pollFirst();
        } else {
            number = nextNumber++;
        }
        setDirty();
        return number;
    }

    /** 释放编号，供后续核心复用（编号尚未分配出去时才记录）。 */
    public void release(int number) {
        if (number < 1) return;
        if (number < nextNumber && !freed.contains(number)) {
            freed.add(number);
            setDirty();
        }
    }

    /** 编号 → 显示名「机器人NN」。 */
    public static String format(int number) {
        return "机器人" + String.format("%02d", number);
    }
}
