package com.cheng.search_bot.block.entity.custom;

import com.cheng.search_bot.item.ModItems;
import com.cheng.search_bot.item.custom.RobotCoreItem;
import com.cheng.search_bot.menu.custom.RobotAssemblerMenu;
import com.cheng.search_bot.util.RobotStats;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RobotAssemblerBlockEntity extends BlockEntity implements MenuProvider {
    // ====== 槽位布局（0.5.1）======
    // 0 机器人输入  1 机器人输出
    // 2 躯干(核心组件)  3 机器人核心(核心组件)
    // 4..13 功能组件槽（共 10 个，可用数量由躯干等级决定）
    // 14..19 附件槽（共 6 个，可用数量由躯干等级决定，每种只能装一个）
    public static final int SLOT_ROBOT_INPUT = 0;
    public static final int SLOT_ROBOT_OUTPUT = 1;
    public static final int SLOT_TORSO = 2;
    public static final int SLOT_CORE = 3;
    public static final int FUNCTIONAL_START = 4;
    public static final int FUNCTIONAL_SLOTS = 10;
    public static final int ATTACHMENT_START = 14;
    public static final int ATTACHMENT_SLOTS = 6;
    public static final int TOTAL_SLOTS = 20;

    private final ItemStackHandler itemHandler = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1; // 每个槽位只能放一个物品
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == SLOT_ROBOT_INPUT || slot == SLOT_ROBOT_OUTPUT) {
                return stack.is(ModItems.ROBOT_ITEM.get());
            }
            if (slot == SLOT_TORSO) {
                return ModItems.isPart(stack, "torso");
            }
            if (slot == SLOT_CORE) {
                return ModItems.isPart(stack, "robot_core");
            }
            if (slot >= FUNCTIONAL_START && slot < FUNCTIONAL_START + FUNCTIONAL_SLOTS) {
                if (!isFunctionalPart(stack)) return false;
                // 只有躯干等级开启的槽位才可放入
                if ((slot - FUNCTIONAL_START) >= getActiveFunctionalSlots()) return false;
                return true;
            }
            if (slot >= ATTACHMENT_START && slot < ATTACHMENT_START + ATTACHMENT_SLOTS) {
                if (!isAttachmentPart(stack)) return false;
                if ((slot - ATTACHMENT_START) >= getActiveAttachmentSlots()) return false;
                // 每种附件只能装一个
                return !hasAttachmentOfType(itemHandler, stack, slot);
            }
            return super.isItemValid(slot, stack);
        }
    };

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return 0;
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 0;
        }
    };

    public RobotAssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(com.cheng.search_bot.block.entity.ModBlockEntities.ROBOT_ASSEMBLER.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.search_bot.robot_assembler");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new RobotAssemblerMenu(id, inventory, this, this.data);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("inventory", itemHandler.serializeNBT());
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        // 不直接 deserializeNBT：它会读旧存档里的 "Size" 字段并把 itemHandler 缩回旧槽位数，
        // 导致新版本新增的槽位越界。这里只按 "Items" 列表逐槽回填，槽位总数始终固定为 TOTAL_SLOTS。
        CompoundTag inv = tag.getCompound("inventory");
        if (inv.contains("Items", Tag.TAG_LIST)) {
            ListTag items = inv.getList("Items", Tag.TAG_COMPOUND);
            for (int i = 0; i < items.size(); i++) {
                CompoundTag itemTag = items.getCompound(i);
                int slot = itemTag.getInt("Slot");
                if (slot >= 0 && slot < itemHandler.getSlots()) {
                    itemHandler.setStackInSlot(slot, ItemStack.of(itemTag));
                }
            }
        }
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    /** 把物品掉落到装配台上方（超出背包上限或槽位被占时用） */
    private void dropItem(ItemStack stack) {
        if (stack.isEmpty() || this.level == null) return;
        net.minecraft.world.entity.item.ItemEntity drop = new net.minecraft.world.entity.item.ItemEntity(
                this.level, this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 1.0,
                this.worldPosition.getZ() + 0.5, stack);
        this.level.addFreshEntity(drop);
    }

    // ====== 零件类型判断 ======

    private boolean isFunctionalPart(ItemStack stack) {
        return ModItems.isPart(stack, "mechanical_arm")
                || ModItems.isPart(stack, "sensor")
                || ModItems.isPart(stack, "engine")
                || ModItems.isPart(stack, "storage_box")
                || ModItems.isPart(stack, "battery");
    }

    private boolean isAttachmentPart(ItemStack stack) {
        return ModItems.isPart(stack, "wheels")
                || ModItems.isPart(stack, "propeller")
                || ModItems.isPart(stack, "teleporter")
                || ModItems.isPart(stack, "shield_part")
                || ModItems.isPart(stack, "wireless_charging");
    }

    private static String materialOf(ItemStack stack) {
        var key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return RobotStats.extractMaterial(key != null ? key.getPath() : "");
    }

    /** 当前躯干提供的可用功能组件槽数（无躯干为 0） */
    public int getActiveFunctionalSlots() {
        ItemStack torso = itemHandler.getStackInSlot(SLOT_TORSO);
        if (torso.isEmpty()) return 0;
        return RobotStats.getTorsoFunctionalSlots(materialOf(torso));
    }

    /** 当前躯干提供的可用附件槽数（无躯干为 0） */
    public int getActiveAttachmentSlots() {
        ItemStack torso = itemHandler.getStackInSlot(SLOT_TORSO);
        if (torso.isEmpty()) return 0;
        return RobotStats.getTorsoAttachmentSlots(materialOf(torso));
    }

    /** 附件槽中是否已存在同类型附件（用于“每种只能装一个”） */
    private boolean hasAttachmentOfType(ItemStackHandler handler, ItemStack stack, int currentSlot) {
        String type = ModItems.getPartType(stack);
        if (type == null) return false;
        for (int i = ATTACHMENT_START; i < ATTACHMENT_START + ATTACHMENT_SLOTS; i++) {
            if (i == currentSlot) continue;
            ItemStack other = handler.getStackInSlot(i);
            if (!other.isEmpty() && type.equals(ModItems.getPartType(other))) {
                return true;
            }
        }
        return false;
    }

    /** 在指定槽位区间内查找第一个属于某类型的零件槽索引，找不到返回 -1 */
    private int findPartSlot(int from, int to, String partName) {
        for (int i = from; i < to; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty() && ModItems.isPart(stack, partName)) {
                return i;
            }
        }
        return -1;
    }

    public boolean canAssemble() {
        if (!itemHandler.getStackInSlot(SLOT_ROBOT_OUTPUT).isEmpty()) return false;
        // 躯干和核心是核心组件，少一个直接无法组装
        return !itemHandler.getStackInSlot(SLOT_TORSO).isEmpty()
                && !itemHandler.getStackInSlot(SLOT_CORE).isEmpty();
    }

    public void assembleRobot() {
        if (!canAssemble()) return;

        ItemStack robot = new ItemStack(ModItems.ROBOT_ITEM.get());
        CompoundTag partsTag = new CompoundTag();

        // 机器人稳定标识 = 核心的 RobotId（首次组装时生成并写回核心 NBT）
        UUID robotId = null;
        ItemStack coreStack = itemHandler.getStackInSlot(SLOT_CORE);
        if (!coreStack.isEmpty()) {
            robotId = ModItems.getOrCreateCoreId(coreStack);
        }

        // 机器人编号 = 核心的 RobotNumber（核心合成时已分配；创造等无编号的核心这里懒分配）
        int robotNumber = 0;
        if (!coreStack.isEmpty()) {
            robotNumber = RobotCoreItem.getNumber(coreStack);
            if (robotNumber == 0 && this.level instanceof ServerLevel serverLevel) {
                robotNumber = RobotCoreItem.assignNumber(coreStack, serverLevel);
            }
        }

        // ====== 收集各零件并统计（功能组件可堆叠，传感器只取最高等级）======
        boolean hasArm = false;
        int collectAmount = 0;
        int collectEnergyCost = 0;
        float armLevelSum = 0.0f;
        double maxArmRadius = 0.0;

        boolean hasSensor = false;
        int sensorRadius = 0;
        String sensorMaterial = "copper"; // 插件卡槽数由最高等级传感器决定

        boolean hasEngine = false;
        float enginePowerSum = 0.0f;

        boolean hasStorage = false;
        int storageSlots = 0;
        List<Integer> boxSlots = new ArrayList<>();

        boolean hasBattery = false;
        int batteryCapacity = 0;
        int batteryEnergy = 0;

        double movementSpeedSum = 0.0;
        double movementEnergySum = 0.0;

        int shieldDamageReduction = 0;
        boolean hasWirelessCharging = false;

        for (int i = SLOT_TORSO; i < TOTAL_SLOTS; i++) {
            ItemStack part = itemHandler.getStackInSlot(i);
            if (part.isEmpty()) continue;

            CompoundTag partTag = new CompoundTag();
            part.save(partTag);

            // 保留之前拆解时的数据（机器人背包 / 电量）
            if (part.hasTag()) {
                CompoundTag existing = part.getTag();
                if (existing.contains("robot_inventory")) {
                    partTag.put("robot_inventory", existing.getCompound("robot_inventory"));
                }
                if (existing.contains("robot_energy")) {
                    partTag.putInt("robot_energy", existing.getInt("robot_energy"));
                }
            }
            partsTag.put("slot_" + i, partTag);

            String type = ModItems.getPartType(part);
            if (type == null) continue;
            String material = materialOf(part);

            switch (type) {
                case "mechanical_arm" -> {
                    hasArm = true;
                    collectAmount += RobotStats.getCollectAmount(material);
                    collectEnergyCost += RobotStats.getCollectEnergyCost(material);
                    armLevelSum += RobotStats.MATERIAL_LEVELS.getOrDefault(material, 3.0f);
                    maxArmRadius = Math.max(maxArmRadius, RobotStats.getArmRadius(material));
                }
                case "sensor" -> {
                    hasSensor = true;
                    // 传感器只依赖最高等级：装多没用；同时记录最高等级材料（决定插件卡槽数）
                    if (RobotStats.getSensorRadius(material) > sensorRadius) {
                        sensorRadius = RobotStats.getSensorRadius(material);
                        sensorMaterial = material;
                    }
                }
                case "engine" -> {
                    hasEngine = true;
                    enginePowerSum += RobotStats.MATERIAL_LEVELS.getOrDefault(material, 3.0f);
                }
                case "storage_box" -> {
                    hasStorage = true;
                    int slots = RobotStats.getStorageSlots(material);
                    storageSlots += slots;
                    boxSlots.add(slots);
                }
                case "battery" -> {
                    hasBattery = true;
                    batteryCapacity += RobotStats.getBatteryCapacity(material);
                    batteryEnergy += part.getOrCreateTag().getInt("energy");
                }
                case "shield_part" -> {
                    shieldDamageReduction += RobotStats.getShieldDamageReduction(material);
                }
                case "wheels", "propeller", "teleporter" -> {
                    movementSpeedSum += RobotStats.MOVEMENT_SPEED_BONUS.getOrDefault(type, 0.0);
                    movementEnergySum += RobotStats.MOVEMENT_ENERGY_COST.getOrDefault(type, 0.0);
                }
                case "wireless_charging" -> hasWirelessCharging = true;
                case "torso", "robot_core" -> { /* 核心组件，不参与 stats */ }
            }
        }

        robot.getOrCreateTag().put("parts", partsTag);
        if (robotId != null) {
            robot.getOrCreateTag().putUUID("RobotId", robotId);
        }
        if (robotNumber > 0) {
            robot.getOrCreateTag().putInt("RobotNumber", robotNumber);
        }

        // 核心被铁砧命名后，同步名称到机器人（物品显示名 + 实体 CustomName）
        if (!coreStack.isEmpty() && coreStack.hasCustomHoverName()) {
            Component coreName = coreStack.getHoverName();
            robot.setHoverName(coreName);
            robot.getOrCreateTag().putString("CustomName", Component.Serializer.toJson(coreName));
        }

        // 恢复核心上保存的收集/运输标记（「更换配件」拆解再组装后，机器人仍记得频道设置过的容器）
        if (!coreStack.isEmpty() && coreStack.hasTag()) {
            CompoundTag coreTag = coreStack.getTag();
            if (coreTag.contains("RobotMarkers")) {
                CompoundTag markers = coreTag.getCompound("RobotMarkers");
                if (markers.contains("CollectTarget")) {
                    robot.getOrCreateTag().putLong("CollectTarget", markers.getLong("CollectTarget"));
                }
                if (markers.contains("TransportWaypoints", Tag.TAG_LIST)) {
                    robot.getOrCreateTag().put("TransportWaypoints", markers.getList("TransportWaypoints", Tag.TAG_COMPOUND));
                }
            }
            // 恢复上次的模式（核心记录），让重新组装后的机器人模式与核心统一
            if (coreTag.contains("RobotMode")) {
                robot.getOrCreateTag().putInt("Mode", coreTag.getInt("RobotMode"));
            }
        }

        // ====== 查表计算 stats ======
        CompoundTag statsTag = new CompoundTag();

        // 躯干：血量 + 伤害减算（能量护盾在躯干血量基础上额外加成）
        String torsoMaterial = materialOf(itemHandler.getStackInSlot(SLOT_TORSO));
        statsTag.putDouble("max_health", RobotStats.getTorsoHealth(torsoMaterial));
        statsTag.putInt("damage_reduction", RobotStats.getTorsoDamageReduction(torsoMaterial) + shieldDamageReduction);
        // 下界合金及以上躯干：防火（不会被火/岩浆点燃与灼伤）
        statsTag.putBoolean("fireproof", torsoMaterial.equals("netherite") || torsoMaterial.equals("ultimate"));

        float enginePower = hasEngine ? enginePowerSum : 3.0f;

        if (hasArm) {
            statsTag.putInt("collect_amount", collectAmount);
            statsTag.putInt("collect_energy_cost", collectEnergyCost);
            statsTag.putInt("collect_cooldown", RobotStats.getCollectCooldown(armLevelSum, enginePower));
            statsTag.putDouble("interact_radius", maxArmRadius);
        }

        if (hasSensor) {
            statsTag.putInt("search_radius", sensorRadius);
            statsTag.putString("sensor_material", sensorMaterial);
            statsTag.putInt("card_slots", RobotStats.getSensorCardSlots(sensorMaterial));
        }

        // 储物：有储物箱用箱子格数（4×9..13×9）；没有储物箱时用机械臂「抓取数量」作为基础格数
        int inventorySize = hasStorage ? storageSlots : (hasArm ? collectAmount : 0);
        statsTag.putInt("inventory_size", inventorySize);
        if (hasStorage) {
            ListTag boxSlotsTag = new ListTag();
            for (int slots : boxSlots) {
                boxSlotsTag.add(IntTag.valueOf(slots));
            }
            statsTag.put("box_slots", boxSlotsTag);
        }

        if (hasBattery) {
            statsTag.putInt("max_energy", batteryCapacity);
        }

        statsTag.putBoolean("has_wireless_charging", hasWirelessCharging);
        statsTag.putDouble("movement_speed", RobotStats.getMovementSpeed(movementSpeedSum, enginePower));
        statsTag.putInt("move_energy_cost", RobotStats.getMoveEnergyCostPerTick(movementEnergySum, enginePower));

        robot.getOrCreateTag().put("stats", statsTag);

        // 初始电量 = 所有电池零件当前电量之和
        robot.getOrCreateTag().putInt("Energy", batteryEnergy);

        // 如果储物箱零件中存有之前拆解时的机器人背包物品，按箱子顺序合并还原到机器人 Inventory。
        // 容量用实际的 inventorySize（最大 13×9=117 或更多箱子累加），不能用写死的 90，否则大箱子会越界崩溃。
        ItemStackHandler mergedInv = new ItemStackHandler(Math.max(1, inventorySize));
        int mergedSlots = 0;
        boolean anyBoxData = false;
        for (int i = FUNCTIONAL_START; i < FUNCTIONAL_START + FUNCTIONAL_SLOTS; i++) {
            ItemStack part = itemHandler.getStackInSlot(i);
            if (part.isEmpty() || !"storage_box".equals(ModItems.getPartType(part))) continue;
            if (part.hasTag() && part.getTag().contains("robot_inventory")) {
                ItemStackHandler boxInv = new ItemStackHandler(90);
                boxInv.deserializeNBT(part.getTag().getCompound("robot_inventory"));
                int size = boxInv.getSlots();
                for (int s = 0; s < size && mergedSlots + s < inventorySize; s++) {
                    ItemStack st = boxInv.getStackInSlot(s);
                    if (!st.isEmpty()) mergedInv.setStackInSlot(mergedSlots + s, st);
                }
                mergedSlots += size;
                anyBoxData = true;
            }
        }
        if (anyBoxData) {
            robot.getOrCreateTag().put("Inventory", mergedInv.serializeNBT());
        }

        // 清空所有零件槽（含核心与躯干）
        for (int i = SLOT_TORSO; i < TOTAL_SLOTS; i++) {
            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
        }

        itemHandler.setStackInSlot(SLOT_ROBOT_OUTPUT, robot);
        setChanged();
    }

    public void disassembleRobot() {
        // 优先从输入槽拆解；若输入槽为空但输出槽有机器人，则从输出槽拆解
        int sourceSlot = SLOT_ROBOT_INPUT;
        ItemStack robot = itemHandler.getStackInSlot(sourceSlot);
        if (!robot.is(ModItems.ROBOT_ITEM.get())) {
            if (itemHandler.getStackInSlot(SLOT_ROBOT_OUTPUT).is(ModItems.ROBOT_ITEM.get())) {
                sourceSlot = SLOT_ROBOT_OUTPUT;
                robot = itemHandler.getStackInSlot(sourceSlot);
            } else {
                return;
            }
        }

        CompoundTag tag = robot.getTag();

        // 1. 还原零件到槽位
        if (tag != null && tag.contains("parts")) {
            CompoundTag partsTag = tag.getCompound("parts");
            for (int i = SLOT_TORSO; i < TOTAL_SLOTS; i++) {
                String key = "slot_" + i;
                if (partsTag.contains(key)) {
                    itemHandler.setStackInSlot(i, ItemStack.of(partsTag.getCompound(key)));
                }
            }
        }

        // 2. 机器人背包物品：写回储物箱
        if (tag != null && tag.contains("Inventory")) {
            ItemStackHandler invHandler = new ItemStackHandler(90);
            invHandler.deserializeNBT(tag.getCompound("Inventory"));

            // 机器人实际背包大小（无 inventory_size 时回退 9，与实体 getInventorySize 一致）
            int inventorySize = 9;
            if (tag.contains("stats")) {
                CompoundTag stats = tag.getCompound("stats");
                if (stats.contains("inventory_size")) inventorySize = stats.getInt("inventory_size");
            }
            // 兼容旧存档：旧版可能把 Inventory 的 Size 写小（如 90），扩容到至少 inventorySize 再逐箱写回
            if (invHandler.getSlots() < inventorySize) {
                invHandler.setSize(inventorySize);
            }

            // 每个储物箱的槽位数（旧存档无 box_slots 时退化为「只有一个箱子、大小=inventory_size」）
            int[] boxSizes;
            if (tag.contains("stats") && tag.getCompound("stats").contains("box_slots", Tag.TAG_LIST)) {
                ListTag list = tag.getCompound("stats").getList("box_slots", Tag.TAG_INT);
                boxSizes = new int[list.size()];
                for (int i = 0; i < boxSizes.length; i++) boxSizes[i] = list.getInt(i);
            } else {
                boxSizes = new int[]{inventorySize};
            }

            // 收集所有储物箱零件槽位（按功能槽顺序）
            List<Integer> storageSlots = new ArrayList<>();
            for (int i = FUNCTIONAL_START; i < FUNCTIONAL_START + FUNCTIONAL_SLOTS; i++) {
                ItemStack part = itemHandler.getStackInSlot(i);
                if (!part.isEmpty() && "storage_box".equals(ModItems.getPartType(part))) {
                    storageSlots.add(i);
                }
            }

            int offset = 0;
            if (!storageSlots.isEmpty()) {
                // 逐箱写回：第 i 个箱子对应区间 [offset, offset + boxSizes[i])
                for (int i = 0; i < storageSlots.size(); i++) {
                    int boxSize = i < boxSizes.length ? boxSizes[i] : 0;
                    ItemStackHandler boxHandler = new ItemStackHandler(Math.max(0, boxSize));
                    for (int s = 0; s < boxSize; s++) {
                        ItemStack st = invHandler.getStackInSlot(offset + s);
                        if (!st.isEmpty()) boxHandler.setStackInSlot(s, st);
                    }
                    ItemStack storagePart = itemHandler.getStackInSlot(storageSlots.get(i));
                    storagePart.getOrCreateTag().put("robot_inventory", boxHandler.serializeNBT());
                    offset += boxSize;
                }
            } else {
                // 没有储物箱零件：整个基础区掉落
                for (int i = 0; i < Math.max(0, inventorySize); i++) {
                    ItemStack s = invHandler.getStackInSlot(i);
                    if (!s.isEmpty()) dropItem(s.copy());
                }
            }
        }

        // 3. 如果机器人有电量，把电量写回第一个电池零件
        if (tag != null && tag.contains("Energy")) {
            int energy = tag.getInt("Energy");
            int batterySlot = findPartSlot(FUNCTIONAL_START, FUNCTIONAL_START + FUNCTIONAL_SLOTS, "battery");
            if (batterySlot >= 0) {
                ItemStack batteryPart = itemHandler.getStackInSlot(batterySlot);
                batteryPart.getOrCreateTag().putInt("energy", energy);
            }
        }

        // 保存收集/运输标记到核心，让「更换配件」重新组装后机器人仍记得这些标记
        if (tag != null) {
            ItemStack coreStack = itemHandler.getStackInSlot(SLOT_CORE);
            if (!coreStack.isEmpty()) {
                CompoundTag markers = new CompoundTag();
                if (tag.contains("CollectTarget")) {
                    markers.putLong("CollectTarget", tag.getLong("CollectTarget"));
                }
                if (tag.contains("TransportWaypoints", Tag.TAG_LIST)) {
                    markers.put("TransportWaypoints", tag.getList("TransportWaypoints", Tag.TAG_COMPOUND));
                }
                coreStack.getOrCreateTag().put("RobotMarkers", markers);
                // 记录当前模式，重新组装后沿用（核心是模式的「记忆」来源，与核心统一）
                if (tag.contains("Mode")) {
                    coreStack.getOrCreateTag().putInt("RobotMode", tag.getInt("Mode"));
                }
            }
        }

        itemHandler.setStackInSlot(sourceSlot, ItemStack.EMPTY);
        setChanged();
    }
}
