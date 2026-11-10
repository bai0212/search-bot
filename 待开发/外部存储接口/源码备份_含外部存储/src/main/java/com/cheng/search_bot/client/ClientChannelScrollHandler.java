package com.cheng.search_bot.client;

import com.cheng.search_bot.SearchBotMod;
import com.cheng.search_bot.item.custom.RobotControllerItem;
import com.cheng.search_bot.network.ControllerActionPacket;
import com.cheng.search_bot.network.ModNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 手持操控台时 shift + 滚轮切换当前频道（客户端 → 服务端发动作包）。
 */
@Mod.EventBusSubscriber(modid = SearchBotMod.MOD_ID, value = Dist.CLIENT)
public class ClientChannelScrollHandler {

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!mc.player.isShiftKeyDown()) return;
        if (mc.screen != null) return; // 打开界面时不抢滚轮

        ItemStack held = RobotControllerItem.findController(mc.player);
        if (held.isEmpty()) return;

        double delta = event.getScrollDelta();
        int dir = delta < 0 ? 1 : -1; // 向下滚 = 下一频道
        ModNetworking.CHANNEL.sendToServer(
                new ControllerActionPacket(ControllerActionPacket.SWITCH_CHANNEL, dir, null));
        event.setCanceled(true);
    }
}
