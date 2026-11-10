package com.cheng.search_bot.client;

import com.cheng.search_bot.SearchBotMod;
import com.cheng.search_bot.entity.custom.SearchRobotEntity;
import com.cheng.search_bot.item.ModItems;
import com.cheng.search_bot.item.custom.RobotControllerItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 手持操控台时，在世界中渲染标记方框：
 * <ul>
 *   <li>运输起点（蓝色）= 天蓝色方框</li>
 *   <li>运输终点（橙色）= 橙色方框</li>
 *   <li>收集目标（绿色）= 绿色方框</li>
 *   <li>已绑定机器人（当前频道）= 白色方框</li>
 * </ul>
 *
 * <p>标记数据存在操控台 NBT 里（{@link RobotControllerItem#syncMarkers} 写入，
 * 服务端设置/取消标记后自动同步到客户端），渲染时直接读手持操控台的 NBT。
 * 已绑定机器人 id 同样从操控台 NBT 的当前频道 {@code RobotIds} 读，匹配附近实体画白框。
 */
@Mod.EventBusSubscriber(modid = SearchBotMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientMarkerRenderer {

    // 颜色：天蓝 / 橙 / 绿 / 白（已绑定机器人）
    private static final float[] START_COLOR = {0.35f, 0.75f, 1.0f};
    private static final float[] END_COLOR = {1.0f, 0.55f, 0.15f};
    private static final float[] COLLECT_COLOR = {0.2f, 0.9f, 0.3f};
    private static final float[] BOUND_COLOR = {1.0f, 1.0f, 1.0f};

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;

        ItemStack held = player.getMainHandItem();
        if (!held.is(ModItems.CONTROLLER.get())) return;

        List<BlockPos> starts = RobotControllerItem.getMarkerStarts(held);
        List<BlockPos> ends = RobotControllerItem.getMarkerEnds(held);
        BlockPos collect = RobotControllerItem.getMarkerCollect(held);
        List<UUID> boundIds = RobotControllerItem.getCurrentChannelRobotIds(held);

        if (starts.isEmpty() && ends.isEmpty() && collect == null && boundIds.isEmpty()) return;

        PoseStack pose = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        VertexConsumer consumer = buffer.getBuffer(RenderType.lines());

        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);

        for (BlockPos p : starts) {
            drawBox(pose, consumer, p, START_COLOR);
        }
        for (BlockPos p : ends) {
            drawBox(pose, consumer, p, END_COLOR);
        }
        if (collect != null) {
            drawBox(pose, consumer, collect, COLLECT_COLOR);
        }

        // 白色方框：框住当前频道绑定的机器人（用操控台 NBT 里的 RobotIds 精确匹配）
        if (!boundIds.isEmpty()) {
            Set<UUID> idSet = new HashSet<>(boundIds);
            AABB search = new AABB(player.blockPosition()).inflate(64.0);
            for (SearchRobotEntity robot : mc.level.getEntitiesOfClass(SearchRobotEntity.class, search)) {
                if (idSet.contains(robot.getRobotId())) {
                    // 框住机器人实体本体：直接画真实碰撞箱（等同 F3+B 看到的命中框），不再外扩
                    AABB box = robot.getBoundingBox();
                    LevelRenderer.renderLineBox(pose, consumer, box,
                            BOUND_COLOR[0], BOUND_COLOR[1], BOUND_COLOR[2], 1.0f);
                }
            }
        }

        pose.popPose();
        buffer.endBatch(RenderType.lines());
    }

    private static void drawBox(PoseStack pose, VertexConsumer consumer, BlockPos pos, float[] color) {
        AABB box = new AABB(
                pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0);
        LevelRenderer.renderLineBox(pose, consumer, box, color[0], color[1], color[2], 1.0f);
    }
}
