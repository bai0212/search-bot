package com.cheng.search_bot.item.custom;

import com.cheng.search_bot.client.renderer.item.RobotItemRenderer;
import com.cheng.search_bot.entity.ModEntities;
import com.cheng.search_bot.entity.custom.SearchRobotEntity;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * 机器人物品
 * - 右键空气 / 远处：在玩家前方放置机器人
 * - 右键方块：由 CommonEventHandler 拦截处理（支持容器识别）
 */
public class RobotItem extends Item {
    public RobotItem(Properties properties) {
        super(properties);
    }

    /**
     * 物品名后追加当前模式（与操控台一致，模式用专属颜色），模式切换提示直接在名称处体现。
     * 后续相关物品（插件卡、信号控制器等）的提示都遵循此约定。
     */
    @Override
    public Component getName(ItemStack stack) {
        Component base = super.getName(stack);
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("Mode")) {
            SearchRobotEntity.RobotMode mode = SearchRobotEntity.RobotMode.byId(tag.getInt("Mode"));
            return base.copy().append(Component.literal(" · " + mode.cnName())
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(mode.color() & 0xFFFFFF))));
        }
        return base;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        // 让物品形态的机器人直接渲染 3D 模型（像精妙背包），而不是 2D 图标
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return RobotItemRenderer.get();
            }
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("Energy")) {
            int energy = tag.getInt("Energy");
            int max = 10000;
            if (tag.contains("stats")) {
                max = tag.getCompound("stats").getInt("max_energy");
            }
            if (max <= 0) max = 10000;
            tooltipComponents.add(Component.literal("电量: " + energy + " / " + max + " FE"));
        } else {
            tooltipComponents.add(Component.literal("电量: 0 / 10000 FE"));
        }
    }

    /**
     * 右键空气时触发：在玩家准星所指位置（或前方）放置机器人
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) return InteractionResultHolder.success(stack);

        // 射线检测，找出玩家看向的方块位置
        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 lookVec = player.getViewVector(1.0f);
        Vec3 endPos = eyePos.add(lookVec.x * 4.0, lookVec.y * 4.0, lookVec.z * 4.0);
        BlockHitResult hitResult = level.clip(new ClipContext(eyePos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

        // 生成位置：如果看向远处无方块，则放在前方；否则放在方块上方
        Vec3 spawnVec;
        if (hitResult.getType() == BlockHitResult.Type.MISS) {
            spawnVec = endPos;
        } else {
            spawnVec = hitResult.getLocation().add(0, 1, 0);
        }

        SearchRobotEntity entity = ModEntities.SEARCH_ROBOT.get().create(level);
        if (entity != null) {
            entity.moveTo(spawnVec.x, spawnVec.y, spawnVec.z, player.getYRot(), 0.0f);

            // 先加载物品 NBT（无 Energy 时默认满电、无 Mode 时默认跟随），
            // 再设主人，避免旧 Owner 标签覆盖当前放置者。
            CompoundTag tag = stack.getTag();
            entity.loadRobotData(tag != null ? tag : new CompoundTag());
            entity.setOwner(player.getUUID());

            entity.determineModeAndRegisterGoals(entity.blockPosition(), true);
            level.addFreshEntity(entity);
            stack.shrink(1);
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.fail(stack);
    }

    /**
     * 右键方块时触发：由 CommonEventHandler 拦截处理，这里不再重复放置
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        // 方块点击统一由 CommonEventHandler.onRightClickBlock 处理
        // 返回 PASS 让事件系统接管
        return InteractionResult.PASS;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains("parts");
    }
}
