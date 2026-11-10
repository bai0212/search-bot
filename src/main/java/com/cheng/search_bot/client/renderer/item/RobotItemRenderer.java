package com.cheng.search_bot.client.renderer.item;

import com.cheng.search_bot.SearchBotMod;
import com.cheng.search_bot.client.model.SearchRobotModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 机器人物品的 3D 渲染器（客户端专用）。
 * 让物品形态的机器人像「精妙背包」一样，在物品栏 / 手中直接显示 3D 模型，而不是 2D 图标。
 * 通过 {@link com.cheng.search_bot.item.custom.RobotItem#initializeClient} 挂到物品上。
 */
@OnlyIn(Dist.CLIENT)
public class RobotItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(SearchBotMod.MOD_ID, "textures/entity/search_robot.png");

    /** 物品栏里机器人的整体缩放（机器人本体约 1 格高，0.6 让它差不多占满物品格）。 */
    private static final float SCALE = 0.6f;

    /** 机器人朝向（度）：180 = 面向相机；若看到的是背面改 0，看到侧面改 90。 */
    private static final float Y_ROT = 180.0f;

    /** 机器人翻正后（脚底→头顶）的近似高度，单位「格」。用于把机器人垂直居中到物品格中心。 */
    private static final float ROBOT_HEIGHT = 1.03f;

    private static RobotItemRenderer instance;

    private final SearchRobotModel model;

    private RobotItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet entityModelSet) {
        super(dispatcher, entityModelSet);
        this.model = new SearchRobotModel(entityModelSet.bakeLayer(SearchRobotModel.LAYER_LOCATION));
    }

    /** 单例：延迟到客户端初始化完成后才创建（需要 Minecraft 实例来取模型集）。 */
    public static RobotItemRenderer get() {
        if (instance == null) {
            Minecraft mc = Minecraft.getInstance();
            instance = new RobotItemRenderer(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
        }
        return instance;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource buffer, int packedLight, int packedOverlay) {
        poseStack.pushPose();

        // 模型是 Blockbench 原版约定（脚底 y=24、y 轴朝下）。
        // 物品渲染器传入的坐标系：原点在「方块的一角」(0,0,0)，方块占据 [0,1]，之后会再 -0.5 把它居中到格子。
        // 所以这里把机器人当作一个方块摆：先翻正 + 脚底归零（同实体渲染），再缩放，
        // 最后平移到方块中心 (0.5, 0.5, 0.5) 并下移半身高，让机器人在物品格里水平、垂直都居中。
        poseStack.translate(0.5f, 0.5f - SCALE * ROBOT_HEIGHT * 0.5f, 0.5f);
        poseStack.mulPose(Axis.YP.rotationDegrees(Y_ROT));
        poseStack.scale(SCALE, SCALE, SCALE);
        SearchRobotModel.applyModelCorrection(poseStack);

        this.model.setupAnim(null, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
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
