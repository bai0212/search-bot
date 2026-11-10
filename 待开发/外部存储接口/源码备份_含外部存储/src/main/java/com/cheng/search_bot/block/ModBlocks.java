package com.cheng.search_bot.block;

import com.cheng.search_bot.SearchBotMod;
import com.cheng.search_bot.block.custom.ChargingStationBlock;
import com.cheng.search_bot.block.custom.CreativeChargingStationBlock;
import com.cheng.search_bot.block.custom.RobotAssemblerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, SearchBotMod.MOD_ID);

    public static final RegistryObject<Block> ROBOT_ASSEMBLER = BLOCKS.register("robot_assembler",
            () -> new RobotAssemblerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(2.5f, 6.0f)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> CHARGING_STATION = BLOCKS.register("charging_station",
            () -> new ChargingStationBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(2.5f, 6.0f)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> CREATIVE_CHARGING_STATION = BLOCKS.register("creative_charging_station",
            () -> new CreativeChargingStationBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(2.5f, 6.0f)
                    .requiresCorrectToolForDrops()));
}
