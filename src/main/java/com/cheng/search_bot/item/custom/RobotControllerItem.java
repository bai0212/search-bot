package com.cheng.search_bot.item.custom;

import com.cheng.search_bot.entity.custom.SearchRobotEntity;
import com.cheng.search_bot.item.ModItems;
import com.cheng.search_bot.menu.custom.RobotControllerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 机器人操控台（手持物件）
 *
 * <p>用「频道」组织机器人，一个频道可绑定多个机器人（以机器人稳定 {@code RobotId} 精确匹配），
 * 操控台通过切换频道来分别控制不同频道的机器人。shift 右键机器人时绑定到「当前频道」，
 * 而不是总绑定到 01 频道。
 *
 * <p>交互：
 * <ul>
 *   <li>手持操控台 shift 右键机器人 → 绑定到当前频道；再次 shift 右键同一机器人 → 解除绑定。</li>
 *   <li>shift + 滚轮 → 切换当前频道（上一个/下一个）。</li>
 *   <li>shift 右键空气 → 打开频道管理 GUI（切换频道 / 切模式 / 改名 / 添加删除频道 / 解绑）。</li>
 *   <li>右键空气 → 循环切换当前频道所有机器人的模式。</li>
 *   <li>左键容器 → 设置当前频道所有机器人的运输起点。</li>
 *   <li>右键容器 → 设置运输终点；shift 右键容器 → 设置/取消收集目标。</li>
 * </ul>
 */
public class RobotControllerItem extends Item {
    private static final double SEARCH_RANGE = 64.0;
    private static final String KEY_CHANNELS = "Channels";
    private static final String KEY_CURRENT = "CurrentChannel";

    public RobotControllerItem(Properties properties) {
        super(properties);
    }

    /**
     * 手持时快捷栏上方显示的名称：在「机器人操控台」后追加当前频道的模式，
     * 模式用专属颜色（待命白/跟随橙/探索青/收集绿/运输蓝），随切模式实时变化。
     */
    @Override
    public Component getName(ItemStack stack) {
        Component base = super.getName(stack);
        int count = getChannelCount(stack);
        if (count == 0) return base;
        SearchRobotEntity.RobotMode mode =
                SearchRobotEntity.RobotMode.byId(getChannelMode(stack, getCurrentChannelIndex(stack)));
        return base.copy().append(Component.literal(" · " + mode.cnName())
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(mode.color() & 0xFFFFFF))));
    }

    // ==================== 频道数据 ====================
    // 频道条目 CompoundTag{ Name:string, Mode:int, Robots:ListTag<CompoundTag{Id:UUID, Name:string}> }，
    // 存于 Channels(ListTag)。Robots 兼容旧版 RobotIds / 单 RobotId 数据自动迁移。

    /** 频道绑定的一个机器人：稳定 id + 显示名（供 GUI 直接显示，不必回服务端查实体）。 */
    public record RobotEntry(UUID id, String name) {}

    /** 确保操控台有频道结构；新操控台默认一个空频道「频道01」。返回频道 ListTag。 */
    public static ListTag getChannels(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(KEY_CHANNELS, Tag.TAG_LIST)) {
            ListTag list = new ListTag();
            CompoundTag ch = new CompoundTag();
            ch.putString("Name", "频道01");
            list.add(ch);
            tag.put(KEY_CHANNELS, list);
            tag.putInt(KEY_CURRENT, 0);
        }
        return tag.getList(KEY_CHANNELS, Tag.TAG_COMPOUND);
    }

    public static int getChannelCount(ItemStack stack) {
        return getChannels(stack).size();
    }

    public static int getCurrentChannelIndex(ItemStack stack) {
        int cur = stack.getOrCreateTag().getInt(KEY_CURRENT);
        int count = getChannelCount(stack);
        return Math.max(0, Math.min(cur, count - 1));
    }

    public static void setCurrentChannelIndex(ItemStack stack, int index) {
        int count = getChannelCount(stack);
        if (count == 0) return;
        stack.getOrCreateTag().putInt(KEY_CURRENT, Math.max(0, Math.min(index, count - 1)));
    }

    public static String getChannelName(ItemStack stack, int index) {
        ListTag channels = getChannels(stack);
        if (index < 0 || index >= channels.size()) return "频道" + String.format("%02d", index + 1);
        CompoundTag ch = channels.getCompound(index);
        return ch.contains("Name") ? ch.getString("Name") : "频道" + String.format("%02d", index + 1);
    }

    public static String getCurrentChannelName(ItemStack stack) {
        return getChannelName(stack, getCurrentChannelIndex(stack));
    }

    /** 读取频道绑定的机器人条目（id + 名字），兼容旧版 RobotIds / RobotId 自动迁移。 */
    public static List<RobotEntry> getChannelRobots(ItemStack stack, int index) {
        ListTag channels = getChannels(stack);
        List<RobotEntry> entries = new ArrayList<>();
        if (index < 0 || index >= channels.size()) return entries;
        CompoundTag ch = channels.getCompound(index);
        if (ch.contains("Robots", Tag.TAG_LIST)) {
            ListTag list = ch.getList("Robots", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag e = list.getCompound(i);
                if (e.hasUUID("Id")) entries.add(new RobotEntry(e.getUUID("Id"), e.getString("Name")));
            }
        } else if (ch.contains("RobotIds", Tag.TAG_LIST)) {
            // 旧版 RobotIds（只有 Id，无名字）迁移
            ListTag list = ch.getList("RobotIds", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag e = list.getCompound(i);
                if (e.hasUUID("Id")) entries.add(new RobotEntry(e.getUUID("Id"), ""));
            }
        } else if (ch.hasUUID("RobotId")) {
            entries.add(new RobotEntry(ch.getUUID("RobotId"), ""));
        }
        return entries;
    }

    /** 覆盖写某频道的机器人条目（统一写 Robots）。 */
    public static void setChannelRobots(ItemStack stack, int index, List<RobotEntry> entries) {
        ListTag channels = getChannels(stack);
        if (index < 0 || index >= channels.size()) return;
        CompoundTag ch = channels.getCompound(index);
        ch.remove("RobotId");
        ch.remove("RobotIds");
        ListTag list = new ListTag();
        for (RobotEntry e : entries) {
            CompoundTag t = new CompoundTag();
            t.putUUID("Id", e.id);
            t.putString("Name", e.name == null ? "" : e.name);
            list.add(t);
        }
        ch.put("Robots", list);
        stack.getOrCreateTag().put(KEY_CHANNELS, channels);
    }

    /** 读取频道绑定的机器人 id 列表（内部逻辑用，基于 RobotEntry 提取）。 */
    public static List<UUID> getChannelRobotIds(ItemStack stack, int index) {
        List<UUID> ids = new ArrayList<>();
        for (RobotEntry e : getChannelRobots(stack, index)) ids.add(e.id);
        return ids;
    }

    /** 频道当前模式 id（存于频道条目，供 GUI 显示；无则默认跟随 0）。 */
    public static int getChannelMode(ItemStack stack, int index) {
        ListTag channels = getChannels(stack);
        if (index < 0 || index >= channels.size()) return 0;
        CompoundTag ch = channels.getCompound(index);
        return ch.contains("Mode") ? ch.getInt("Mode") : 0;
    }

    public static void setChannelMode(ItemStack stack, int index, int mode) {
        ListTag channels = getChannels(stack);
        if (index < 0 || index >= channels.size()) return;
        channels.getCompound(index).putInt("Mode", mode);
        stack.getOrCreateTag().put(KEY_CHANNELS, channels);
    }

    public static List<UUID> getCurrentChannelRobotIds(ItemStack stack) {
        return getChannelRobotIds(stack, getCurrentChannelIndex(stack));
    }

    public static boolean isChannelBound(ItemStack stack, int index) {
        return !getChannelRobotIds(stack, index).isEmpty();
    }

    public static void setChannelName(ItemStack stack, int index, String name) {
        ListTag channels = getChannels(stack);
        if (index < 0 || index >= channels.size()) return;
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) return;
        if (trimmed.length() > 16) trimmed = trimmed.substring(0, 16);
        channels.getCompound(index).putString("Name", trimmed);
        stack.getOrCreateTag().put(KEY_CHANNELS, channels);
    }

    /** 追加一个空频道（按顺序命名「频道NN」） */
    public static void addChannel(ItemStack stack) {
        ListTag channels = getChannels(stack);
        CompoundTag ch = new CompoundTag();
        ch.putString("Name", "频道" + String.format("%02d", channels.size() + 1));
        channels.add(ch);
        stack.getOrCreateTag().put(KEY_CHANNELS, channels);
    }

    /** 保证至少有一个空频道（绑定后若没有空频道则追加一个备用） */
    private static void ensureSpareChannel(ItemStack stack) {
        ListTag channels = getChannels(stack);
        for (int i = 0; i < channels.size(); i++) {
            if (getChannelRobotIds(stack, i).isEmpty()) return;
        }
        addChannel(stack);
    }

    /** 从所有频道里移除某个机器人 id（一个机器人同一时间只属于一个频道）。 */
    private static void removeRobotFromAllChannels(ItemStack stack, UUID id) {
        ListTag channels = getChannels(stack);
        for (int i = 0; i < channels.size(); i++) {
            List<RobotEntry> entries = getChannelRobots(stack, i);
            if (entries.removeIf(e -> e.id.equals(id))) {
                setChannelRobots(stack, i, entries);
            }
        }
    }

    // ==================== 绑定 ====================

    /** shift 右键机器人：绑定到「当前频道」；当前频道已绑定该机器人则解除。返回给玩家的提示。 */
    public static String bindRobot(ItemStack stack, SearchRobotEntity robot) {
        UUID id = robot.getRobotId();
        int current = getCurrentChannelIndex(stack);
        List<RobotEntry> entries = getChannelRobots(stack, current);
        boolean alreadyInCurrent = entries.stream().anyMatch(e -> e.id.equals(id));

        if (alreadyInCurrent) {
            // 解除：从所有频道移除该机器人，保证一个机器人同一时间不属于任何频道
            removeRobotFromAllChannels(stack, id);
            return "已解除机器人绑定（" + getChannelName(stack, current) + "）";
        }

        // 绑定到当前频道：先从所有频道移除（保证一个机器人只属于一个频道），再加入当前频道并记录显示名
        removeRobotFromAllChannels(stack, id);
        entries.add(new RobotEntry(id, robot.getRobotDisplayName().getString()));
        setChannelRobots(stack, current, entries);
        setChannelMode(stack, current, robot.getMode().id);
        ensureSpareChannel(stack); // 保持一个备用空频道
        // 频道统一：新机器人立即继承同频道已有机器人的标记与模式
        syncNewRobotToChannel(robot.level(), stack, robot);
        return "已绑定机器人到「" + getChannelName(stack, current) + "」";
    }

    /** 让新绑定的机器人继承同频道已有机器人的标记与模式（频道内所有机器人保持同一模式/标记） */
    private static void syncNewRobotToChannel(Level level, ItemStack stack, SearchRobotEntity robot) {
        if (level.isClientSide) return;
        for (SearchRobotEntity peer : findCurrentRobots(level, stack, robot.blockPosition())) {
            if (peer == robot) continue;
            robot.copyMarkersFrom(peer);
            if (robot.getMode() != peer.getMode()) {
                robot.switchMode(peer.getMode());
            }
            return; // 对齐第一个已有机器人即可
        }
    }

    // ==================== 查找机器人 ====================

    /** 找到当前频道绑定的所有机器人实体（服务端） */
    public static List<SearchRobotEntity> findCurrentRobots(Level level, ItemStack stack, BlockPos center) {
        List<SearchRobotEntity> result = new ArrayList<>();
        if (level.isClientSide) return result;
        List<UUID> ids = getCurrentChannelRobotIds(stack);
        if (ids.isEmpty()) return result;
        Set<UUID> idSet = new HashSet<>(ids);
        AABB box = new AABB(center).inflate(SEARCH_RANGE);
        for (SearchRobotEntity e : level.getEntitiesOfClass(SearchRobotEntity.class, box)) {
            if (idSet.contains(e.getRobotId())) result.add(e);
        }
        return result;
    }

    /** 找到玩家手中（主手优先）的操控台；没有则返回 EMPTY。 */
    public static ItemStack findController(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.is(ModItems.CONTROLLER.get())) return main;
        ItemStack off = player.getOffhandItem();
        if (off.is(ModItems.CONTROLLER.get())) return off;
        return ItemStack.EMPTY;
    }

    // ==================== 标记同步 ====================

    /** 把机器人当前的运输/收集标记镜像写入操控台 NBT（覆盖语义，服务端调用） */
    public static void syncMarkers(ItemStack stack, SearchRobotEntity robot) {
        CompoundTag tag = stack.getOrCreateTag();

        ListTag starts = new ListTag();
        for (BlockPos p : robot.getTransportStarts()) {
            starts.add(LongTag.valueOf(p.asLong()));
        }
        if (!starts.isEmpty()) tag.put("MarkerStarts", starts); else tag.remove("MarkerStarts");

        ListTag ends = new ListTag();
        for (BlockPos p : robot.getTransportEnds()) {
            ends.add(LongTag.valueOf(p.asLong()));
        }
        if (!ends.isEmpty()) tag.put("MarkerEnds", ends); else tag.remove("MarkerEnds");

        BlockPos collect = robot.getCollectTargetPos();
        if (collect != null) tag.putLong("MarkerCollect", collect.asLong());
        else tag.remove("MarkerCollect");
    }

    public static void clearMarkers(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.remove("MarkerStarts");
        tag.remove("MarkerEnds");
        tag.remove("MarkerCollect");
    }

    /** 把当前频道机器人的标记写入操控台（切换频道/绑定后刷新客户端方框）；取第一个机器人即可（同频道标记一致）。 */
    public static void syncMarkersForCurrent(ItemStack stack, Level level, BlockPos center) {
        List<SearchRobotEntity> robots = findCurrentRobots(level, stack, center);
        if (!robots.isEmpty()) {
            syncMarkers(stack, robots.get(0));
        } else {
            clearMarkers(stack);
        }
    }

    // ==================== 操控动作（服务端，由网络包调用） ====================

    public static void switchChannel(ServerPlayer player, ItemStack stack, int delta) {
        int count = getChannelCount(stack);
        if (count <= 0) return;
        int cur = getCurrentChannelIndex(stack);
        int next = Math.floorMod(cur + delta, count);
        setCurrentChannelIndex(stack, next);
        syncMarkersForCurrent(stack, player.level(), player.blockPosition());
        notifyChannel(player, stack, next);
    }

    public static void setCurrentChannel(ServerPlayer player, ItemStack stack, int index) {
        if (index < 0 || index >= getChannelCount(stack)) return;
        setCurrentChannelIndex(stack, index);
        syncMarkersForCurrent(stack, player.level(), player.blockPosition());
        notifyChannel(player, stack, index);
    }

    public static void cycleCurrentMode(ServerPlayer player, ItemStack stack) {
        List<SearchRobotEntity> robots = findCurrentRobots(player.level(), stack, player.blockPosition());
        if (robots.isEmpty()) {
            player.sendSystemMessage(Component.literal("当前频道未绑定机器人，请先 shift 右键机器人绑定"));
            return;
        }
        // 频道统一：以第一个机器人切到的模式为准，其余机器人对齐到同一模式（避免各机器人因自身标记不同而切出不同模式）
        SearchRobotEntity first = robots.get(0);
        first.cycleMode();
        for (int i = 1; i < robots.size(); i++) {
            SearchRobotEntity robot = robots.get(i);
            if (robot.getMode() != first.getMode()) {
                robot.switchMode(first.getMode());
            }
        }
        // 把新模式写回操控台物品 NBT，手持名称（快捷栏上方）才能实时刷新
        setChannelMode(stack, getCurrentChannelIndex(stack), first.getMode().id);
    }

    public static void addChannel(ServerPlayer player, ItemStack stack) {
        addChannel(stack);
        player.sendSystemMessage(Component.literal("已添加频道「" + getChannelName(stack, getChannelCount(stack) - 1) + "」"));
    }

    public static void renameChannel(ServerPlayer player, ItemStack stack, int index, String name) {
        setChannelName(stack, index, name);
        player.sendSystemMessage(Component.literal("频道已改名为「" + getChannelName(stack, index) + "」"));
    }

    /** 删除当前频道（至少保留一个频道）；被删频道的机器人随频道数据消失自动解绑。 */
    public static void deleteChannel(ServerPlayer player, ItemStack stack) {
        ListTag channels = getChannels(stack);
        int count = channels.size();
        if (count <= 1) {
            player.sendSystemMessage(Component.literal("至少保留一个频道，无法删除"));
            return;
        }
        int current = getCurrentChannelIndex(stack);
        String removedName = getChannelName(stack, current);
        channels.remove(current);
        stack.getOrCreateTag().put(KEY_CHANNELS, channels);
        // 删除后把当前频道指到有效位置（若删的是最后一个，落到新末尾）
        setCurrentChannelIndex(stack, Math.min(current, channels.size() - 1));
        syncMarkersForCurrent(stack, player.level(), player.blockPosition());
        player.sendSystemMessage(Component.literal("已删除频道「" + removedName
                + "」，当前频道：「" + getCurrentChannelName(stack) + "」"));
    }

    /** 解除当前频道的全部机器人绑定（机器人实体本身保留，仅不再受该频道控制）。 */
    public static void unbindCurrentChannel(ServerPlayer player, ItemStack stack) {
        int current = getCurrentChannelIndex(stack);
        List<UUID> ids = getChannelRobotIds(stack, current);
        if (ids.isEmpty()) {
            player.sendSystemMessage(Component.literal("当前频道「" + getChannelName(stack, current) + "」没有绑定的机器人"));
            return;
        }
        setChannelRobots(stack, current, new ArrayList<>());
        clearMarkers(stack); // 解除后操控台不再显示该频道标记方框
        player.sendSystemMessage(Component.literal("已解除当前频道「" + getChannelName(stack, current)
                + "」的全部 " + ids.size() + " 个机器人绑定"));
    }

    /** 拖拽：把机器人移动到目标频道（若已在目标频道则忽略）。 */
    public static void moveRobot(ServerPlayer player, ItemStack stack, UUID robotId, int toChannel) {
        if (toChannel < 0 || toChannel >= getChannelCount(stack)) return;

        int from = -1;
        RobotEntry moved = null;
        int count = getChannelCount(stack);
        for (int i = 0; i < count; i++) {
            for (RobotEntry e : getChannelRobots(stack, i)) {
                if (e.id.equals(robotId)) { from = i; moved = e; break; }
            }
            if (moved != null) break;
        }
        if (moved == null || from == toChannel) return;

        // 数据层：从原频道移除，加入目标频道
        List<RobotEntry> fromEntries = getChannelRobots(stack, from);
        fromEntries.removeIf(e -> e.id.equals(robotId));
        setChannelRobots(stack, from, fromEntries);
        List<RobotEntry> toEntries = getChannelRobots(stack, toChannel);
        toEntries.add(moved);
        setChannelRobots(stack, toChannel, toEntries);

        // 模式对齐：目标频道已有机器人则对齐到它们；否则用被移机器人当前模式更新目标频道 Mode
        BlockPos center = player.blockPosition();
        SearchRobotEntity movedEntity = findRobotById(player.level(), robotId, center);
        List<SearchRobotEntity> targetRobots = findRobotsInChannel(player.level(), stack, toChannel, center);
        if (!targetRobots.isEmpty()) {
            int mode = targetRobots.get(0).getMode().id;
            if (movedEntity != null && movedEntity.getMode().id != mode) {
                movedEntity.switchMode(SearchRobotEntity.RobotMode.byId(mode));
            }
        } else if (movedEntity != null) {
            setChannelMode(stack, toChannel, movedEntity.getMode().id);
        }

        player.sendSystemMessage(Component.literal("已将「" + (moved.name.isEmpty() ? "机器人" : moved.name)
                + "」移动到「" + getChannelName(stack, toChannel) + "」"));
    }

    /** 拖到频道行外：解除该机器人绑定。 */
    public static void unbindRobot(ServerPlayer player, ItemStack stack, UUID robotId) {
        removeRobotFromAllChannels(stack, robotId);
        syncMarkersForCurrent(stack, player.level(), player.blockPosition());
        player.sendSystemMessage(Component.literal("已解除机器人绑定"));
    }

    /** 批量删除频道（GUI 关闭时执行），索引从大到小删除避免偏移。 */
    public static void deleteChannels(ServerPlayer player, ItemStack stack, List<Integer> indexes) {
        ListTag channels = getChannels(stack);
        if (channels.size() <= 1) return;
        List<Integer> sorted = new ArrayList<>(indexes);
        sorted.sort(java.util.Comparator.reverseOrder());
        int removed = 0;
        for (int idx : sorted) {
            if (idx >= 0 && idx < channels.size() && channels.size() > 1) {
                channels.remove(idx);
                removed++;
            }
        }
        stack.getOrCreateTag().put(KEY_CHANNELS, channels);
        setCurrentChannelIndex(stack, Math.min(getCurrentChannelIndex(stack), channels.size() - 1));
        syncMarkersForCurrent(stack, player.level(), player.blockPosition());
        if (removed > 0) {
            player.sendSystemMessage(Component.literal("已删除 " + removed + " 个频道"));
        }
    }

    /** 切指定频道的模式（GUI 里右键模式按钮），并让频道内所有机器人对齐。 */
    public static void cycleModeForChannel(ServerPlayer player, ItemStack stack, int channel) {
        if (channel < 0 || channel >= getChannelCount(stack)) return;
        List<SearchRobotEntity> robots = findRobotsInChannel(player.level(), stack, channel, player.blockPosition());
        if (robots.isEmpty()) {
            player.sendSystemMessage(Component.literal("该频道未绑定机器人，无法切换模式"));
            return;
        }
        SearchRobotEntity first = robots.get(0);
        first.cycleMode();
        for (int i = 1; i < robots.size(); i++) {
            SearchRobotEntity robot = robots.get(i);
            if (robot.getMode() != first.getMode()) robot.switchMode(first.getMode());
        }
        setChannelMode(stack, channel, first.getMode().id);
        // 不再弹聊天栏提示，模式直接显示在手持操控台名称处
    }

    /** 查找某频道绑定的机器人实体（服务端，按中心点搜索）。 */
    public static List<SearchRobotEntity> findRobotsInChannel(Level level, ItemStack stack, int channel, BlockPos center) {
        List<SearchRobotEntity> result = new ArrayList<>();
        if (level.isClientSide) return result;
        List<UUID> ids = getChannelRobotIds(stack, channel);
        if (ids.isEmpty()) return result;
        Set<UUID> idSet = new HashSet<>(ids);
        AABB box = new AABB(center).inflate(SEARCH_RANGE);
        for (SearchRobotEntity e : level.getEntitiesOfClass(SearchRobotEntity.class, box)) {
            if (idSet.contains(e.getRobotId())) result.add(e);
        }
        return result;
    }

    private static SearchRobotEntity findRobotById(Level level, UUID robotId, BlockPos center) {
        if (level.isClientSide) return null;
        AABB box = new AABB(center).inflate(SEARCH_RANGE);
        for (SearchRobotEntity e : level.getEntitiesOfClass(SearchRobotEntity.class, box)) {
            if (robotId.equals(e.getRobotId())) return e;
        }
        return null;
    }

    private static void notifyChannel(ServerPlayer player, ItemStack stack, int index) {
        String name = getChannelName(stack, index);
        int bound = getChannelRobotIds(stack, index).size();
        player.sendSystemMessage(Component.literal("当前频道：" + name
                + (bound > 0 ? "（已绑定 " + bound + " 个机器人）" : "（空）")));
    }

    // ==================== 客户端标记读取（供 ClientMarkerRenderer） ====================

    public static List<BlockPos> getMarkerStarts(ItemStack stack) {
        List<BlockPos> list = new ArrayList<>();
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("MarkerStarts", Tag.TAG_LIST)) {
            ListTag lt = tag.getList("MarkerStarts", Tag.TAG_LONG);
            for (int i = 0; i < lt.size(); i++) list.add(BlockPos.of(((LongTag) lt.get(i)).getAsLong()));
        }
        return list;
    }

    public static List<BlockPos> getMarkerEnds(ItemStack stack) {
        List<BlockPos> list = new ArrayList<>();
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("MarkerEnds", Tag.TAG_LIST)) {
            ListTag lt = tag.getList("MarkerEnds", Tag.TAG_LONG);
            for (int i = 0; i < lt.size(); i++) list.add(BlockPos.of(((LongTag) lt.get(i)).getAsLong()));
        }
        return list;
    }

    @Nullable
    public static BlockPos getMarkerCollect(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("MarkerCollect")) {
            return BlockPos.of(tag.getLong("MarkerCollect"));
        }
        return null;
    }

    // ==================== 交互 ====================

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        // shift 右键空气：打开频道管理 GUI
        if (player.isShiftKeyDown()) {
            if (player instanceof ServerPlayer sp) {
                NetworkHooks.openScreen(sp, new SimpleMenuProvider(
                        (id, inv, p) -> new RobotControllerMenu(id, inv),
                        Component.literal("机器人操控台")
                ));
            }
            return InteractionResultHolder.success(stack);
        }

        // 右键空气：循环切换当前频道所有机器人的模式
        if (player instanceof ServerPlayer sp) {
            cycleCurrentMode(sp, stack);
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        // 右键方块统一由 CommonEventHandler 处理（设收集容器/运输终点）
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        int count = getChannelCount(stack);
        int current = getCurrentChannelIndex(stack);
        if (count > 0) {
            int bound = getChannelRobotIds(stack, current).size();
            tooltip.add(Component.literal("当前频道：" + getChannelName(stack, current)
                    + (bound > 0 ? "（已绑定 " + bound + " 个机器人）" : "（空）")));
            tooltip.add(Component.literal("频道总数：" + count));
        } else {
            tooltip.add(Component.literal("未绑定（shift 右键机器人绑定）"));
        }

        List<BlockPos> starts = getMarkerStarts(stack);
        List<BlockPos> ends = getMarkerEnds(stack);
        BlockPos collect = getMarkerCollect(stack);
        if (!starts.isEmpty()) tooltip.add(Component.literal("运输起点：" + starts.size() + " 个"));
        if (!ends.isEmpty()) tooltip.add(Component.literal("运输终点：" + ends.size() + " 个"));
        if (collect != null) tooltip.add(Component.literal("收集目标：" + collect.toShortString()));
    }
}
