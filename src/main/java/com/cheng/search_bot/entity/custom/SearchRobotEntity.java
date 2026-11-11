package com.cheng.search_bot.entity.custom;

import com.cheng.search_bot.block.ModBlocks;
import com.cheng.search_bot.block.entity.custom.ChargingStationBlockEntity;
import com.cheng.search_bot.item.ModItems;
import com.cheng.search_bot.menu.custom.RobotInventoryMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import com.cheng.search_bot.energy.CustomEnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 搜索机器人实体
 * 模式：
 * - FOLLOW（跟随模式）：生成在定位区域外，跟随玩家
 * - EXPLORATION（探索模式）：生成在定位区域内，自动收集物品
 */
public class SearchRobotEntity extends PathfinderMob {
    private static final Logger LOGGER = LogUtils.getLogger();

    // 同步数据
    private static final EntityDataAccessor<Integer> DATA_ENERGY = SynchedEntityData.defineId(SearchRobotEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ROBOT_MODE = SynchedEntityData.defineId(SearchRobotEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ROBOT_STATE = SynchedEntityData.defineId(SearchRobotEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> DATA_ROBOT_ID = SynchedEntityData.defineId(SearchRobotEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    public enum RobotState {
        WORKING(0),
        RETURNING_TO_CHARGE(1),
        CHARGING(2),
        OUT_OF_POWER(3);

        public final int id;
        RobotState(int id) { this.id = id; }
        public static RobotState byId(int id) {
            return values()[Math.max(0, Math.min(id, values().length - 1))];
        }
    }

    /** 工作模式（与 RobotState 运行时状态是两套概念） */
    public enum RobotMode {
        FOLLOW(0),
        EXPLORE(1),
        COLLECT(2),
        TRANSPORT(3),
        STANDBY(4);

        public final int id;
        RobotMode(int id) { this.id = id; }
        public static RobotMode byId(int id) {
            return values()[Math.max(0, Math.min(id, values().length - 1))];
        }

        public String cnName() {
            return switch (this) {
                case FOLLOW -> "跟随";
                case EXPLORE -> "探索";
                case COLLECT -> "收集";
                case TRANSPORT -> "运输";
                case STANDBY -> "待命";
            };
        }

        /** 模式专属颜色（ARGB）：待命白、跟随橙、探索青、收集绿、运输蓝。 */
        public int color() {
            return switch (this) {
                case FOLLOW -> 0xFFFF8800;   // 跟随 = 橙
                case EXPLORE -> 0xFF33CCCC;  // 探索 = 青
                case COLLECT -> 0xFF55CC55;  // 收集 = 绿
                case TRANSPORT -> 0xFF5599FF; // 运输 = 蓝
                case STANDBY -> 0xFFF2F2F2;  // 待命 = 白
            };
        }
    }

    // 主人与存储
    private UUID ownerUUID;
    /** 机器人稳定标识：跨充电站存取、物品↔实体转换都不变，供操控台多绑定精确匹配 */
    private UUID robotId;
    /** 机器人编号（由核心分配），用于显示「机器人NN」；死亡时释放编号供后续核心复用 */
    private int robotNumber = 0;
    private BlockPos homeStoragePos;
    private BlockPos targetChargingStation;

    /** 运输点位：一个容器位置 + 是否起点（起点=蓝色搬运源，终点=橙色搬运目标） */
    public static final class TransportWaypoint {
        public final BlockPos pos;
        public final boolean isStart;

        public TransportWaypoint(BlockPos pos, boolean isStart) {
            this.pos = pos;
            this.isStart = isStart;
        }
    }

    // 收集目标容器（单个，绿色标记）
    private BlockPos collectTargetPos;
    // 运输点位（有序，点击顺序；每个点位标记起点/终点）
    private final List<TransportWaypoint> transportWaypoints = new ArrayList<>();

    // 物品过滤（仓库右侧黑白名单）：0=无过滤，1=黑名单，2=白名单
    private int filterMode = 0;
    // 是否忽略物品后缀（NBT）：true 时匹配只按物品类型，不看附魔/耐久等 NBT
    private boolean ignoreFilterNbt = false;
    // 过滤物品列表（仓库右侧槽位，最多 12 个）
    private final ItemStackHandler filterItems = new ItemStackHandler(12);

    // 插件卡（精妙背包式升级，最多 6 个，实际可用由 statsTag 的 card_slots 决定）
    private final ItemStackHandler cardInventory = new ItemStackHandler(6);

    // 红石卡模式：0=无，1=高信号(有信号暂停)，2=低信号(无信号暂停)
    private int redstoneMode = 0;
    // 信号控制器当前是否通电（由信号控制器方块实时推送，仅服务端，不存档）
    private boolean redstonePowered = false;

    // 储罐卡：流体储罐（容量 8000mb = 8 桶）
    private FluidStack tankFluid = FluidStack.EMPTY;
    private static final int TANK_CAPACITY = 8000;

    // 零件信息（回收时需要保留）
    private CompoundTag partsTag = new CompoundTag();

    // 运行时属性（由组装时的零件等级决定）
    private CompoundTag statsTag = new CompoundTag();

    // 背包（最大 1170 格 = 10 个终极储物箱 × 117，实际可用大小由 statsTag 中的 inventory_size 决定）
    private static final int MAX_INVENTORY_SLOTS = 1170;
    private final ItemStackHandler inventory = new ItemStackHandler(MAX_INVENTORY_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            // 实体背包变化
        }

        @Override
        public int getSlotLimit(int slot) {
            // 堆叠卡：单格堆叠上限提升（取装入的最高一档堆叠卡）
            int limit = getStackingLimit();
            return limit > 64 ? limit : super.getSlotLimit(slot);
        }

        @Override
        protected int getStackLimit(int slot, ItemStack stack) {
            // 堆叠卡只对「可堆叠」物品生效（工具/装备等 maxStackSize=1 的保持 1）。
            // 带 NBT 的可堆叠物品同样生效，NBT 是否一致由 ItemHandlerHelper.canItemStacksStack 把关。
            int limit = getStackingLimit();
            if (limit > 64 && stack.isStackable()) {
                return limit;
            }
            return super.getStackLimit(slot, stack);
        }
    };

    // 能量（容量由 statsTag 中的 max_energy 决定，默认 10000 作为回退）
    private CustomEnergyStorage energyStorage = new CustomEnergyStorage(10000, 500, 500) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (!simulate) syncEnergy();
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = super.extractEnergy(maxExtract, simulate);
            if (!simulate) syncEnergy();
            return extracted;
        }
    };

    // 收集物品独立模块的冷却计数器
    private int collectCooldownTick = 0;

    // 磁力卡扫描冷却：每 5 tick（0.25 秒）扫一次，避免每 tick 都做实体范围查询
    private int magnetCooldownTick = 0;

    // 传送装置瞬移冷却
    private int teleportCooldown = 0;

    // 飞行相关：两种导航器和移动控制器，根据是否有螺旋桨切换
    private PathNavigation groundNavigation;
    private PathNavigation flyingNavigation;
    private MoveControl groundMoveControl;
    private MoveControl flyingMoveControl;
    private boolean canFly = false;
    private boolean flyingActive = false;                 // 当前是否真的在飞（有螺旋桨 + 需要往上）
    private static final double FLY_START_Y = 1.0;        // 目标比脚底高超过 1 格才起飞
    private static final int WIRELESS_CHARGE_RATE = 500;  // 无线充电速率 FE/tick

    public SearchRobotEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        // 初始能量由组装时的电池决定；构造器中不硬编码，避免覆盖 NBT 加载值
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        this.groundNavigation = new GroundPathNavigation(this, level);
        this.flyingNavigation = new FlyingPathNavigation(this, level);
        this.groundMoveControl = new MoveControl(this);
        this.flyingMoveControl = new FlyingMoveControl(this, 20, true);
        this.moveControl = this.groundMoveControl;
        return this.groundNavigation;
    }

    /** 根据零件更新飞行能力（只改能力，不直接切导航器/控制器） */
    private void updateCanFly() {
        boolean newCanFly = hasPropeller();
        if (newCanFly != this.canFly) {
            this.canFly = newCanFly;
            if (!this.canFly) {
                this.flyingActive = false;
                applyMovementMode();
            }
        }
    }

    /** 根据当前是否真正飞行，切换导航器与移动控制器 */
    private void applyMovementMode() {
        this.navigation = this.flyingActive ? this.flyingNavigation : this.groundNavigation;
        this.moveControl = this.flyingActive ? this.flyingMoveControl : this.groundMoveControl;
        this.setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.WATER, this.flyingActive ? 0.0f : -1.0f);
        this.setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.LAVA, this.flyingActive ? 0.0f : -1.0f);
    }

    /**
     * 每 tick 决定是否真正飞行（节能）：有螺旋桨能力时默认地面步行，
     * 只有当目标在头顶上方超过 {@link #FLY_START_Y} 格时才起飞；
     * 用「滞回」避免刚起飞又落地、上下来回抖。
     */
    private void updateFlyingActive() {
        if (!this.canFly) return;
        boolean followMode = getMode() == RobotMode.FOLLOW || getMode() == RobotMode.EXPLORE;
        Player owner = followMode ? getOwner() : null;
        boolean shouldFly;
        BlockPos navTarget = this.getNavigation().getTargetPos(); // 切换前先记住当前导航目标

        if (owner != null) {
            double dy = owner.getY() - this.getY(); // 主人比我高多少（正=在上方）
            if (dy > FLY_START_Y) {
                shouldFly = true;                       // 主人在上方较多 → 飞
            } else if (owner.onGround() && dy < 0.5) {
                shouldFly = false;                      // 主人已落地且我不明显高于他 → 步行
            } else {
                shouldFly = this.flyingActive;          // 过渡带保持，防振荡
            }
        } else {
            // 收集/运输：目标在上方才飞；目标在附近保持当前状态（飞则悬停、步行则继续走）
            double targetY = navTarget != null ? navTarget.getY() + 0.5 : this.getY();
            double dy = targetY - this.getY();
            if (dy > FLY_START_Y) {
                shouldFly = true;
            } else if (dy < -1.0) {
                shouldFly = false;                      // 目标明显在下方 → 落地步行
            } else {
                shouldFly = this.flyingActive;          // 目标在 ±1 格内：保持当前，避免到达后失重坠落
            }
        }

        if (shouldFly != this.flyingActive) {
            this.flyingActive = shouldFly;
            applyMovementMode();
            // 切换导航器后新导航器还没有目标；把刚才的目标重新交给它，否则会「丢目标→切回地面→再切飞行」来回抖、永远飞不上去。
            // 跟随/探索的飞行由 FollowOwnerGoal 用 moveControl 直接控制，这里不重设导航。
            if (owner == null && navTarget != null) {
                this.getNavigation().moveTo(navTarget.getX() + 0.5, navTarget.getY(), navTarget.getZ() + 0.5, 1.0);
            }
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FLYING_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    /** 右键交互严格按实体边界判定，避免点附近误触发 */
    @Override
    public float getPickRadius() {
        return 0.0f;
    }

    /** 检查零件中是否包含车轮 */
    public boolean hasWheels() {
        for (String key : partsTag.getAllKeys()) {
            ItemStack stack = ItemStack.of(partsTag.getCompound(key));
            if (stack.is(ModItems.WHEELS.get())) {
                return true;
            }
        }
        return false;
    }

    /** 检查零件中是否包含传送装置 */
    public boolean hasTeleporter() {
        for (String key : partsTag.getAllKeys()) {
            ItemStack stack = ItemStack.of(partsTag.getCompound(key));
            if (stack.is(ModItems.TELEPORTER.get())) {
                return true;
            }
        }
        return false;
    }

    /** 检查零件中是否包含螺旋桨（飞行部件） */
    private boolean hasPropeller() {
        for (String key : partsTag.getAllKeys()) {
            ItemStack stack = ItemStack.of(partsTag.getCompound(key));
            if (stack.is(ModItems.PROPELLER.get())) {
                return true;
            }
        }
        return false;
    }

    /** 检查零件中是否包含无线充电装置 */
    public boolean hasWirelessCharging() {
        for (String key : partsTag.getAllKeys()) {
            ItemStack stack = ItemStack.of(partsTag.getCompound(key));
            if (stack.is(ModItems.WIRELESS_CHARGING.get())) {
                return true;
            }
        }
        return false;
    }

    /** 仅在真正飞行时消除重力（有螺旋桨且需要往上时）；地面步行时恢复正常重力 */
    @Override
    public boolean isNoGravity() {
        // 只有「正在飞」才无视重力；地面步行时恢复重力，否则会永远悬停在空中、切模式也不落地
        return this.flyingActive;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_ENERGY, 0);
        this.entityData.define(DATA_ROBOT_MODE, RobotMode.FOLLOW.id);
        this.entityData.define(DATA_ROBOT_STATE, RobotState.WORKING.id);
        this.entityData.define(DATA_ROBOT_ID, Optional.empty());
    }

    /**
     * 注册 AI 目标（模式已在 loadRobotData 中从 NBT 读取，默认 FOLLOW）。
     * 放置时调用一次，区块重载后也调用一次。
     */
    public void determineModeAndRegisterGoals(BlockPos spawnPos, boolean detectMode) {
        if (this.level().isClientSide) return;

        // 注册 AI（先清空再注册，避免重复）
        clearAllGoals();
        this.registerGoalsByMode();

        LOGGER.info("[SearchRobot] 模式={}, 已注册AI目标数={}",
                getMode(), this.goalSelector.getAvailableGoals().size());
    }

    private void registerGoalsByMode() {
        this.goalSelector.addGoal(0, new FloatGoal(this));

        // 低电返航充电：所有模式通用
        this.goalSelector.addGoal(1, new com.cheng.search_bot.entity.ai.ReturnToChargeGoal(this));

        RobotMode mode = getMode();
        switch (mode) {
            case FOLLOW -> {
                // 跟随：跟玩家为主，收集由 tick 独立模块路过执行
                this.goalSelector.addGoal(2, new com.cheng.search_bot.entity.ai.FollowOwnerGoal(this, 1.0, 5.5f, 1.5f));
                this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8));
            }
            case EXPLORE -> {
                // 探索：主动收集玩家附近容器为主（不会跑远），无容器时才跟随玩家。
                this.goalSelector.addGoal(2, new com.cheng.search_bot.entity.ai.CollectItemsGoal(this));
                this.goalSelector.addGoal(3, new com.cheng.search_bot.entity.ai.FollowOwnerGoal(this, 1.0, 5.5f, 1.5f));
                this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8));
            }
            case COLLECT -> {
                // 收集：目标容器半径10格内搬运（专门的 CollectGoal）
                this.goalSelector.addGoal(2, new com.cheng.search_bot.entity.ai.CollectGoal(this));
            }
            case TRANSPORT -> {
                // 运输：起点→终点搬运（专门的 TransportGoal）
                this.goalSelector.addGoal(2, new com.cheng.search_bot.entity.ai.TransportGoal(this));
            }
            case STANDBY -> {
                // 待命：原地不动，不注册任何移动目标（仅保留返航充电）
            }
        }

        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6.0f));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    protected void registerGoals() {
        // 初始不注册，等 determineModeAndRegisterGoals 在放置时调用
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    /**
     * 头部最大上下转动角（俯仰）：上下 ±30°，像玩家一样只有头部能低头抬头。
     */
    @Override
    public int getMaxHeadXRot() {
        return 30;
    }

    /**
     * 头部最大左右转动角（偏航）：左右 ±45°。
     */
    @Override
    public int getMaxHeadYRot() {
        return 45;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            if (teleportCooldown > 0) teleportCooldown--;

            // 按需飞行：目标在头顶上方才起飞，否则地面步行节能
            updateFlyingActive();

            // 每 tick 更新一次状态，避免 10 tick 内电量从 >20% 跌到 0 错过返航
            updateRobotState();

            // 无线充电：装有无线充电装置时，附近存在充电站则由最近的充电站持续供电
            if (hasWirelessCharging() && energyStorage.getEnergyStored() < energyStorage.getMaxEnergyStored()) {
                tickWirelessCharging();
            }

            // 移动消耗能量（按 stats 中的 move_energy_cost）
            if (this.getDeltaMovement().lengthSqr() > 0.001) {
                int moveCost = getMoveEnergyCost();
                energyStorage.extractEnergy(moveCost, false);
            }

            // 没电时减速，有电时恢复速度（0 电速度更慢，接近缓慢爬行）
            if (energyStorage.getEnergyStored() <= 0) {
                this.setSpeed(0.02f);
            } else {
                // 飞行与步行统一使用移动速度
                this.setSpeed((float) this.getAttributeValue(Attributes.MOVEMENT_SPEED));
            }

            // ====== 独立收集物品模块（每 tick 运行，不与移动冲突）======
            tickCollectItems();

            // 磁力卡：吸取周围掉落物进背包
            tickMagnetPickup();
        }
    }

    /** 磁力卡：把机器人周围掉在地上的物品吸进背包（尊重过滤/销毁卡）。 */
    private void tickMagnetPickup() {
        if (!hasCard(ModItems.CARD_MAGNET)) return;
        // 扫描冷却：每 5 tick 扫一次，避免每 tick 都做实体范围查询（性能优化，不影响手感）
        if (magnetCooldownTick > 0) {
            magnetCooldownTick--;
            return;
        }
        magnetCooldownTick = 4;
        if (isInventoryFull()) return;
        // 磁力卡范围固定半径 6 格（不随传感器等级变化）
        int radius = 6;
        AABB box = this.getBoundingBox().inflate(radius);
        int invSize = getInventorySize();

        for (ItemEntity itemEntity : this.level().getEntitiesOfClass(ItemEntity.class, box)) {
            if (!itemEntity.isAlive()) continue;
            ItemStack stack = itemEntity.getItem();
            if (stack.isEmpty()) continue;

            if (!canPickupItem(stack)) {
                // 销毁卡：被过滤掉的掉落物直接销毁
                if (shouldVoid(stack)) itemEntity.discard();
                continue;
            }

            ItemStack remainder = stack.copy();
            for (int j = 0; j < invSize; j++) {
                remainder = inventory.insertItem(j, remainder, false);
                if (remainder.isEmpty()) break;
            }
            int moved = stack.getCount() - remainder.getCount();
            if (moved > 0) {
                stack.shrink(moved);
                if (stack.isEmpty()) itemEntity.discard();
                if (isInventoryFull()) return;
            }
        }
    }

    /**
     * 覆写移动物理：飞行模式（有螺旋桨时）使用类似 FlyingMob 的空中移动，不受重力影响；
     * 步行模式走原版逻辑。
     */
    @Override
    public void travel(Vec3 travelVector) {
        if (this.flyingActive) {
            // === 飞行模式 ===
            if (this.isInWater()) {
                this.moveRelative(0.02F, travelVector);
                this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().scale(0.8F));
            } else if (this.isInLava()) {
                this.moveRelative(0.02F, travelVector);
                this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().scale(0.5));
            } else {
                // 空中直线飞行：绕过 FlyingMoveControl 的 bang-bang 控制（它只按符号给 yya、
                // 且无水平分量，会上下振荡/乱飘），直接朝移动控制器目标点计算带阻尼的 3D 速度向量。
                // 匀速直线 + 越近越慢，稳定不抖、到位即停。
                double wantedX = this.moveControl.getWantedX();
                double wantedY = this.moveControl.getWantedY();
                double wantedZ = this.moveControl.getWantedZ();
                double dx = wantedX - this.getX();
                double dy = wantedY - this.getY();
                double dz = wantedZ - this.getZ();
                double distSqr = dx * dx + dy * dy + dz * dz;
                double speedModifier = this.moveControl.getSpeedModifier();

                // speedModifier>0 表示仍有目标在驱动（follow 目标点或飞行导航节点）；
                // 到达后 brake()/stop()/clearAllGoals() 都会把它归零，据此停下、不再漂移。
                if (speedModifier > 0.0 && distSqr > 1.0E-4) {
                    double dist = Math.sqrt(distSqr);
                    // 飞行速度与陆地步行统一：speedModifier × MOVEMENT_SPEED（同一个速度算子）
                    double maxSpeed = this.getAttributeValue(Attributes.MOVEMENT_SPEED);
                    double targetSpeed = Math.min(speedModifier * maxSpeed, dist * 0.5);
                    double inv = 1.0 / dist;
                    this.setDeltaMovement(dx * inv * targetSpeed, dy * inv * targetSpeed, dz * inv * targetSpeed);
                } else {
                    // 指数衰减减速：目标已到（speedModifier=0）时自然滑停，而非瞬间归零
                    Vec3 decayed = this.getDeltaMovement().scale(0.6);
                    if (decayed.lengthSqr() < 1.0E-4) decayed = Vec3.ZERO;
                    this.setDeltaMovement(decayed);
                }
                this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());
            }
            this.calculateEntityAnimation(false);
        } else {
            // === 步行模式 ===
            super.travel(travelVector);
        }
    }

    /**
     * 独立收集物品模块：直接嵌入 tick()，不依赖 GoalSelector。
     * 只要未停靠充电、有电、背包未满，且交互半径内有容器，就直接取物。
     * 即使机器人因路径受阻无法移动（RETURNING_TO_CHARGE/OUT_OF_POWER），
     * 只要还有电量就可以继续收集。仅 CHARGING（停靠充电站）时暂停收集。
     */
    private void tickCollectItems() {
        if (collectCooldownTick > 0) {
            collectCooldownTick--;
            return;
        }
        // 红石卡：通电/断电触发暂停时不工作
        if (isRedstonePaused()) return;
        // 仅探索模式「路过收集」；跟随模式只跟随不碰容器，收集/运输模式背包专用于搬运
        if (getMode() != RobotMode.EXPLORE) return;
        // 只有停靠充电时才暂停收集；低电返航/没电但还有残电时继续收集
        if (getRobotState() == RobotState.CHARGING) return;
        if (getEnergy() <= 0) return;
        if (isInventoryFull()) return;

        BlockPos container = findContainerInInteractionRadius();
        if (container != null) {
            collectFromContainer(container);
            collectCooldownTick = getCollectCooldown();
        }
    }

    /**
     * 在交互半径内查找最近的容器。
     */
    /**
     * 在交互半径内查找最近的容器（纯范围扫描，无视线要求）。
     * 搜索范围：XZ = 交互半径，Y = ±3 格（覆盖地面箱子和高处架子）。
     */
    private BlockPos findContainerInInteractionRadius() {
        BlockPos robotPos = this.blockPosition();
        int radius = (int) Math.ceil(getInteractionRadius());
        BlockPos closest = null;
        double closestDist = Double.MAX_VALUE;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = robotPos.offset(x, y, z);
                    // 只认「有可拿物品」的容器，避免机器人反复在已空/被过滤的容器旁尝试
                    if (containerHasCollectibleItems(pos)) {
                        double dist = robotPos.distSqr(pos);
                        if (dist < closestDist) {
                            closestDist = dist;
                            closest = pos;
                        }
                    }
                }
            }
        }
        return closest;
    }

    /**
     * 从指定容器取物（供 tick 路过收集与 CollectItemsGoal 主动收集共用）。
     */
    public void collectFromContainer(BlockPos pos) {
        var be = this.level().getBlockEntity(pos);
        if (be == null) return;

        int collectAmount = getCollectAmount();
        int energyCost = getCollectEnergyCost();
        int invSize = getInventorySize();

        var cap = be.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER);
        cap.ifPresent(handler -> {
            boolean collected = false;
            for (int i = 0; i < handler.getSlots(); i++) {
                if (isInventoryFull()) break;
                net.minecraft.world.item.ItemStack stack = handler.extractItem(i, collectAmount, true);
                if (!stack.isEmpty()) {
                    if (!canPickupItem(stack)) {
                        // 销毁卡：被过滤掉的物品直接销毁（清掉垃圾），不算入背包
                        if (shouldVoid(stack)) {
                            handler.extractItem(i, stack.getCount(), false);
                            collected = true;
                        }
                        continue;
                    }
                    net.minecraft.world.item.ItemStack remainder = stack.copy();
                    for (int j = 0; j < invSize; j++) {
                        remainder = inventory.insertItem(j, remainder, false);
                        if (remainder.isEmpty()) break;
                    }
                    if (remainder.getCount() < stack.getCount()) {
                        int toExtract = stack.getCount() - remainder.getCount();
                        handler.extractItem(i, toExtract, false);
                        collected = true;
                    }
                }
            }
            if (collected) {
                energyStorage.extractEnergy(energyCost, false);
                syncEnergy();
            }
        });
    }

    /**
     * 从源容器尽可能多地取物到机器人背包（直到容器空或背包满）。
     * 供 CollectGoal / TransportGoal 搬运使用。返回是否取到了东西。
     */
    public boolean transferFromContainerToInventory(BlockPos source) {
        var be = this.level().getBlockEntity(source);
        if (be == null) return false;
        var cap = be.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER);
        final boolean[] moved = {false};
        cap.ifPresent(handler -> {
            int invSize = getInventorySize();
            for (int i = 0; i < handler.getSlots(); i++) {
                if (isInventoryFull()) break;
                net.minecraft.world.item.ItemStack extracted = handler.extractItem(i, 64, true);
                if (extracted.isEmpty()) continue;
                if (!canPickupItem(extracted)) {
                    // 销毁卡：被过滤掉的物品直接销毁
                    if (shouldVoid(extracted)) {
                        handler.extractItem(i, extracted.getCount(), false);
                        moved[0] = true;
                    }
                    continue;
                }
                net.minecraft.world.item.ItemStack remainder = extracted.copy();
                for (int j = 0; j < invSize; j++) {
                    remainder = inventory.insertItem(j, remainder, false);
                    if (remainder.isEmpty()) break;
                }
                int movedCount = extracted.getCount() - remainder.getCount();
                if (movedCount > 0) {
                    handler.extractItem(i, movedCount, false);
                    moved[0] = true;
                }
            }
        });
        return moved[0];
    }

    /**
     * 从机器人背包尽可能多地卸货到目标容器（直到背包空或容器满）。
     * 供 CollectGoal / TransportGoal 搬运使用。返回是否卸出了东西。
     */
    public boolean transferFromInventoryToContainer(BlockPos dest) {
        var be = this.level().getBlockEntity(dest);
        if (be == null) return false;
        var cap = be.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER);
        final boolean[] moved = {false};
        cap.ifPresent(handler -> {
            int invSize = getInventorySize();
            for (int i = 0; i < invSize; i++) {
                net.minecraft.world.item.ItemStack stack = inventory.getStackInSlot(i);
                if (stack.isEmpty()) continue;
                net.minecraft.world.item.ItemStack remainder = stack.copy();
                for (int j = 0; j < handler.getSlots(); j++) {
                    remainder = handler.insertItem(j, remainder, false);
                    if (remainder.isEmpty()) break;
                }
                int movedCount = stack.getCount() - remainder.getCount();
                if (movedCount > 0) {
                    inventory.extractItem(i, movedCount, false);
                    moved[0] = true;
                }
            }
        });
        return moved[0];
    }

    /**
     * 运输模式起点：从容器抽流体进储罐（需要储罐卡）。
     * 罐空时抽取任意流体；罐里有流体时只抽取同种流体，避免混罐。
     * 每次至多抽 1000mB（1 桶），让多个起点能分摊搬运。
     * 返回是否真的抽到了流体。
     */
    public boolean transferFluidFromContainerToTank(BlockPos source) {
        if (!hasCard(ModItems.CARD_TANK)) return false;
        var be = this.level().getBlockEntity(source);
        if (be == null) return false;
        var cap = be.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER);
        if (!cap.isPresent()) return false;
        IFluidHandler handler = cap.orElse(null);
        if (handler == null) return false;

        int space = TANK_CAPACITY - tankFluid.getAmount();
        if (space <= 0) return false;

        FluidStack preview = handler.drain(Math.min(space, 1000), IFluidHandler.FluidAction.SIMULATE);
        if (preview.isEmpty()) return false;
        if (!tankFluid.isEmpty() && !tankFluid.getFluid().isSame(preview.getFluid())) return false;

        FluidStack drained = handler.drain(preview.getAmount(), IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) return false;
        if (tankFluid.isEmpty()) {
            tankFluid = drained.copy();
        } else {
            tankFluid.grow(drained.getAmount());
        }
        return true;
    }

    /**
     * 运输模式终点：从储罐注入流体到容器（需要储罐卡）。
     * 每次至多注 1000mB（1 桶）。返回是否真的注入了流体。
     */
    public boolean transferFluidFromTankToContainer(BlockPos dest) {
        if (!hasCard(ModItems.CARD_TANK)) return false;
        if (tankFluid.isEmpty()) return false;
        var be = this.level().getBlockEntity(dest);
        if (be == null) return false;
        var cap = be.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER);
        if (!cap.isPresent()) return false;
        IFluidHandler handler = cap.orElse(null);
        if (handler == null) return false;

        int amount = Math.min(tankFluid.getAmount(), 1000);
        FluidStack toFill = new FluidStack(tankFluid.getFluid(), amount);
        int filled = handler.fill(toFill, IFluidHandler.FluidAction.EXECUTE);
        if (filled <= 0) return false;
        tankFluid.shrink(filled);
        if (tankFluid.isEmpty()) tankFluid = FluidStack.EMPTY;
        return true;
    }

    /**
     * 状态机核心：根据电量更新机器人状态
     */
    private void updateRobotState() {
        RobotState current = getRobotState();
        int energy = getEnergy();
        int maxEnergy = energyStorage.getMaxEnergyStored();

        // 优先级 1：完全没电 —— 最初设定：0 电也缓慢移动去充电
        if (energy <= 0) {
            if (this.targetChargingStation == null) {
                BlockPos station = findNearestChargingStation();
                if (station != null) {
                    this.targetChargingStation = station;
                }
            }
            if (this.targetChargingStation != null) {
                if (current != RobotState.RETURNING_TO_CHARGE) {
                    setRobotState(RobotState.RETURNING_TO_CHARGE);
                }
            } else if (current != RobotState.OUT_OF_POWER) {
                // 附近没有充电站：没电待机（由 FollowOwnerGoal 的 OUT_OF_POWER 分支缓慢跟随）
                setRobotState(RobotState.OUT_OF_POWER);
            }
            return;
        }

        // 优先级 2：低电量自动返航（WORKING 状态且电量 < 20%）
        if (current == RobotState.WORKING && maxEnergy > 0 && energy < maxEnergy * 0.2) {
            if (this.targetChargingStation == null) {
                BlockPos station = findNearestChargingStation();
                if (station != null) {
                    this.targetChargingStation = station;
                }
            }
            if (this.targetChargingStation != null) {
                setRobotState(RobotState.RETURNING_TO_CHARGE);
            }
            return;
        }

        // 优先级 3：电量恢复后从 OUT_OF_POWER 恢复工作，并清除旧充电目标
        if (current == RobotState.OUT_OF_POWER && energy > 0) {
            setRobotState(RobotState.WORKING);
            this.targetChargingStation = null;
            return;
        }

        // 电量充足且在工作：清除残留的旧充电目标（满电弹出/取出后不再带着旧目标）
        if (current == RobotState.WORKING && this.targetChargingStation != null) {
            this.targetChargingStation = null;
        }
    }

    /**
     * 在周围 32 格范围内搜索最近的充电站
     */
    private BlockPos findNearestChargingStation() {
        BlockPos pos = this.blockPosition();
        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (int dx = -16; dx <= 16; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -16; dz <= 16; dz++) {
                    BlockPos check = pos.offset(dx, dy, dz);
                    if (this.level().getBlockState(check).is(ModBlocks.CHARGING_STATION.get())
                            || this.level().getBlockState(check).is(ModBlocks.CREATIVE_CHARGING_STATION.get())) {
                        double dist = pos.distSqr(check);
                        if (dist < nearestDist) {
                            nearestDist = dist;
                            nearest = check;
                        }
                    }
                }
            }
        }
        return nearest;
    }

    /**
     * 无线充电：由最近的充电站消耗自身电量为机器人充电（每 tick 少量）。
     * 无充电站或电量已满时不操作。
     */
    private void tickWirelessCharging() {
        BlockPos station = findNearestChargingStation();
        if (station == null) return;
        if (!(this.level().getBlockEntity(station) instanceof ChargingStationBlockEntity chargingStation)) return;
        int space = energyStorage.getMaxEnergyStored() - energyStorage.getEnergyStored();
        int want = Math.min(WIRELESS_CHARGE_RATE, space);
        int drawn = chargingStation.getEnergyStorage().extractEnergy(want, false);
        if (drawn > 0) {
            energyStorage.receiveEnergy(drawn, false);
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.level().isClientSide) return InteractionResult.SUCCESS;

        if (player.isShiftKeyDown()) {
            // 手持操控台 shift 右键：绑定到频道 / 再次 shift 右键同一机器人解除绑定
            ItemStack held = player.getItemInHand(hand);
            if (held.is(ModItems.CONTROLLER.get())) {
                String msg = com.cheng.search_bot.item.custom.RobotControllerItem.bindRobot(held, this);
                // 绑定/解绑后刷新当前频道的容器标记方框
                com.cheng.search_bot.item.custom.RobotControllerItem.syncMarkersForCurrent(held, this.level(), this.blockPosition());
                player.sendSystemMessage(Component.literal(msg));
                return InteractionResult.CONSUME;
            }
            // 手持信号控制器 shift 右键：绑定到该机器人（用稳定 RobotId，拆装不失效）
            if (held.is(ModItems.SIGNAL_CONTROLLER.get())) {
                held.getOrCreateTag().putUUID("BoundRobot", this.getRobotId());
                player.sendSystemMessage(Component.literal("已绑定信号控制器 → " + this.getRobotDisplayName().getString()));
                return InteractionResult.CONSUME;
            }
            // 手持红石卡 shift 右键：循环切换红石卡模式
            if (ModItems.CARD_REDSTONE.equals(ModItems.getCardType(held))) {
                this.cycleRedstoneMode();
                String modeName = switch (this.getRedstoneMode()) {
                    case 1 -> "高信号（收到红石信号暂停）";
                    case 2 -> "低信号（没有红石信号暂停）";
                    default -> "关闭";
                };
                player.sendSystemMessage(Component.literal("红石卡模式：" + modeName));
                return InteractionResult.CONSUME;
            }
            // 否则：拾取机器人
            ItemStack robotItem = new ItemStack(ModItems.ROBOT_ITEM.get());
            CompoundTag tag = new CompoundTag();
            saveRobotData(tag);
            robotItem.setTag(tag);
            if (hasCustomName()) {
                robotItem.setHoverName(getCustomName()); // 拾取后物品也保留自定义名
            }

            if (!player.addItem(robotItem)) {
                this.spawnAtLocation(robotItem);
            }
            this.discard();
            return InteractionResult.CONSUME;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            ItemStack held = player.getItemInHand(hand);
            // 储罐卡 + 桶：空桶装流体 / 满桶抽流体
            if (hand == InteractionHand.MAIN_HAND && hasCard(ModItems.CARD_TANK)
                    && held.getItem() instanceof BucketItem bucketItem) {
                InteractionResult result = handleTankBucket(player, held, bucketItem);
                if (result != null) return result;
            }
            if (hand == InteractionHand.MAIN_HAND && held.isEmpty()) {
                // 右键打开机器人仓库 GUI；顶部有分页栏，可切换各个储物箱
                openRobotInventory(serverPlayer);
                return InteractionResult.CONSUME;
            }
        }

        return super.mobInteract(player, hand);
    }

    /** 打开机器人自己的仓库 GUI（顶部有分页栏：各储物箱）。 */
    private void openRobotInventory(ServerPlayer serverPlayer) {
        NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                (id, inv, p) -> new RobotInventoryMenu(id, inv, this),
                Component.translatable("entity.search_bot.search_robot")
        ), buf -> {
            buf.writeInt(this.getInventorySize());
            int[] boxes = this.getBoxSlots();
            buf.writeInt(boxes.length);
            for (int b : boxes) buf.writeInt(b);
            buf.writeInt(this.getEnergyStorage().getEnergyStored());
            buf.writeInt(this.getEnergyStorage().getMaxEnergyStored());
            buf.writeInt(this.getFilterMode());
            buf.writeInt(this.isIgnoreFilterNbt() ? 1 : 0);
            buf.writeInt(this.getMode().id);
            buf.writeInt(this.getCardSlots());
            buf.writeInt(this.getRedstoneMode());
            buf.writeInt(this.getTankFluid().getAmount());
            buf.writeComponent(this.getTankFluid().isEmpty()
                    ? Component.empty()
                    : this.getTankFluid().getDisplayName());
        });
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player && player.isShiftKeyDown()) {
            return false;
        }
        // 防火躯干：免疫火焰/岩浆/灼烧类直接伤害（fireImmune 已挡点燃，这里再挡岩浆等直接伤害）
        if (isFireproof() && source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
            return false;
        }
        // 躯干伤害减算：最终伤害 = max(0, 伤害 - 减算)，例：1 点伤害 - 1 点减算 = 0
        int reduction = getStatInt("damage_reduction", 0);
        float reduced = Math.max(0.0f, amount - reduction);
        return super.hurt(source, reduced);
    }

    /** 防火躯干：火焰免疫（不会被火/岩浆点燃） */
    @Override
    public boolean fireImmune() {
        return isFireproof() || super.fireImmune();
    }

    /** 死亡时释放编号，供后续制作的核心复用空缺编号（核心已随机器人消失）。 */
    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!this.level().isClientSide && this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            com.cheng.search_bot.util.RobotNameCounter.get(serverLevel).release(this.robotNumber);
        }
    }

    /** 摔落伤害减免：螺旋桨完全免疫 */
    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        if (this.canFly) return false;
        return super.causeFallDamage(fallDistance, multiplier, source);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        saveRobotData(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        loadRobotData(tag);
        // 区块重载后重新注册 AI（registerGoals() 只注册了 FloatGoal）
        if (!this.level().isClientSide) {
            determineModeAndRegisterGoals(this.blockPosition(), false);
        }
    }

    public void saveRobotData(CompoundTag tag) {
        if (ownerUUID != null) tag.putUUID("Owner", ownerUUID);
        if (getRobotId() != null) tag.putUUID("RobotId", getRobotId());
        if (robotNumber > 0) tag.putInt("RobotNumber", robotNumber);
        if (hasCustomName()) tag.putString("CustomName", Component.Serializer.toJson(getCustomName()));
        if (homeStoragePos != null) tag.putLong("HomeStorage", homeStoragePos.asLong());
        tag.put("Inventory", inventory.serializeNBT());
        tag.putInt("Energy", energyStorage.getEnergyStored());
        tag.putInt("Mode", getMode().id);
        tag.putInt("RobotState", getRobotState().id);
        if (targetChargingStation != null) {
            tag.putLong("TargetChargingStation", targetChargingStation.asLong());
        }
        if (collectTargetPos != null) tag.putLong("CollectTarget", collectTargetPos.asLong());
        tag.putInt("FilterMode", filterMode);
        tag.putBoolean("IgnoreFilterNbt", ignoreFilterNbt);
        tag.put("FilterItems", filterItems.serializeNBT());
        tag.put("Cards", cardInventory.serializeNBT());
        tag.putInt("RedstoneMode", redstoneMode);
        if (!tankFluid.isEmpty()) {
            tag.put("FluidTank", tankFluid.writeToNBT(new CompoundTag()));
        }
        if (!transportWaypoints.isEmpty()) {
            ListTag list = new ListTag();
            for (TransportWaypoint w : transportWaypoints) {
                CompoundTag wt = new CompoundTag();
                wt.putLong("Pos", w.pos.asLong());
                wt.putBoolean("IsStart", w.isStart);
                list.add(wt);
            }
            tag.put("TransportWaypoints", list);
        }
        tag.put("parts", partsTag);
        tag.put("stats", statsTag);
    }

    public void loadRobotData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) ownerUUID = tag.getUUID("Owner");
        if (tag.hasUUID("RobotId")) {
            robotId = tag.getUUID("RobotId");
            this.entityData.set(DATA_ROBOT_ID, Optional.of(robotId));
        }
        if (tag.contains("RobotNumber")) robotNumber = tag.getInt("RobotNumber");
        if (tag.contains("CustomName", Tag.TAG_STRING)) {
            try {
                setCustomName(Component.Serializer.fromJson(tag.getString("CustomName")));
            } catch (Exception ignored) {
                // 名称数据损坏时忽略，避免影响加载
            }
        }
        if (tag.contains("HomeStorage")) homeStoragePos = BlockPos.of(tag.getLong("HomeStorage"));
        if (tag.contains("Inventory")) inventory.deserializeNBT(tag.getCompound("Inventory"));
        if (tag.contains("Mode")) {
            setMode(RobotMode.byId(tag.getInt("Mode")));
        } else {
            setMode(RobotMode.FOLLOW);
        }
        if (tag.contains("RobotState")) {
            setRobotState(RobotState.byId(tag.getInt("RobotState")));
        } else {
            setRobotState(RobotState.WORKING);
        }
        if (tag.contains("TargetChargingStation")) {
            targetChargingStation = BlockPos.of(tag.getLong("TargetChargingStation"));
        }
        if (tag.contains("CollectTarget")) {
            collectTargetPos = BlockPos.of(tag.getLong("CollectTarget"));
        } else {
            collectTargetPos = null;
        }
        if (tag.contains("FilterMode")) filterMode = tag.getInt("FilterMode");
        if (tag.contains("IgnoreFilterNbt")) ignoreFilterNbt = tag.getBoolean("IgnoreFilterNbt");
        if (tag.contains("FilterItems")) filterItems.deserializeNBT(tag.getCompound("FilterItems"));
        if (tag.contains("Cards")) cardInventory.deserializeNBT(tag.getCompound("Cards"));
        if (tag.contains("RedstoneMode")) redstoneMode = tag.getInt("RedstoneMode");
        if (tag.contains("FluidTank")) tankFluid = FluidStack.loadFluidStackFromNBT(tag.getCompound("FluidTank"));
        transportWaypoints.clear();
        if (tag.contains("TransportWaypoints", Tag.TAG_LIST)) {
            ListTag list = tag.getList("TransportWaypoints", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag wt = list.getCompound(i);
                transportWaypoints.add(new TransportWaypoint(BlockPos.of(wt.getLong("Pos")), wt.getBoolean("IsStart")));
            }
        }
        // 向后兼容：旧版本用单点 TransportStart/TransportEnd
        if (transportWaypoints.isEmpty()) {
            if (tag.contains("TransportStart")) {
                transportWaypoints.add(new TransportWaypoint(BlockPos.of(tag.getLong("TransportStart")), true));
            }
            if (tag.contains("TransportEnd")) {
                transportWaypoints.add(new TransportWaypoint(BlockPos.of(tag.getLong("TransportEnd")), false));
            }
        }
        if (tag.contains("parts")) {
            partsTag = tag.getCompound("parts").copy();
            updateCanFly();
        }
        // 必须先应用 stats（设置容量），再设置电量，避免电量被默认容量截断
        if (tag.contains("stats")) {
            statsTag = tag.getCompound("stats").copy();
            applyStats(statsTag);
        }
        // 修复旧存档：背包处理器可能因旧版本把 Size 写小（如硬编码 90）而小于 inventory_size，
        // 扩容到至少 inventory_size，避免打开仓库 GUI 时槽位越界崩溃（Slot N not in valid range）。
        int neededSlots = getInventorySize();
        if (inventory.getSlots() < neededSlots) {
            inventory.setSize(neededSlots);
        }
        if (tag.contains("Energy")) {
            energyStorage.setEnergy(tag.getInt("Energy"));
        } else {
            // 无 Energy 标签（如创造模式直接取出机器人）默认满电，避免没电不动
            energyStorage.setEnergy(energyStorage.getMaxEnergyStored());
        }
        syncEnergy();
    }

    private void applyStats(CompoundTag stats) {
        if (stats.contains("max_energy")) {
            energyStorage.setCapacity(stats.getInt("max_energy"));
        }
        if (stats.contains("max_health") && this.getAttribute(Attributes.MAX_HEALTH) != null) {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(stats.getDouble("max_health"));
            this.setHealth((float) stats.getDouble("max_health"));
        }
        if (stats.contains("movement_speed") && this.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(stats.getDouble("movement_speed"));
            // 飞行速度与陆地移动速度统一：都用同一个速度算子值，保证飞行不慢于步行
            if (this.getAttribute(Attributes.FLYING_SPEED) != null) {
                this.getAttribute(Attributes.FLYING_SPEED).setBaseValue(stats.getDouble("movement_speed"));
            }
        }
        // 存储 stats 供 AI Goal 读取
        this.statsTag = stats;
    }

    /** 从 stats 读取整数属性，若不存在返回默认值 */
    public int getStatInt(String key, int fallback) {
        return statsTag.contains(key) ? statsTag.getInt(key) : fallback;
    }

    /** 从 stats 读取小数属性，若不存在返回默认值 */
    public double getStatDouble(String key, double fallback) {
        return statsTag.contains(key) ? statsTag.getDouble(key) : fallback;
    }

    /** 从 stats 读取交互半径（机械臂决定，够得着多远） */
    public double getInteractionRadius() {
        return getStatDouble("interact_radius", 1.0);
    }

    /** 从 stats 读取搜索半径（传感器决定，探索/寻找容器范围） */
    public int getSearchRadius() {
        return getStatInt("search_radius", 6);
    }

    /** 从 stats 读取每次取物数量 */
    public int getCollectAmount() {
        return getStatInt("collect_amount", 1);
    }

    /** 从 stats 读取取物耗电 */
    public int getCollectEnergyCost() {
        return getStatInt("collect_energy_cost", 50);
    }

    /** 从 stats 读取取物冷却 */
    public int getCollectCooldown() {
        return getStatInt("collect_cooldown", 20);
    }

    /** 从 stats 读取移动耗电/tick */
    public int getMoveEnergyCost() {
        return getStatInt("move_energy_cost", 1);
    }

    /** 躯干是否防火（下界合金/终极）：防火的机器人不会被火点燃、不吃火焰/岩浆伤害 */
    public boolean isFireproof() {
        return statsTag.contains("fireproof") && statsTag.getBoolean("fireproof");
    }

    public void setOwner(UUID uuid) {
        this.ownerUUID = uuid;
    }

    @Nullable
    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    /** 机器人稳定标识（跨充电站存取、物品↔实体转换不变），供操控台多绑定精确匹配 */
    public UUID getRobotId() {
        // 客户端读同步值（白框等渲染用），服务端懒生成并同步，保证两端 id 一致
        if (this.level().isClientSide) {
            return this.entityData.get(DATA_ROBOT_ID).orElse(null);
        }
        if (robotId == null) {
            robotId = UUID.randomUUID();
            this.entityData.set(DATA_ROBOT_ID, Optional.of(robotId));
        }
        return robotId;
    }

    /** 机器人编号（由核心分配），未分配为 0。 */
    public int getRobotNumber() {
        return robotNumber;
    }

    public void setRobotNumber(int number) {
        this.robotNumber = number;
    }

    /**
     * 机器人显示名：铁砧命名（CustomName）优先；否则显示编号名「机器人NN」；都没有则回退默认实体名。
     * 供操控台 GUI 显示机器人名称使用。
     */
    public Component getRobotDisplayName() {
        if (hasCustomName()) {
            return getCustomName();
        }
        if (robotNumber > 0) {
            return Component.literal(com.cheng.search_bot.util.RobotNameCounter.format(robotNumber));
        }
        return getName();
    }

    public Player getOwner() {
        if (ownerUUID == null || this.level().isClientSide) return null;
        return this.level().getPlayerByUUID(ownerUUID);
    }

    public void setHomeStorage(BlockPos pos) {
        this.homeStoragePos = pos;
    }

    @Nullable
    public BlockPos getHomeStorage() {
        return homeStoragePos;
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    /** 各储物箱的格数。box_slots 缺失时（旧机器人）回退为「单一整包」。 */
    public int[] getBoxSlots() {
        if (statsTag.contains("box_slots", Tag.TAG_LIST)) {
            ListTag list = statsTag.getList("box_slots", Tag.TAG_INT);
            int[] arr = new int[list.size()];
            for (int i = 0; i < arr.length; i++) arr[i] = list.getInt(i);
            return arr;
        }
        return new int[]{getInventorySize()};
    }

    public int getBoxCount() {
        return getBoxSlots().length;
    }

    /** 第 box 个箱子在平铺背包里的起始槽位 */
    public int getBoxStart(int box) {
        int[] slots = getBoxSlots();
        int start = 0;
        for (int i = 0; i < box && i < slots.length; i++) start += slots[i];
        return start;
    }

    /** 过滤模式：0=无过滤，1=黑名单，2=白名单 */
    public int getFilterMode() {
        return filterMode;
    }

    public void setFilterMode(int mode) {
        this.filterMode = mode;
    }

    /** 是否忽略物品后缀（NBT） */
    public boolean isIgnoreFilterNbt() {
        return ignoreFilterNbt;
    }

    public void setIgnoreFilterNbt(boolean ignore) {
        this.ignoreFilterNbt = ignore;
    }

    /** 过滤物品列表（仓库右侧槽位） */
    public ItemStackHandler getFilterItems() {
        return filterItems;
    }

    /**
     * 判断某物品是否允许机器人拿取（依据仓库黑白名单）。
     * 无过滤返回 true；黑名单：不在列表里才拿；白名单：在列表里才拿。
     * <p>过滤卡未装时，黑白名单不生效，机器人什么都拿。
     */
    public boolean canPickupItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return true;
        if (!hasCard(ModItems.CARD_FILTER)) return true; // 没过滤卡：不过滤
        if (filterMode == 0) return true;
        boolean matched = matchesFilter(stack);
        return filterMode == 1 ? !matched : matched;
    }

    /** 判断物品是否命中过滤列表（ignoreFilterNbt 决定是否忽略 NBT） */
    private boolean matchesFilter(ItemStack stack) {
        for (int i = 0; i < filterItems.getSlots(); i++) {
            ItemStack f = filterItems.getStackInSlot(i);
            if (f.isEmpty()) continue;
            if (ignoreFilterNbt) {
                if (f.getItem() == stack.getItem()) return true;
            } else {
                if (ItemStack.isSameItemSameTags(f, stack)) return true;
            }
        }
        return false;
    }

    /** 销毁卡：过滤卡存在 + 名单过滤生效 + 该物品被过滤掉时，销毁它而非跳过。 */
    private boolean shouldVoid(ItemStack stack) {
        return hasCard(ModItems.CARD_VOID)
                && hasCard(ModItems.CARD_FILTER)
                && filterMode != 0
                && !canPickupItem(stack);
    }

    // ==================== 插件卡 ====================

    /** 插件卡背包（最多 6 格，实际可用由 card_slots 决定） */
    public ItemStackHandler getCardInventory() {
        return cardInventory;
    }

    /** 当前传感器等级决定的插件卡槽数（statsTag 中的 card_slots，默认 1） */
    public int getCardSlots() {
        return getStatInt("card_slots", 1);
    }

    /** 判断是否装入了某张插件卡。 */
    public boolean hasCard(String cardId) {
        for (int i = 0; i < cardInventory.getSlots(); i++) {
            ItemStack card = cardInventory.getStackInSlot(i);
            if (!card.isEmpty() && cardId.equals(ModItems.getCardType(card))) return true;
        }
        return false;
    }

    /** 判断是否装入了任意材质档的堆叠卡。 */
    public boolean hasStackingCard() {
        for (int i = 0; i < cardInventory.getSlots(); i++) {
            ItemStack card = cardInventory.getStackInSlot(i);
            if (!card.isEmpty() && ModItems.isStackingCard(ModItems.getCardType(card))) return true;
        }
        return false;
    }

    /**
     * 当前堆叠卡决定的单格堆叠上限（无堆叠卡返回 64）。
     * 取装入的堆叠卡里最高的一档（最多 {@link ModItems#MAX_STACKING_CARDS} 张生效）。
     */
    public int getStackingLimit() {
        int best = 0;
        int count = 0;
        for (int i = 0; i < cardInventory.getSlots(); i++) {
            ItemStack card = cardInventory.getStackInSlot(i);
            if (card.isEmpty()) continue;
            Integer limit = ModItems.getStackingLimit(ModItems.getCardType(card));
            if (limit != null) {
                count++;
                if (count <= ModItems.MAX_STACKING_CARDS) best = Math.max(best, limit);
            }
        }
        return best > 64 ? best : 64;
    }

    /** 红石卡模式：0=无，1=高信号(有信号暂停)，2=低信号(无信号暂停) */
    public int getRedstoneMode() {
        return redstoneMode;
    }

    /** 循环切换红石卡模式（无 → 高信号 → 低信号 → 无） */
    public void cycleRedstoneMode() {
        redstoneMode = (redstoneMode + 1) % 3;
    }

    /** 信号控制器实时推送的当前电平（仅服务端调用） */
    public void setRedstonePowered(boolean powered) {
        this.redstonePowered = powered;
    }

    /** 红石卡生效且当前电平触发暂停时返回 true（AI 目标据此停止工作） */
    public boolean isRedstonePaused() {
        if (!hasCard(ModItems.CARD_REDSTONE)) return false;
        return switch (redstoneMode) {
            case 1 -> redstonePowered;    // 高信号：收到红石信号暂停
            case 2 -> !redstonePowered;   // 低信号：没有红石信号暂停
            default -> false;
        };
    }

    // ==================== 储罐卡（流体） ====================

    public FluidStack getTankFluid() {
        return tankFluid;
    }

    public int getTankCapacity() {
        return TANK_CAPACITY;
    }

    /**
     * 储罐卡 + 桶交互。
     * <ul>
     *   <li>空桶 + 罐内有流体：装满对应流体桶，罐内减 1000mB。</li>
     *   <li>满桶 + 罐内有空间（或同流体）：桶内流体入罐，返还空桶。</li>
     * </ul>
     * 牛奶桶等无 Forge 流体的桶会被跳过（返回 null 走后续逻辑）。
     *
     * @return 已处理返回 CONSUME，未处理返回 null
     */
    @Nullable
    private InteractionResult handleTankBucket(Player player, ItemStack held, BucketItem bucketItem) {
        // 空桶：从罐里装流体
        if (held.is(Items.BUCKET)) {
            if (tankFluid.isEmpty() || tankFluid.getAmount() < 1000) {
                player.sendSystemMessage(Component.literal("机器人储罐是空的"));
                return InteractionResult.CONSUME;
            }
            Item bucketItem2 = tankFluid.getFluid().getBucket();
            held.shrink(1);
            ItemStack filled = new ItemStack(bucketItem2);
            if (!player.addItem(filled)) player.drop(filled, false);
            tankFluid.shrink(1000);
            if (tankFluid.isEmpty()) tankFluid = FluidStack.EMPTY;
            return InteractionResult.CONSUME;
        }
        // 满桶：往罐里倒流体
        Fluid fluid = bucketItem.getFluid();
        if (fluid == Fluids.EMPTY) return null; // 牛奶等无流体桶：跳过
        if (!tankFluid.isEmpty() && !tankFluid.getFluid().isSame(fluid)) {
            player.sendSystemMessage(Component.literal("机器人储罐里是别的流体"));
            return InteractionResult.CONSUME;
        }
        int space = TANK_CAPACITY - tankFluid.getAmount();
        if (space < 1000) {
            player.sendSystemMessage(Component.literal("机器人储罐已满"));
            return InteractionResult.CONSUME;
        }
        if (tankFluid.isEmpty()) tankFluid = new FluidStack(fluid, 1000);
        else tankFluid.grow(1000);
        held.shrink(1);
        if (!player.addItem(new ItemStack(Items.BUCKET))) player.drop(new ItemStack(Items.BUCKET), false);
        return InteractionResult.CONSUME;
    }

    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    public boolean isInventoryFull() {
        int size = getInventorySize();
        for (int i = 0; i < size; i++) {
            if (inventory.getStackInSlot(i).isEmpty()) return false;
        }
        return true;
    }

    public boolean isInventoryEmpty() {
        int size = getInventorySize();
        for (int i = 0; i < size; i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) return false;
        }
        return true;
    }

    /** 判断指定位置容器内是否有任何物品（供收集/运输目标查询源容器使用） */
    public boolean containerHasItems(BlockPos pos) {
        var be = this.level().getBlockEntity(pos);
        if (be == null) return false;
        var cap = be.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER);
        return cap.map(handler -> {
            for (int i = 0; i < handler.getSlots(); i++) {
                if (!handler.getStackInSlot(i).isEmpty()) return true;
            }
            return false;
        }).orElse(false);
    }

    /** 判断容器内是否有「机器人可拿走」的物品（考虑黑白名单），避免反复尝试已空/被过滤的容器 */
    public boolean containerHasCollectibleItems(BlockPos pos) {
        var be = this.level().getBlockEntity(pos);
        if (be == null) return false;
        var cap = be.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER);
        return cap.map(handler -> {
            for (int i = 0; i < handler.getSlots(); i++) {
                net.minecraft.world.item.ItemStack stack = handler.getStackInSlot(i);
                if (!stack.isEmpty() && canPickupItem(stack)) return true;
            }
            return false;
        }).orElse(false);
    }

    /** 判断指定位置容器是否已满（没有任何空格、也没有未满的格子，供运输终点跳过判断使用） */
    public boolean containerIsFull(BlockPos pos) {
        var be = this.level().getBlockEntity(pos);
        if (be == null) return false;
        var cap = be.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER);
        return cap.map(handler -> {
            for (int i = 0; i < handler.getSlots(); i++) {
                net.minecraft.world.item.ItemStack stack = handler.getStackInSlot(i);
                if (stack.isEmpty()) return false; // 有空位
                if (stack.getCount() < stack.getMaxStackSize()) return false; // 该格未满
            }
            return true;
        }).orElse(false);
    }

    public int getInventorySize() {
        return statsTag.contains("inventory_size") ? statsTag.getInt("inventory_size") : 9;
    }

    public int getEnergy() {
        return this.entityData.get(DATA_ENERGY);
    }

    public RobotState getRobotState() {
        return RobotState.byId(this.entityData.get(DATA_ROBOT_STATE));
    }

    public void setRobotState(RobotState state) {
        this.entityData.set(DATA_ROBOT_STATE, state.id);
    }

    @Nullable
    public BlockPos getTargetChargingStation() {
        return targetChargingStation;
    }

    public void setTargetChargingStation(@Nullable BlockPos pos) {
        this.targetChargingStation = pos;
    }

    public float getFollowSpeed() {
        return (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED);
    }

    public RobotMode getMode() {
        return RobotMode.byId(this.entityData.get(DATA_ROBOT_MODE));
    }

    public void setMode(RobotMode mode) {
        this.entityData.set(DATA_ROBOT_MODE, mode.id);
    }

    /**
     * 清空并停止所有 AI 目标。
     *
     * <p>注意：原版 {@code GoalSelector.removeAllGoals} 只把目标从集合里移除，
     * <b>不会停止正在运行的目标、也不会释放其 flag 锁</b>。直接 removeAllGoals 后重新
     * registerGoals，旧目标（如正在跟随的 FollowOwnerGoal）仍占着 MOVE/LOOK 锁，
     * 新注册的跟随/收集目标全部无法启动，机器人就「呆在原地」。
     * 所以这里逐个 {@code removeGoal}（它会正确 stop 运行中的目标、把 running 置 false，
     * 下一 tick 由 GoalSelector 自动释放 flag 锁）。
     */
    private void clearAllGoals() {
        java.util.List<net.minecraft.world.entity.ai.goal.Goal> snapshot = new java.util.ArrayList<>();
        for (net.minecraft.world.entity.ai.goal.WrappedGoal wg : this.goalSelector.getAvailableGoals()) {
            snapshot.add(wg.getGoal());
        }
        for (net.minecraft.world.entity.ai.goal.Goal goal : snapshot) {
            this.goalSelector.removeGoal(goal);
        }
        // 停寻路的同时把移动控制器目标也归零，避免切模式后残留旧速度/旧目标、
        // 飞行机器人继续朝旧目标漂移（「改模式没反应」的一部分根因）。
        this.getNavigation().stop();
        this.getMoveControl().setWantedPosition(this.getX(), this.getY(), this.getZ(), 0.0);
        this.setDeltaMovement(Vec3.ZERO);
    }

    /** 服务端切换工作模式并重注册 AI（仅服务端调用） */
    public void switchMode(RobotMode mode) {
        if (this.level().isClientSide) return;
        setMode(mode);
        clearAllGoals();
        this.registerGoalsByMode();
        LOGGER.info("[SearchRobot] switchMode -> {} (owner={}, state={}, energy={})",
                mode.cnName(), getOwnerUUID(), getRobotState().name(), getEnergy());
    }

    /** 根据当前已设置的容器，返回操控台右键空气时的模式循环列表（待命恒在末尾） */
    public java.util.List<RobotMode> getModeCycle() {
        java.util.List<RobotMode> cycle = new java.util.ArrayList<>();
        cycle.add(RobotMode.FOLLOW);
        cycle.add(RobotMode.EXPLORE);
        if (collectTargetPos != null) cycle.add(RobotMode.COLLECT);
        if (!transportWaypoints.isEmpty()) cycle.add(RobotMode.TRANSPORT);
        cycle.add(RobotMode.STANDBY);
        return cycle;
    }

    /** 循环切换到下一个可用模式（操控台右键空气 / 仓库标题点击调用） */
    public void cycleMode() {
        java.util.List<RobotMode> cycle = getModeCycle();
        RobotMode current = getMode();
        int idx = cycle.indexOf(current);
        if (idx < 0) idx = 0;

        // 探索模式要求背包有空位；背包满时直接跳过探索，切到下一个可用模式，
        // 不再退回跟随 + 弹聊天栏提示（模式变化只在仓库标题里体现）。
        RobotMode next = null;
        for (int i = 1; i <= cycle.size(); i++) {
            RobotMode candidate = cycle.get((idx + i) % cycle.size());
            if (candidate == RobotMode.EXPLORE && isInventoryFull()) {
                continue;
            }
            next = candidate;
            break;
        }
        if (next == null) next = current;
        switchMode(next);
    }

    /** 给主人发送一条聊天栏提示（仅服务端） */
    public void notifyOwner(String message) {
        Player owner = getOwner();
        if (owner != null) {
            owner.sendSystemMessage(Component.literal(message));
        }
    }

    @Nullable
    public BlockPos getCollectTargetPos() {
        return collectTargetPos;
    }

    public void setCollectTargetPos(@Nullable BlockPos pos) {
        this.collectTargetPos = pos;
    }

    /** 从另一机器人复制全部运输/收集标记（绑定到已有机器人的频道时，让新机器人继承同频道标记） */
    public void copyMarkersFrom(SearchRobotEntity other) {
        this.transportWaypoints.clear();
        for (TransportWaypoint w : other.transportWaypoints) {
            this.transportWaypoints.add(new TransportWaypoint(w.pos, w.isStart));
        }
        this.collectTargetPos = other.collectTargetPos;
    }

    /** 全部运输点位（有序，点击顺序） */
    public List<TransportWaypoint> getTransportWaypoints() {
        return transportWaypoints;
    }

    /** 所有运输起点（蓝色标记） */
    public List<BlockPos> getTransportStarts() {
        List<BlockPos> list = new ArrayList<>();
        for (TransportWaypoint w : transportWaypoints) {
            if (w.isStart) list.add(w.pos);
        }
        return list;
    }

    /** 所有运输终点（橙色标记） */
    public List<BlockPos> getTransportEnds() {
        List<BlockPos> list = new ArrayList<>();
        for (TransportWaypoint w : transportWaypoints) {
            if (!w.isStart) list.add(w.pos);
        }
        return list;
    }

    /** 该位置是否已设为运输起点 */
    public boolean isTransportStart(BlockPos pos) {
        for (TransportWaypoint w : transportWaypoints) {
            if (w.pos.equals(pos)) return w.isStart;
        }
        return false;
    }

    /** 左键容器：切换运输起点。已是起点则取消；否则移除该位置旧点位后新增起点。运输与收集互斥：设起点时取消收集目标。 */
    public void toggleTransportStart(BlockPos pos) {
        // 运输与收集互斥：设置起点时取消收集目标（同频道）
        collectTargetPos = null;
        for (int i = 0; i < transportWaypoints.size(); i++) {
            TransportWaypoint w = transportWaypoints.get(i);
            if (w.pos.equals(pos)) {
                if (w.isStart) {
                    transportWaypoints.remove(i);
                } else {
                    transportWaypoints.set(i, new TransportWaypoint(pos, true));
                }
                return;
            }
        }
        transportWaypoints.add(new TransportWaypoint(pos, true));
    }

    /**
     * 右键容器：切换运输终点。已是终点则取消；否则设为终点。
     * 运输与收集互斥：设终点时取消收集目标。返回给玩家的提示信息。
     */
    public String handleControllerRightClick(BlockPos pos) {
        for (int i = 0; i < transportWaypoints.size(); i++) {
            TransportWaypoint w = transportWaypoints.get(i);
            if (w.pos.equals(pos) && !w.isStart) {
                transportWaypoints.remove(i);
                return "已取消运输终点";
            }
        }
        // 设为终点（先移除该位置已有点位，避免同位置既是起点又是终点）
        transportWaypoints.removeIf(w -> w.pos.equals(pos));
        transportWaypoints.add(new TransportWaypoint(pos, false));
        collectTargetPos = null; // 运输与收集互斥
        return "已设置运输终点（橙色标记）";
    }

    /**
     * shift 右键容器：切换收集目标。已是收集目标则取消；否则设为收集目标。
     * 运输与收集互斥：设收集目标时清空所有运输点位。返回给玩家的提示信息。
     */
    public String toggleCollectTarget(BlockPos pos) {
        if (pos.equals(collectTargetPos)) {
            collectTargetPos = null;
            return "已取消收集目标容器";
        }
        collectTargetPos = pos;
        transportWaypoints.clear(); // 运输与收集互斥
        return "已设置收集目标容器（绿色标记），并清空同频道所有运输起点和终点";
    }

    public boolean canFly() {
        return this.canFly;
    }

    /** 当前是否正在飞行（有螺旋桨且目标在上方） */
    public boolean isFlyingActive() {
        return this.flyingActive;
    }

    /** 立刻刹停：停寻路 + 移动控制器归零 + 清空速度（到达目的地用，避免带惯性冲过头） */
    public void brake() {
        this.getNavigation().stop();
        this.getMoveControl().setWantedPosition(this.getX(), this.getY(), this.getZ(), 0.0);
        this.setDeltaMovement(Vec3.ZERO);
    }

    /** 到达目的地时自然减速：停寻路 + 移动控制器归零，但不清零速度，
     *  让残余速度靠摩擦（步行）或 travel() 里的指数衰减（飞行）自然滑停，比瞬间归零更自然 */
    public void smoothStop() {
        this.getNavigation().stop();
        this.getMoveControl().setWantedPosition(this.getX(), this.getY(), this.getZ(), 0.0);
        this.setDeltaMovement(this.getDeltaMovement().scale(0.5));
    }

    /**
     * 传送装置：瞬移到目标容器四周可悬停的有效区域（交互范围内）。返回是否真的瞬移成功；
     * 冷却中或四周无安全空位时返回 false，由 AI 回退到常规寻路。
     * 配合螺旋桨无视重力，无需实体落脚点，只要该格是空气即可悬停。
     */
    public boolean teleportNearTarget(BlockPos target) {
        if (teleportCooldown > 0) return false;
        BlockPos spot = findTeleportPosition(target);
        if (spot == null) return false;
        this.teleportTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
        this.fallDistance = 0.0f;
        this.brake();
        this.teleportCooldown = 10;
        energyStorage.extractEnergy(50, false); // 瞬移耗电
        syncEnergy();
        return true;
    }

    /**
     * 传送装置辅助移动：目标位置超过 10 格时直接瞬移（冷却中则原地等待），返回 true 表示
     * 本 tick 由传送接管、调用方不要再寻路；10 格内返回 false，调用方走常规寻路。
     */
    public boolean teleportIfFar(BlockPos target) {
        if (!hasTeleporter()) return false;
        double dx = target.getX() + 0.5 - this.getX();
        double dy = target.getY() - this.getY();
        double dz = target.getZ() + 0.5 - this.getZ();
        if (dx * dx + dy * dy + dz * dz <= 10.0 * 10.0) return false; // 10 格内：常规移动
        teleportNearTarget(target); // 冷却中或四周无安全空位时返回 false，这里不关心
        return true; // 远距离一律交给瞬移接管，不寻路
    }

    /** 在目标容器四周找可悬停的空位（避开固体/岩浆/水，头顶也不能有障碍） */
    private BlockPos findTeleportPosition(BlockPos target) {
        BlockPos[] candidates = {
                target.above(), target.north(), target.south(), target.east(), target.west(), target.below(),
                target.above(2), target.north(2), target.south(2), target.east(2), target.west(2)
        };
        for (BlockPos p : candidates) {
            if (isSafeHoverPosition(p)) return p;
        }
        return null;
    }

    /** 悬停位有效：该格与头顶一格都是空气（非固体、非岩浆、非水） */
    private boolean isSafeHoverPosition(BlockPos pos) {
        return this.level().getBlockState(pos).isAir()
                && this.level().getBlockState(pos.above()).isAir();
    }

    private void syncEnergy() {
        this.entityData.set(DATA_ENERGY, energyStorage.getEnergyStored());
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true; // 有主人，不应被刷掉
    }
}
