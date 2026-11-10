package com.cheng.search_bot.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import com.cheng.search_bot.energy.CustomEnergyStorage;
import com.cheng.search_bot.entity.ModEntities;
import com.cheng.search_bot.entity.custom.SearchRobotEntity;
import com.cheng.search_bot.item.ModItems;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 充电站 BlockEntity
 * - 无 GUI
 * - 普通充电站需要外部供电（内部能量耗尽即停止充电）；创造充电站无限电量
 * - 单机器人舱位 + 单物品充电槽
 * - 自动极速充电，充满后自动释放
 */
public class ChargingStationBlockEntity extends BlockEntity {
    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final CustomEnergyStorage energyStorage = new CustomEnergyStorage(100000, 100000, 100000) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (!simulate && received > 0) {
                setChanged();
            }
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = super.extractEnergy(maxExtract, simulate);
            if (!simulate && extracted > 0) {
                setChanged();
            }
            return extracted;
        }
    };

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> lazyEnergyHandler = LazyOptional.empty();

    // ====== v0.2.0 新增：机器人存入/充电/释放 ======
    private CompoundTag storedRobot = new CompoundTag();
    private boolean hasRobot = false;
    private int chargeTicks = 0; // 机器人在站内已停留的 tick 数
    private static final int CHARGE_RATE = 5000; // 每 tick 充 5000 FE
    private static final int MIN_STAY_TICKS = 40; // 最短驻留 40 tick (2 秒)

    public ChargingStationBlockEntity(BlockPos pos, BlockState state) {
        super(com.cheng.search_bot.block.entity.ModBlockEntities.CHARGING_STATION.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ChargingStationBlockEntity station) {
        if (level.isClientSide) return;

        // 创造充电站：无需供电，秒充满
        boolean creative = state.is(com.cheng.search_bot.block.ModBlocks.CREATIVE_CHARGING_STATION.get());

        // 创造充电站：无限电量，自动补满；普通充电站：需要外部供电，能量用完就停止充电
        if (creative) {
            int stored = station.energyStorage.getEnergyStored();
            int maxCap = station.energyStorage.getMaxEnergyStored();
            if (stored < maxCap) {
                station.energyStorage.receiveEnergy(maxCap - stored, false);
            }
        }

        // 1. 给内部机器人充电
        if (station.hasRobot) {
            station.chargeTicks++;
            int currentEnergy = station.storedRobot.getInt("Energy");
            int maxEnergy = station.getRobotMaxEnergy();

            if (currentEnergy < maxEnergy) {
                // 创造站：秒充满；普通站：按 CHARGE_RATE 充电，且受充电站剩余电量限制
                int transfer;
                if (creative) {
                    transfer = maxEnergy - currentEnergy;
                } else {
                    transfer = Math.min(CHARGE_RATE, maxEnergy - currentEnergy);
                    transfer = Math.min(transfer, station.energyStorage.getEnergyStored());
                }
                if (transfer > 0) {
                    station.storedRobot.putInt("Energy", currentEnergy + transfer);
                    if (!creative) {
                        station.energyStorage.extractEnergy(transfer, false);
                    }
                    station.setChanged();
                }
            } else if (station.storedRobot.getBoolean("AutoEject") && station.chargeTicks >= MIN_STAY_TICKS) {
                station.ejectRobot();
            }
        }

        // 2. 给充电槽中的物品充电
        ItemStack stack = station.itemHandler.getStackInSlot(0);
        if (!stack.isEmpty()) {
            var cap = stack.getCapability(ForgeCapabilities.ENERGY);
            cap.ifPresent(energy -> {
                int amount = creative ? energy.getMaxEnergyStored() : Math.min(CHARGE_RATE, station.energyStorage.getEnergyStored());
                int received = energy.receiveEnergy(amount, false);
                if (received > 0) {
                    if (!creative) {
                        station.energyStorage.extractEnergy(received, false);
                    }
                    station.setChanged();
                }
            });
        }
    }

    /**
     * 将充满电的机器人释放回世界。
     */
    public void ejectRobot() {
        if (!hasRobot || level == null || level.isClientSide) return;

        int maxEnergy = getRobotMaxEnergy();

        storedRobot.putInt("Energy", maxEnergy);
        storedRobot.putInt("RobotState", SearchRobotEntity.RobotState.WORKING.id);

        SearchRobotEntity robot = ModEntities.SEARCH_ROBOT.get().create(level);
        if (robot != null) {
            robot.load(storedRobot);
            robot.setPos(
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 1.0,
                    worldPosition.getZ() + 0.5
            );
            level.addFreshEntity(robot);
        }

        this.storedRobot = new CompoundTag();
        this.hasRobot = false;
        markUpdated();
    }

    /**
     * 尝试将机器人实体存入充电站。
     * @return true 存入成功；false 站内已有机器人。
     */
    public boolean insertRobot(SearchRobotEntity robot) {
        if (hasRobot) return false;
        CompoundTag robotTag = new CompoundTag();
        robot.save(robotTag);
        robotTag.putBoolean("AutoEject", true); // 自动返航存入的机器人，充满后自动释放
        this.storedRobot = robotTag;
        this.hasRobot = true;
        this.chargeTicks = 0;
        robot.discard();
        markUpdated();
        return true;
    }

    /**
     * 玩家手动将物品形态机器人放入充电站。
     * @return true 放入成功。
     */
    public boolean insertRobotFromItem(ItemStack robotItem) {
        if (hasRobot) return false;
        CompoundTag robotTag = robotItem.getOrCreateTag();
        if (!robotTag.contains("Energy")) {
            robotTag.putInt("Energy", 0);
        }
        robotTag.putBoolean("AutoEject", false); // 手动放入的机器人，充满后不自弹，需玩家空手右键取出
        this.storedRobot = robotTag;
        this.hasRobot = true;
        this.chargeTicks = 0;
        markUpdated();
        return true;
    }

    /**
     * 将可充电物品放入充电槽。
     * @return true 放入成功。
     */
    public boolean insertItemToCharge(ItemStack stack) {
        if (hasRobot) return false;
        if (!itemHandler.getStackInSlot(0).isEmpty()) return false;
        if (!stack.getCapability(ForgeCapabilities.ENERGY).isPresent()) return false;
        itemHandler.setStackInSlot(0, stack.copy());
        setChanged();
        return true;
    }

    /**
     * 从充电槽取出物品。
     * @return 取出的物品，若为空则返回 EMPTY。
     */
    public ItemStack extractChargedItem() {
        ItemStack stack = itemHandler.getStackInSlot(0);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        itemHandler.setStackInSlot(0, ItemStack.EMPTY);
        setChanged();
        return stack;
    }

    /**
     * 将站内机器人作为物品取出（交给玩家，不生成实体）。
     * @return 取出的机器人物品，若站内无机器人返回 EMPTY。
     */
    public ItemStack extractRobotAsItem() {
        if (!hasRobot) return ItemStack.EMPTY;
        CompoundTag tag = storedRobot.copy();
        tag.remove("AutoEject"); // 内部标记，物品形态不需要
        ItemStack robotItem = new ItemStack(ModItems.ROBOT_ITEM.get());
        robotItem.setTag(tag);
        this.storedRobot = new CompoundTag();
        this.hasRobot = false;
        markUpdated();
        return robotItem;
    }

    public boolean hasItemInChargeSlot() {
        return !itemHandler.getStackInSlot(0).isEmpty();
    }

    /**
     * 充电站被破坏时吐出机器人和物品。
     */
    public void onStationDestroyed() {
        if (level == null || level.isClientSide) return;

        // 吐出机器人
        if (hasRobot) {
            SearchRobotEntity robot = ModEntities.SEARCH_ROBOT.get().create(level);
            if (robot != null) {
                robot.load(storedRobot);
                robot.setPos(
                        worldPosition.getX() + 0.5,
                        worldPosition.getY() + 0.5,
                        worldPosition.getZ() + 0.5
                );
                level.addFreshEntity(robot);
            }
            this.storedRobot = new CompoundTag();
            this.hasRobot = false;
        }

        // 掉落充电槽物品
        drops();
    }

    public boolean hasRobot() {
        return hasRobot;
    }

    /** 站内机器人的最大电量；无 stats 标签或数值非法时回退到 10000。 */
    private int getRobotMaxEnergy() {
        int maxEnergy = 10000;
        if (storedRobot.contains("stats")) {
            maxEnergy = storedRobot.getCompound("stats").getInt("max_energy");
        }
        return maxEnergy > 0 ? maxEnergy : 10000;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        if (cap == ForgeCapabilities.ENERGY) {
            return lazyEnergyHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
        lazyEnergyHandler = LazyOptional.of(() -> energyStorage);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
        lazyEnergyHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("inventory", itemHandler.serializeNBT());
        tag.putInt("energy", energyStorage.getEnergyStored());
        tag.putBoolean("HasRobot", hasRobot);
        if (hasRobot) {
            tag.put("StoredRobot", storedRobot);
        }
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        energyStorage.receiveEnergy(tag.getInt("energy"), false);
        this.hasRobot = tag.getBoolean("HasRobot");
        if (hasRobot && tag.contains("StoredRobot")) {
            this.storedRobot = tag.getCompound("StoredRobot");
        }
    }

    // 站内机器人状态变化要同步给客户端，否则客户端 hasRobot 不更新、站上方不渲染机器人提示模型
    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    /** 标记脏并通知客户端刷新（hasRobot / 充电状态变化后调用）。 */
    private void markUpdated() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }
}
