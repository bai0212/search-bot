package com.cheng.search_bot.menu;

import com.cheng.search_bot.SearchBotMod;
import com.cheng.search_bot.menu.custom.RobotAssemblerMenu;
import com.cheng.search_bot.menu.custom.RobotControllerMenu;
import com.cheng.search_bot.menu.custom.RobotInventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, SearchBotMod.MOD_ID);

    public static final RegistryObject<MenuType<RobotAssemblerMenu>> ROBOT_ASSEMBLER =
            MENUS.register("robot_assembler", () ->
                    IForgeMenuType.create(RobotAssemblerMenu::new));

    public static final RegistryObject<MenuType<RobotInventoryMenu>> ROBOT_INVENTORY =
            MENUS.register("robot_inventory", () ->
                    IForgeMenuType.create(RobotInventoryMenu::new));

    public static final RegistryObject<MenuType<RobotControllerMenu>> ROBOT_CONTROLLER =
            MENUS.register("robot_controller", () ->
                    IForgeMenuType.create((id, inv, data) -> new RobotControllerMenu(id, inv)));
}
