package com.cheng.search_bot.integration;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.IEnergyStorage;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AE2「便携存储元件」(Portable Cell) 反射适配层。
 *
 * <p>本模组没有 AE2 的编译期依赖，全部通过运行时反射读写元件内部的「类型 + 数量」压缩存储，
 * 以及通过 AE2 的 FORGE_ENERGY 能力给它充电。核心点：
 * <ul>
 *   <li>检测：向上遍历物品类的父类链，看是否继承 {@code appeng.items.tools.powered.AbstractPortableCell}。</li>
 *   <li>读：{@code StorageCells.getCellInventory(stack, null)} → {@code getAvailableStacks()} → 遍历 KeyCounter，
 *       把每个 (AEItemKey, 数量) 展开成 ≤64 一叠的 ItemStack。</li>
 *   <li>写：按槽位比较「打开前快照」和「关闭时内容」的差值，对差值调用 insert/extract + persist 写回元件 NBT。</li>
 *   <li>充电：{@code Capabilities.FORGE_ENERGY} 拿到 Forge 电量能力，机器人 FE 直接转进去。</li>
 * </ul>
 *
 * <p>反射一旦失败（AE2 版本对不上等）就静默降级为「读不到 / 写不进 / 不充电」，绝不抛异常崩服。
 * 注意：元件有「类型数 + 字节数」上限，塞满后再放进不下的物品不会被存进去（见 {@link #applyDelta}）。
 */
public final class Ae2PortableCell {

    /** 外部分页最多展示的格数（7 行 × 9 列，对应 1K 元件的类型容量）；超出的类型不显示但不会丢。 */
    public static final int MAX_SLOTS = 63;

    private static final String CLS_ABSTRACT_PORTABLE = "appeng.items.tools.powered.AbstractPortableCell";
    private static final String CLS_STORAGE_CELLS = "appeng.api.storage.StorageCells";
    private static final String CLS_SAVE_PROVIDER = "appeng.api.storage.cells.ISaveProvider";
    private static final String CLS_AE_KEY = "appeng.api.stacks.AEKey";
    private static final String CLS_AE_ITEM_KEY = "appeng.api.stacks.AEItemKey";
    private static final String CLS_ACTIONABLE = "appeng.api.config.Actionable";
    private static final String CLS_ACTION_SOURCE = "appeng.api.networking.security.IActionSource";
    private static final String CLS_CAPABILITIES = "appeng.capabilities.Capabilities";

    private Ae2PortableCell() {
    }

    /** 是否为 AE2 便携存储元件（按物品类继承链判断，AE2 不在场时永远返回 false）。 */
    public static boolean isPortableCell(ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (Class<?> c = stack.getItem().getClass(); c != null; c = c.getSuperclass()) {
            if (c.getName().equals(CLS_ABSTRACT_PORTABLE)) return true;
        }
        return false;
    }

    /** 读元件内容，展开成正好 MAX_SLOTS 个 ItemStack（每叠 ≤64，其余补空）。 */
    public static List<ItemStack> readItems(ItemStack stack) {
        List<ItemStack> out = new ArrayList<>(MAX_SLOTS);
        try {
            Object cell = getCellInventory(stack);
            if (cell != null) {
                Object counter = cell.getClass().getMethod("getAvailableStacks").invoke(cell);
                if (counter instanceof Iterable<?> it) {
                    for (Object entry : it) {
                        if (out.size() >= MAX_SLOTS) break;
                        Object key = entry.getClass().getMethod("getKey").invoke(entry);
                        long count = (Long) entry.getClass().getMethod("getLongValue").invoke(entry);
                        if (count <= 0) continue;
                        ItemStack sample = (ItemStack) key.getClass().getMethod("toStack", int.class).invoke(key, 1);
                        int max = Math.max(1, Math.min(64, sample.getMaxStackSize()));
                        long remain = count;
                        while (remain > 0 && out.size() < MAX_SLOTS) {
                            int take = (int) Math.min(remain, max);
                            ItemStack s = (ItemStack) key.getClass().getMethod("toStack", int.class).invoke(key, take);
                            if (s != null && !s.isEmpty()) out.add(s);
                            remain -= take;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
            // 读失败按空处理
        }
        while (out.size() < MAX_SLOTS) out.add(ItemStack.EMPTY);
        return out;
    }

    /**
     * 按槽位差值写回：before 是打开时的快照，after 是关闭时的内容，两者都应是 MAX_SLOTS 个。
     * 返回「元件放不进去（或被整体失败退回）」的物品，调用方负责把这些退还给玩家/掉落，**绝不吞物**。
     * 取出（负差值）永不失败；放入（正差值）先全部取出腾空间、再尝试放入，放不进的进返回值。
     */
    public static List<ItemStack> applyDelta(ItemStack stack, List<ItemStack> before, List<ItemStack> after) {
        // 1. 先算净差值（不需要元件本身），这样即使后面反射失败也能算清玩家到底放进了多少
        Map<Object, Long> delta = new HashMap<>();
        for (ItemStack s : before) {
            if (s == null || s.isEmpty()) continue;
            Object key = itemKeyOfQuiet(s);
            if (key != null) delta.merge(key, -(long) s.getCount(), Long::sum);
        }
        for (ItemStack s : after) {
            if (s == null || s.isEmpty()) continue;
            Object key = itemKeyOfQuiet(s);
            if (key != null) delta.merge(key, (long) s.getCount(), Long::sum);
        }

        List<ItemStack> leftover = new ArrayList<>();
        try {
            Object cell = getCellInventory(stack);
            if (cell == null) {
                // 读不到元件：玩家新增的全部退回
                for (Map.Entry<Object, Long> e : delta.entrySet()) {
                    if (e.getValue() > 0) leftover.addAll(stackify(e.getKey(), e.getValue()));
                }
                return leftover;
            }

            Class<?> aeKey = Class.forName(CLS_AE_KEY);
            Class<?> actionable = Class.forName(CLS_ACTIONABLE);
            Class<?> actionSource = Class.forName(CLS_ACTION_SOURCE);
            Object modulate = enumValue(actionable, "MODULATE");
            Object source = actionSource.getMethod("empty").invoke(null);

            Method insert = cell.getClass().getMethod("insert", aeKey, long.class, actionable, actionSource);
            Method extract = cell.getClass().getMethod("extract", aeKey, long.class, actionable, actionSource);

            // 2. 先全部取出（取出永不失败，还能给放入腾出空间）
            for (Map.Entry<Object, Long> e : delta.entrySet()) {
                if (e.getValue() < 0) extract.invoke(cell, e.getKey(), -e.getValue(), modulate, source);
            }
            // 3. 再尝试放入；insert 返回值 = 实际放入量，没放进去的收集起来退还
            for (Map.Entry<Object, Long> e : delta.entrySet()) {
                if (e.getValue() <= 0) continue;
                long inserted = (Long) insert.invoke(cell, e.getKey(), e.getValue(), modulate, source);
                long remain = e.getValue() - inserted;
                if (remain > 0) leftover.addAll(stackify(e.getKey(), remain));
            }
            cell.getClass().getMethod("persist").invoke(cell);
        } catch (Throwable ignored) {
            // 反射整体失败：元件原状未动，把玩家新增（净正值）全部退回，绝不吞
            leftover.clear();
            for (Map.Entry<Object, Long> e : delta.entrySet()) {
                if (e.getValue() > 0) leftover.addAll(stackify(e.getKey(), e.getValue()));
            }
        }
        return leftover;
    }

    /** 通过 AE2 的 FORGE_ENERGY 能力拿电量存储（FE 兼容），拿不到返回 null。 */
    public static IEnergyStorage getEnergyStorage(ItemStack stack) {
        try {
            Object capObj = Class.forName(CLS_CAPABILITIES).getField("FORGE_ENERGY").get(null);
            if (capObj instanceof Capability<?> cap) {
                @SuppressWarnings("unchecked")
                Capability<IEnergyStorage> energyCap = (Capability<IEnergyStorage>) cap;
                return stack.getCapability(energyCap).resolve().orElse(null);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Object getCellInventory(ItemStack stack) throws Exception {
        return Class.forName(CLS_STORAGE_CELLS)
                .getMethod("getCellInventory", ItemStack.class, Class.forName(CLS_SAVE_PROVIDER))
                .invoke(null, stack, null);
    }

    private static Object itemKeyOf(ItemStack stack) throws Exception {
        return Class.forName(CLS_AE_ITEM_KEY).getMethod("of", ItemStack.class).invoke(null, stack);
    }

    /** itemKeyOf 的静默版：拿不到 key 时返回 null，不抛异常。 */
    private static Object itemKeyOfQuiet(ItemStack stack) {
        try {
            return itemKeyOf(stack);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** 把 (AEItemKey, 数量) 拆成 ≤64 一叠的 ItemStack（用于把「放不进」的余额还给玩家）。 */
    private static List<ItemStack> stackify(Object key, long count) {
        List<ItemStack> out = new ArrayList<>();
        try {
            ItemStack sample = (ItemStack) key.getClass().getMethod("toStack", int.class).invoke(key, 1);
            int max = Math.max(1, Math.min(64, sample.getMaxStackSize()));
            long remain = count;
            while (remain > 0) {
                int take = (int) Math.min(remain, max);
                ItemStack s = (ItemStack) key.getClass().getMethod("toStack", int.class).invoke(key, take);
                if (s != null && !s.isEmpty()) out.add(s);
                remain -= take;
            }
        } catch (Throwable ignored) {
            // 拆不出（几乎不可能）：宁可少还，也不能让调用方崩
        }
        return out;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object enumValue(Class<?> type, String name) {
        return Enum.valueOf((Class) type, name);
    }
}
