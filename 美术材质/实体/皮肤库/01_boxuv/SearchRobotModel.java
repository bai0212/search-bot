package com.cheng.search_bot.client.model;

import com.cheng.search_bot.SearchBotMod;
import com.cheng.search_bot.entity.custom.SearchRobotEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 搜索机器人模型（由 D:\Blockbench\mcp\apply_01.py 从 01.bbmodel + Blockbench 导出改造，勿手改）。
 * 几何就是用户自己在 Blockbench 里建的 01，UV 岛由 apply_01.py 按 MC 的 box-UV 公式重排。
 * 坐标走原版约定：模型空间 y 向下、脚底在 y=24，渲染端要做 scale(-1,-1,1) + translate(0,-1.501,0)。
 */
@OnlyIn(Dist.CLIENT)
public class SearchRobotModel extends EntityModel<SearchRobotEntity> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(SearchBotMod.MOD_ID, "search_robot"), "main");

    public final ModelPart head;
    public final ModelPart body;
    public final ModelPart arm;
    public final ModelPart hand;
    public final ModelPart armLeft;
    public final ModelPart handLeft;

    public SearchRobotModel(ModelPart root) {
        this.head = root.getChild("head");
        this.body = root.getChild("boody");
        this.arm = root.getChild("arm");
        this.hand = root.getChild("hand");
        this.armLeft = root.getChild("arm_left");
        this.handLeft = root.getChild("hand_left");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(89, 0).addBox(-3.0F, -19.0F, -3.0F, 6.0F, 4.0F, 6.0F, new CubeDeformation(0.0F))
        .texOffs(7, 33).addBox(-2.5F, -18.5F, -3.4F, 5.0F, 0.5F, 0.4F, new CubeDeformation(0.0F))
        .texOffs(101, 33).addBox(-2.5F, -16.0F, -3.4F, 5.0F, 0.5F, 0.4F, new CubeDeformation(0.0F))
        .texOffs(86, 20).addBox(2.0F, -18.0F, -3.4F, 0.5F, 2.0F, 0.4F, new CubeDeformation(0.0F))
        .texOffs(126, 26).addBox(-2.5F, -18.0F, -3.4F, 0.5F, 2.0F, 0.4F, new CubeDeformation(0.0F))
        .texOffs(91, 32).addBox(-2.0F, -18.0F, -3.2F, 4.0F, 2.0F, 0.2F, new CubeDeformation(0.0F))
        .texOffs(53, 20).addBox(-1.75F, -19.5F, -1.75F, 3.5F, 1.5F, 3.5F, new CubeDeformation(0.0F))
        .texOffs(31, 20).addBox(2.5F, -18.25F, -2.0F, 1.0F, 2.5F, 4.0F, new CubeDeformation(0.0F))
        .texOffs(42, 20).addBox(-3.5F, -18.25F, -2.0F, 1.0F, 2.5F, 4.0F, new CubeDeformation(0.0F))
        .texOffs(11, 21).addBox(-2.25F, -18.0F, 1.75F, 4.5F, 3.0F, 1.5F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 27.05F, 0.0F));
        PartDefinition boody = partdefinition.addOrReplaceChild("boody", CubeListBuilder.create().texOffs(31, 0).addBox(-3.5F, -14.0F, -3.5F, 7.0F, 5.0F, 7.0F, new CubeDeformation(0.0F))
        .texOffs(89, 17).addBox(-2.0F, -9.5F, -3.0F, 4.0F, 2.0F, 6.0F, new CubeDeformation(0.0F))
        .texOffs(114, 0).addBox(2.0F, -9.5F, -2.5F, 1.0F, 2.0F, 5.0F, new CubeDeformation(0.0F))
        .texOffs(0, 0).addBox(-3.75F, -7.75F, -3.75F, 7.5F, 4.2F, 7.5F, new CubeDeformation(0.0F))
        .texOffs(60, 0).addBox(-3.0F, -5.75F, -4.0F, 6.0F, 2.7F, 8.0F, new CubeDeformation(0.0F))
        .texOffs(114, 13).addBox(-3.0F, -9.5F, -2.5F, 1.0F, 2.0F, 5.0F, new CubeDeformation(0.0F))
        .texOffs(11, 28).addBox(-0.75F, -15.55F, -0.75F, 1.5F, 1.0F, 1.5F, new CubeDeformation(0.0F))
        .texOffs(110, 26).addBox(-1.25F, -14.55F, -1.25F, 2.5F, 1.0F, 2.5F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 27.05F, 0.0F));
        PartDefinition arm = partdefinition.addOrReplaceChild("arm", CubeListBuilder.create().texOffs(18, 28).addBox(-4.5F, -13.0F, -0.5F, 1.5F, 1.25F, 1.25F, new CubeDeformation(0.0F))
        .texOffs(68, 20).addBox(-6.25F, -13.75F, -1.5F, 2.0F, 2.75F, 3.0F, new CubeDeformation(0.0F))
        .texOffs(79, 20).addBox(-6.0F, -11.0F, -0.75F, 1.5F, 2.5F, 1.5F, new CubeDeformation(0.0F))
        .texOffs(121, 26).addBox(-5.75F, -8.0F, -0.5F, 1.0F, 2.5F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 27.05F, 0.0F));
        PartDefinition cube_r1 = arm.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(86, 24).addBox(-0.25F, -0.25F, -0.25F, 0.5F, 0.5F, 0.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.25F, -5.25F, 0.0F, 0.8727F, 0.0F, 0.0F));
        PartDefinition cube_r2 = arm.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(84, 27).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.25F, -8.25F, 0.0F, 0.8727F, 0.0F, 0.0F));
        PartDefinition hand = partdefinition.addOrReplaceChild("hand", CubeListBuilder.create(), PartPose.offsetAndRotation(-5.3F, 23.2F, -0.3F, -1.5708F, -1.0036F, 1.5708F));
        PartDefinition cube_r3 = hand.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(110, 17).addBox(-0.25F, -1.25F, -0.4F, 0.5F, 0.5F, 0.8F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.2F, -0.4F, 0.0F, 0.0F, 0.0F, -0.6109F));
        PartDefinition finger_1 = hand.addOrReplaceChild("finger_1", CubeListBuilder.create().texOffs(53, 30).addBox(-0.25F, -0.75F, -0.65F, 1.5F, 0.5F, 1.3F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -2.6616F));
        PartDefinition cube_r4 = finger_1.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(60, 30).addBox(-0.75F, -1.25F, -0.65F, 1.5F, 0.5F, 1.3F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.5708F));
        PartDefinition finger_2 = hand.addOrReplaceChild("finger_2", CubeListBuilder.create().texOffs(67, 30).addBox(-0.75F, -1.25F, -0.65F, 1.5F, 0.5F, 1.3F, new CubeDeformation(0.0F)), PartPose.offset(0.5F, -0.25F, 0.0F));
        PartDefinition cube_r5 = finger_2.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(0, 31).addBox(-0.75F, -1.25F, -0.65F, 1.5F, 0.5F, 1.3F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.5F, -0.5F, 0.0F, 0.0F, 0.0F, 1.5708F));
        PartDefinition arm_left = partdefinition.addOrReplaceChild("arm_left", CubeListBuilder.create().texOffs(25, 32).addBox(3.0F, -13.0F, -0.5F, 1.5F, 1.25F, 1.25F, new CubeDeformation(0.0F))
        .texOffs(0, 21).addBox(4.25F, -13.75F, -1.5F, 2.0F, 2.75F, 3.0F, new CubeDeformation(0.0F))
        .texOffs(24, 21).addBox(4.5F, -11.0F, -0.75F, 1.5F, 2.5F, 1.5F, new CubeDeformation(0.0F))
        .texOffs(79, 27).addBox(4.75F, -8.0F, -0.5F, 1.0F, 2.5F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 27.05F, 0.0F));
        PartDefinition cube_L_r1 = arm_left.addOrReplaceChild("cube_L_r1", CubeListBuilder.create().texOffs(74, 30).addBox(-0.25F, -0.25F, -0.25F, 0.5F, 0.5F, 0.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.25F, -5.25F, 0.0F, 0.8727F, 0.0F, 0.0F));
        PartDefinition cube_L_r2 = arm_left.addOrReplaceChild("cube_L_r2", CubeListBuilder.create().texOffs(25, 28).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.25F, -8.25F, 0.0F, 0.8727F, 0.0F, 0.0F));
        PartDefinition hand_left = partdefinition.addOrReplaceChild("hand_left", CubeListBuilder.create(), PartPose.offsetAndRotation(5.3F, 23.2F, -0.3F, -1.5708F, 1.0036F, -1.5708F));
        PartDefinition cube_L_r3 = hand_left.addOrReplaceChild("cube_L_r3", CubeListBuilder.create().texOffs(110, 21).addBox(-0.25F, -1.25F, -0.4F, 0.5F, 0.5F, 0.8F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.2F, -0.4F, 0.0F, 0.0F, 0.0F, 0.6109F));
        PartDefinition finger_1_left = hand_left.addOrReplaceChild("finger_1_left", CubeListBuilder.create().texOffs(32, 32).addBox(-1.25F, -0.75F, -0.65F, 1.5F, 0.5F, 1.3F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 2.6616F));
        PartDefinition cube_L_r4 = finger_1_left.addOrReplaceChild("cube_L_r4", CubeListBuilder.create().texOffs(39, 32).addBox(-0.75F, -1.25F, -0.65F, 1.5F, 0.5F, 1.3F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.5708F));
        PartDefinition finger_2_left = hand_left.addOrReplaceChild("finger_2_left", CubeListBuilder.create().texOffs(46, 32).addBox(-0.75F, -1.25F, -0.65F, 1.5F, 0.5F, 1.3F, new CubeDeformation(0.0F)), PartPose.offset(-0.5F, -0.25F, 0.0F));
        PartDefinition cube_L_r5 = finger_2_left.addOrReplaceChild("cube_L_r5", CubeListBuilder.create().texOffs(84, 32).addBox(-0.75F, -1.25F, -0.65F, 1.5F, 0.5F, 1.3F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.5F, -0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

        return LayerDefinition.create(meshdefinition, 128, 64);
    }

    @Override
    public void setupAnim(SearchRobotEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        float rad = (float) Math.PI / 180.0f;
        this.head.yRot = Mth.clamp(netHeadYaw, -45.0f, 45.0f) * rad;
        this.head.xRot = Mth.clamp(headPitch, -30.0f, 30.0f) * rad;
        // 上身跟着头转一点点，整体不会像块铁坨
        this.body.yRot = Mth.clamp(netHeadYaw * 0.35f, -20.0f, 20.0f) * rad;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha) {
        head.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        body.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        arm.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        hand.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        armLeft.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        handLeft.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
