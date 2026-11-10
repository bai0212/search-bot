package com.cheng.search_bot.block.entity;

import com.cheng.search_bot.SearchBotMod;
import com.cheng.search_bot.block.ModBlocks;
import com.cheng.search_bot.block.entity.custom.ChargingStationBlockEntity;
import com.cheng.search_bot.block.entity.custom.RobotAssemblerBlockEntity;
import com.cheng.search_bot.block.entity.custom.SignalControllerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, SearchBotMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<RobotAssemblerBlockEntity>> ROBOT_ASSEMBLER =
            BLOCK_ENTITIES.register("robot_assembler", () ->
                    BlockEntityType.Builder.of(RobotAssemblerBlockEntity::new,
                            ModBlocks.ROBOT_ASSEMBLER.get()).build(null));

    public static final RegistryObject<BlockEntityType<ChargingStationBlockEntity>> CHARGING_STATION =
            BLOCK_ENTITIES.register("charging_station", () ->
                    BlockEntityType.Builder.of(ChargingStationBlockEntity::new,
                            ModBlocks.CHARGING_STATION.get(),
                            ModBlocks.CREATIVE_CHARGING_STATION.get()).build(null));

    public static final RegistryObject<BlockEntityType<SignalControllerBlockEntity>> SIGNAL_CONTROLLER =
            BLOCK_ENTITIES.register("signal_controller", () ->
                    BlockEntityType.Builder.of(SignalControllerBlockEntity::new,
                            ModBlocks.SIGNAL_CONTROLLER.get()).build(null));
}
