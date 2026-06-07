package me.tuanzi.warp;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class TeleportManager {
    private static final Map<UUID, TeleportTask> pendingTeleports = new HashMap<>();
    private static final Map<UUID, LocationRecord> previousLocations = new HashMap<>();

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<Map.Entry<UUID, TeleportTask>> it = pendingTeleports.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, TeleportTask> entry = it.next();
                TeleportTask task = entry.getValue();
                ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());

                if (player == null || !player.isAlive()) {
                    it.remove();
                    continue;
                }

                if (player.position().distanceToSqr(task.initialPos) > 0.01 || player.getHealth() < task.initialHealth) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c传送被打断！(检测到移动或受伤)"));
                    it.remove();
                    continue;
                }

                task.ticksLeft--;
                if (task.ticksLeft <= 0) {
                    executeTeleport(player, task.target);
                    it.remove();
                }
            }
        });
    }

    public static void requestTeleport(ServerPlayer player, LocationRecord target) {
        if (pendingTeleports.containsKey(player.getUUID())) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c你已经有一个正在进行的传送请求！"));
            return;
        }

        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a传送将在 3 秒后开始，请保持站立且不要受到伤害..."));
        pendingTeleports.put(player.getUUID(), new TeleportTask(player, target, 60)); // 3 seconds = 60 ticks
    }

    private static void executeTeleport(ServerPlayer player, LocationRecord target) {
        // 记录返回位置
        previousLocations.put(player.getUUID(), new LocationRecord(
                player.level().dimension(),
                player.position(),
                player.getYRot(),
                player.getXRot()
        ));

        ServerLevel targetLevel = player.level().getServer().getLevel(target.dimension);
        if (targetLevel != null) {
            player.teleportTo(targetLevel, target.pos.x(), target.pos.y(), target.pos.z(), java.util.Collections.emptySet(), target.yaw, target.pitch, true);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a传送成功！"));
        } else {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c目标世界不存在！"));
        }
    }

    public static void requestWarpBack(ServerPlayer player) {
        LocationRecord prev = previousLocations.get(player.getUUID());
        if (prev != null) {
            // 返回上一个位置，但不覆盖当前的 previousLocation（或者让它循环？）
            // 这里我们选择直接传送回去，也可以像普通传送一样等3秒。
            // 需求说 "使用/warp后,使用/warpback传送回原来的位置", 意味着这也是一种传送。
            requestTeleport(player, prev);
        } else {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c没有可返回的记录！"));
        }
    }

    public static class TeleportTask {
        public final Vec3 initialPos;
        public final float initialHealth;
        public final LocationRecord target;
        public int ticksLeft;

        public TeleportTask(ServerPlayer player, LocationRecord target, int ticks) {
            this.initialPos = player.position();
            this.initialHealth = player.getHealth();
            this.target = target;
            this.ticksLeft = ticks;
        }
    }
}
