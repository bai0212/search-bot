package com.cheng.search_bot.client.renderer.block;

import com.cheng.search_bot.SearchBotMod;
import com.cheng.search_bot.block.entity.custom.ChargingStationBlockEntity;
import com.cheng.search_bot.client.model.SearchRobotModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 充电站渲染器（客户端专用）
 *
 * 站内有机器人时，在充电站顶部渲染一个缩小、缓慢自转的机器人模型作为提示。
 * 注意：充电站方块本体是不透明的 cube_all，机器人若真的画在方块内部会被挡住，
 * 所以这里把它画在方块上方（也正是 ejectRobot 释放机器人的位置），让玩家能直观
 * 看到「这台充电站里存着一台机器人」。
 */
@OnlyIn(Dist.CLIENT)
public class ChargingStationRenderer implements BlockEntityRenderer<ChargingStationBlockEntity> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(SearchBotMod.MOD_ID, "textures/entity/search_robot.png");

    // 站内机器人缩小后的边长（格）；模型原为 16 单位（1 格）立方体
    private static final float ROBOT_SIZE = 0.3f;

    private final SearchRobotModel model;

    public ChargingStationRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new SearchRobotModel(context.bakeLayer(SearchRobotModel.LAYER_LOCATION));
    }

    @Override
    public void render(ChargingStationBlockEntity station, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (!station.hasRobot()) {
            return;
        }

        Level level = station.getLevel();
        float gameTime = level != null ? level.getGameTime() + partialTick : 0.0f;
        // 轻微上下浮动，让提示更明显
        float bob = (float) Math.sin(gameTime * 0.05f) * 0.05f;

        poseStack.pushPose();

        // 移到方块中心上方（机器人被释放的位置）
        poseStack.translate(0.5f, 1.15f + bob, 0.5f);
        // 缓慢自转
        poseStack.mulPose(Axis.YP.rotationDegrees(gameTime * 3.0f));
        // 01 模型走原版约定（y 向下、脚底 y=24），先缩小再做模型校正（翻转 + 抬脚底），
        // 让缩放后的机器人脚底正好落在上面 translate 的点上
        poseStack.scale(ROBOT_SIZE, ROBOT_SIZE, ROBOT_SIZE);
        SearchRobotModel.applyModelCorrection(poseStack);

        this.model.setupAnim(null, 0.0f, 0.0f, gameTime, 0.0f, 0.0f);
        this.model.renderToBuffer(
                poseStack,
                buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)),
                packedLight,
                packedOverlay,
                1.0f, 1.0f, 1.0f, 1.0f
        );

        poseStack.popPose();
    }
}
