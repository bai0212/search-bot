package com.cheng.search_bot.block.entity.custom;

import com.cheng.search_bot.block.entity.ModBlockEntities;
import com.cheng.search_bot.entity.custom.SearchRobotEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 信号控制器方块实体。
 *
 * <p>配合红石卡使用：方块接收红石信号，把「当前是否通电」实时推送给绑定到的机器人
 * （按机器人稳定 {@code RobotId} 匹配，跨拆解/组装依然有效）。机器人侧由红石卡决定
 * 通电/断电时是否暂停工作。
 */
public class SignalControllerBlockEntity extends BlockEntity {
    @Nullable
    private UUID boundRobotUuid = null;

    public SignalControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SIGNAL_CONTROLLER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SignalControllerBlockEntity be) {
        if (level.isClientSide) return;
        be.pushSignalToRobot(level);
    }

    /** 把当前红石电平推送给绑定机器人。 */
    private void pushSignalToRobot(Level level) {
        boolean powered = level.hasNeighborSignal(this.worldPosition);
        if (this.boundRobotUuid == null) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        Entity e = serverLevel.getEntity(this.boundRobotUuid);
        // 注意：绑定的是机器人稳定 RobotId，而实体自带的 UUID 是另一套，这里用 RobotId 精确匹配。
        // 直接 getEntity 拿不到，改在附近搜索带该 RobotId 的机器人（与操控台查找同一套逻辑）。
        if (e instanceof SearchRobotEntity robot) {
            robot.setRedstonePowered(powered);
            return;
        }

        // 兜底：实体 UUID 与 RobotId 不同，按包围盒搜索
        var box = new net.minecraft.world.phys.AABB(this.worldPosition).inflate(64);
        for (SearchRobotEntity robot : level.getEntitiesOfClass(SearchRobotEntity.class, box)) {
            if (this.boundRobotUuid.equals(robot.getRobotId())) {
                robot.setRedstonePowered(powered);
                return;
            }
        }
    }

    public void setBoundRobot(@Nullable UUID uuid) {
        this.boundRobotUuid = uuid;
        setChanged();
    }

    @Nullable
    public UUID getBoundRobotUuid() {
        return boundRobotUuid;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (boundRobotUuid != null) {
            tag.putUUID("BoundRobot", boundRobotUuid);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID("BoundRobot")) {
            this.boundRobotUuid = tag.getUUID("BoundRobot");
        } else {
            this.boundRobotUuid = null;
        }
    }
}
