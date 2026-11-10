package com.cheng.search_bot.client.renderer.entity;

import com.cheng.search_bot.SearchBotMod;
import com.cheng.search_bot.client.model.SearchRobotModel;
import com.cheng.search_bot.entity.custom.SearchRobotEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 搜索机器人的实体渲染器（客户端专用）
 * 模型用的是 Blockbench 里那版 01（16 单位 = 1 格，机器人约 1 格高）
 */
@OnlyIn(Dist.CLIENT)
public class SearchRobotRenderer extends EntityRenderer<SearchRobotEntity> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(SearchBotMod.MOD_ID, "textures/entity/search_robot.png");

    private final SearchRobotModel model;

    public SearchRobotRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.4f;
        this.model = new SearchRobotModel(context.bakeLayer(SearchRobotModel.LAYER_LOCATION));
    }

    @Override
    public ResourceLocation getTextureLocation(SearchRobotEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(SearchRobotEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        // 01 模型是 Blockbench 按原版约定导出的：模型空间 y 轴朝下、脚底在 y=24，
        // 所以要跟原版活体渲染器一样先翻转再抬 1.501，不然就是倒着长
        float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - bodyYaw));
        poseStack.scale(-1.0f, -1.0f, 1.0f);
        poseStack.translate(0.0f, -1.501f, 0.0f);

        // 头部相对身体的转角：原版就是 headYaw - bodyYaw，翻转之后 xRot 也不用再取负
        float headYaw = Mth.rotLerp(partialTick, entity.yHeadRotO, entity.yHeadRot);
        float netHeadYaw = headYaw - bodyYaw;
        float headPitch = entity.getViewXRot(partialTick);

        this.model.setupAnim(entity, 0.0f, 0.0f, entity.tickCount + partialTick, netHeadYaw, headPitch);
        this.model.renderToBuffer(
                poseStack,
                buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)),
                packedLight,
                OverlayTexture.NO_OVERLAY,
                1.0f, 1.0f, 1.0f, 1.0f
        );

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }
}
