package com.cheng.search_bot.compat;

import com.cheng.search_bot.client.screen.RobotInventoryScreen;
import com.cheng.search_bot.menu.custom.RobotInventoryMenu;
import com.cheng.search_bot.network.ModNetworking;
import com.cheng.search_bot.network.RobotFilterPacket;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI 兼容插件：让机器人仓库里的「标记物品」过滤槽支持 <b>JEI 拖动标记</b>。
 *
 * <p>当玩家在 JEI 界面里按住一个物品、拖到过滤槽上松手时，JEI 会调用这里返回的
 * {@link Target#accept(Object)}，把该物品作为幽灵标记写入对应过滤槽（不消耗物品）。
 * 行为与「手上有物品时点击过滤槽」完全一致——都走 {@link RobotFilterPacket}。
 *
 * <p>本类只在装了 JEI 时才会被 JEI 扫描加载；没装 JEI 时模组不受任何影响。
 */
@JeiPlugin
public class SearchBotJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID = new ResourceLocation("search_bot", "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(RobotInventoryScreen.class, new FilterSlotGhostHandler());
    }

    /**
     * 把 JEI 里拖出来的物品标记进右侧「标记物品」面板的幽灵槽。
     * 只有当前选中「过滤卡」分页时这些槽位才可见/可标记。
     */
    private static final class FilterSlotGhostHandler implements IGhostIngredientHandler<RobotInventoryScreen> {

        @Override
        public <I> List<Target<I>> getTargetsTyped(RobotInventoryScreen gui, ITypedIngredient<I> ingredient, boolean doStart) {
            // 过滤槽只接受物品标记，流体等其它 JEI 类型一律忽略
            ItemStack item = ingredient.getItemStack().orElse(ItemStack.EMPTY);
            if (item.isEmpty()) {
                return List.of();
            }

            RobotInventoryMenu menu = gui.getMenu();
            List<Target<I>> targets = new ArrayList<>();
            for (Slot slot : menu.slots) {
                if (!(slot instanceof RobotInventoryMenu.FilterSlot filterSlot)) continue;
                if (!filterSlot.isActive()) continue; // 只有选中「过滤卡」分页时槽位才可标记

                int x = gui.getGuiLeft() + filterSlot.x;
                int y = gui.getGuiTop() + filterSlot.y;
                Rect2i area = new Rect2i(x, y, 16, 16);
                int handlerIndex = filterSlot.getSlotIndex();

                targets.add(new Target<I>() {
                    @Override
                    public Rect2i getArea() {
                        return area;
                    }

                    @Override
                    public void accept(I ingredient) {
                        if (ingredient instanceof ItemStack stack && !stack.isEmpty()) {
                            ModNetworking.CHANNEL.sendToServer(new RobotFilterPacket(
                                    RobotFilterPacket.ACTION_SET_SLOT, handlerIndex, stack));
                        }
                    }
                });
            }
            return targets;
        }

        @Override
        public void onComplete() {
            // 拖动结束后无需额外清理
        }

        @Override
        public boolean shouldHighlightTargets() {
            return true;
        }
    }
}
