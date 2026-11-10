package com.cheng.search_bot.item.custom;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 电池零件
 * 默认电量为 0，支持 Forge Energy (FE) 充放电。
 * 不同材料等级的电池拥有不同容量上限，容量从 RobotStats 查表获取。
 */
public class BatteryItem extends Item {
    public BatteryItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
        int energy = stack.getOrCreateTag().getInt("energy");
        int capacity = getCapacity(stack);
        tooltipComponents.add(Component.literal("能量: " + energy + " / " + capacity + " FE"));
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        stack.getOrCreateTag().putInt("energy", 0);
        return stack;
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new BatteryEnergyProvider(stack);
    }

    /** 根据物品 Registry ID 查表获取电池容量 */
    public static int getCapacity(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (tag.contains("capacity")) {
            return tag.getInt("capacity");
        }
        // 首次使用：查表并缓存到 NBT
        var itemKey = ForgeRegistries.ITEMS.getKey(stack.getItem());
        String itemId = itemKey != null ? itemKey.getPath() : "";
        String material = com.cheng.search_bot.util.RobotStats.extractMaterial(itemId);
        int capacity = com.cheng.search_bot.util.RobotStats.getBatteryCapacity(material);
        tag.putInt("capacity", capacity);
        return capacity;
    }

    /**
     * 为电池物品提供 FE 能量存储能力
     */
    public static class BatteryEnergyProvider implements ICapabilityProvider {
        private final ItemStack stack;
        private final LazyOptional<IEnergyStorage> energyOptional;

        public BatteryEnergyProvider(ItemStack stack) {
            this.stack = stack;
            this.energyOptional = LazyOptional.of(this::createEnergyStorage);
        }

        private IEnergyStorage createEnergyStorage() {
            return new IEnergyStorage() {
                private static final int MAX_TRANSFER = 1000;

                private int getCapacity() {
                    return BatteryItem.getCapacity(stack);
                }

                private int getEnergy() {
                    return stack.getOrCreateTag().getInt("energy");
                }

                private void setEnergy(int energy) {
                    stack.getOrCreateTag().putInt("energy", energy);
                }

                @Override
                public int receiveEnergy(int maxReceive, boolean simulate) {
                    int energy = getEnergy();
                    int capacity = getCapacity();
                    int received = Math.min(capacity - energy, Math.min(MAX_TRANSFER, maxReceive));
                    if (!simulate) {
                        setEnergy(energy + received);
                    }
                    return received;
                }

                @Override
                public int extractEnergy(int maxExtract, boolean simulate) {
                    int energy = getEnergy();
                    int extracted = Math.min(energy, Math.min(MAX_TRANSFER, maxExtract));
                    if (!simulate) {
                        setEnergy(energy - extracted);
                    }
                    return extracted;
                }

                @Override
                public int getEnergyStored() {
                    return getEnergy();
                }

                @Override
                public int getMaxEnergyStored() {
                    return getCapacity();
                }

                @Override
                public boolean canExtract() {
                    return true;
                }

                @Override
                public boolean canReceive() {
                    return true;
                }
            };
        }

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            if (cap == ForgeCapabilities.ENERGY) {
                return energyOptional.cast();
            }
            return LazyOptional.empty();
        }
    }
}
