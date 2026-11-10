package com.cheng.search_bot.energy;

import net.minecraftforge.energy.EnergyStorage;

public class CustomEnergyStorage extends EnergyStorage {
    public CustomEnergyStorage(int capacity, int maxReceive, int maxExtract) {
        super(capacity, maxReceive, maxExtract);
    }

    public void setEnergy(int energy) {
        this.energy = Math.max(0, Math.min(capacity, energy));
    }

    public void addEnergy(int energy) {
        this.energy = Math.max(0, Math.min(capacity, this.energy + energy));
    }

    public void setCapacity(int newCapacity) {
        this.capacity = newCapacity;
        this.energy = Math.min(this.energy, newCapacity);
    }

    public int getMaxEnergyStored() {
        return this.capacity;
    }
}
