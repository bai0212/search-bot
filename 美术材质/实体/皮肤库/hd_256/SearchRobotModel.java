package com.cheng.search_bot.client.model;

import com.cheng.search_bot.SearchBotMod;
import com.cheng.search_bot.entity.custom.SearchRobotEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 搜索机器人模型（由 D:\Blockbench\mcp\gen_java_hd.py 从 search_robot_hd_manifest.json 生成，勿手改）。
 * 32 单位 = 1 格，脚底在 y=0，渲染时由 SearchRobotRenderer 缩到 0.5。
 */
@OnlyIn(Dist.CLIENT)
public class SearchRobotModel extends EntityModel<SearchRobotEntity> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(SearchBotMod.MOD_ID, "search_robot"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart upperBody;

    public SearchRobotModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.upperBody = root.getChild("body").getChild("upper_body");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition root = meshdefinition.getRoot();
        PartDefinition part_head = root.addOrReplaceChild(
                "head",
                CubeListBuilder.create()
                    .texOffs(179, 36).addBox(-6f, 0f, -6f, 12f, 8f, 12f)
                    .texOffs(203, 69).addBox(-5f, 1f, -7f, 10f, 6f, 2f)
                    .texOffs(236, 19).addBox(5f, 2f, -3f, 3f, 4f, 6f)
                    .texOffs(71, 72).addBox(-4f, 1f, 6f, 8f, 5f, 2f)
                    .texOffs(27, 58).addBox(-3f, 7f, -3f, 6f, 3f, 6f)
                    .texOffs(46, 74).addBox(-1f, 9f, -1f, 2f, 3f, 2f)
                    .texOffs(81, 39).addBox(-8f, 2f, -3f, 3f, 4f, 6f),
                PartPose.offset(0f, 23f, 0f)
        );
        PartDefinition part_boody = root.addOrReplaceChild(
                "body",
                CubeListBuilder.create(),
                PartPose.offset(0f, 0f, 0f)
        );
        PartDefinition part_upper_body = part_boody.addOrReplaceChild(
                "upper_body",
                CubeListBuilder.create()
                    .texOffs(65, 0).addBox(-7f, 0f, -7f, 14f, 10f, 14f)
                    .texOffs(178, 69).addBox(-5f, 2f, -8f, 10f, 6f, 2f)
                    .texOffs(27, 39).addBox(-4f, 9f, -4f, 8f, 2f, 8f)
                    .texOffs(228, 62).addBox(-4f, 2f, 6f, 8f, 6f, 4f)
                    .texOffs(81, 56).addBox(-3f, 9f, -3f, 6f, 3f, 6f),
                PartPose.offset(0f, 12f, 0f)
        );
        PartDefinition part_arm = part_upper_body.addOrReplaceChild(
                "arm",
                CubeListBuilder.create()
                    .texOffs(106, 55).addBox(-4f, -4f, -2f, 3f, 4f, 4f)
                    .texOffs(236, 0).addBox(-2f, -5f, -3f, 4f, 6f, 6f)
                    .texOffs(163, 38).addBox(-1f, -9f, -2f, 3f, 5f, 4f),
                PartPose.offset(10f, 9f, 0f)
        );
        PartDefinition part_hand = part_upper_body.addOrReplaceChild(
                "hand",
                CubeListBuilder.create()
                    .texOffs(0, 65).addBox(-2f, 2f, -2f, 3f, 4f, 4f)
                    .texOffs(163, 66).addBox(-2f, -1f, -2f, 3f, 4f, 4f)
                    .texOffs(71, 58).addBox(-1f, -5f, -1f, 2f, 5f, 2f)
                    .texOffs(100, 39).addBox(-2f, -7f, -3f, 3f, 3f, 6f)
                    .texOffs(151, 68).addBox(-2f, -8f, -4f, 3f, 3f, 2f)
                    .texOffs(92, 72).addBox(-2f, -8f, 2f, 3f, 3f, 2f),
                PartPose.offset(11f, -4f, 0f)
        );
        PartDefinition part_arm_left = part_upper_body.addOrReplaceChild(
                "arm_left",
                CubeListBuilder.create()
                    .texOffs(106, 68).addBox(1f, -4f, -2f, 3f, 4f, 4f)
                    .texOffs(60, 39).addBox(-2f, -5f, -3f, 4f, 6f, 6f)
                    .texOffs(163, 52).addBox(-2f, -9f, -2f, 3f, 5f, 4f),
                PartPose.offset(-10f, 9f, 0f)
        );
        PartDefinition part_hand_left = part_upper_body.addOrReplaceChild(
                "hand_left",
                CubeListBuilder.create()
                    .texOffs(121, 68).addBox(-1f, 2f, -2f, 3f, 4f, 4f)
                    .texOffs(136, 68).addBox(-1f, -1f, -2f, 3f, 4f, 4f)
                    .texOffs(15, 65).addBox(-1f, -5f, -1f, 2f, 5f, 2f)
                    .texOffs(52, 58).addBox(-1f, -7f, -3f, 3f, 3f, 6f)
                    .texOffs(24, 74).addBox(-1f, -8f, -4f, 3f, 3f, 2f)
                    .texOffs(35, 74).addBox(-1f, -8f, 2f, 3f, 3f, 2f),
                PartPose.offset(-11f, -4f, 0f)
        );
        PartDefinition part_lower_body = part_boody.addOrReplaceChild(
                "lower_body",
                CubeListBuilder.create()
                    .texOffs(122, 0).addBox(-6f, 0f, -8f, 12f, 5f, 16f)
                    .texOffs(0, 0).addBox(-7f, 1f, -9f, 14f, 2f, 18f)
                    .texOffs(179, 0).addBox(-7f, 2f, -7f, 14f, 7f, 14f)
                    .texOffs(122, 38).addBox(-4f, 8f, -6f, 8f, 5f, 12f)
                    .texOffs(228, 36).addBox(3f, 7f, -5f, 3f, 5f, 10f)
                    .texOffs(0, 39).addBox(-6f, 7f, -5f, 3f, 5f, 10f),
                PartPose.offset(0f, 0f, 0f)
        );
        return LayerDefinition.create(meshdefinition, 256, 256);
    }

    @Override
    public void setupAnim(SearchRobotEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float rad = (float) (Math.PI / 180.0);
        this.head.yRot = Math.max(-45f, Math.min(45f, netHeadYaw)) * rad;
        this.head.xRot = -Math.max(-30f, Math.min(30f, headPitch)) * rad;
        this.upperBody.yRot = Math.max(-30f, Math.min(30f, netHeadYaw)) * rad;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        root.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
