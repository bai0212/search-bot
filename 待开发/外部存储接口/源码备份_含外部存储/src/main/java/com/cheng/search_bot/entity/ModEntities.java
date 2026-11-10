package com.cheng.search_bot.entity;

import com.cheng.search_bot.SearchBotMod;
import com.cheng.search_bot.entity.custom.SearchRobotEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, SearchBotMod.MOD_ID);

    public static final RegistryObject<EntityType<SearchRobotEntity>> SEARCH_ROBOT =
            ENTITY_TYPES.register("search_robot", () ->
                    EntityType.Builder.<SearchRobotEntity>of(SearchRobotEntity::new, MobCategory.MISC)
                            .sized(0.8f, 0.8f)
                            .build("search_robot"));
}
