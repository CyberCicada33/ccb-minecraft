package com.example.ccb;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.util.Vector;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class SneakListener implements Listener {
    private final CCBPlugin plugin;
    private final Map<UUID, Long> lastSneakTime = new HashMap<>();
    private final Map<UUID, Integer> sneakCount = new HashMap<>();
    private final Random random = new Random();

    public SneakListener(CCBPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return; // 只处理开始潜行的动作

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        long lastTime = lastSneakTime.getOrDefault(playerId, 0L);
        int count = sneakCount.getOrDefault(playerId, 0);

        // 更新潜行计数
        if (currentTime - lastTime < 1000) {
            count++;
        } else {
            count = 1; // 超过1秒重置计数
        }

        lastSneakTime.put(playerId, currentTime);
        sneakCount.put(playerId, count);

        // 检查CPS是否大于4
        if (count >= 4) {
            handleSneakEffect(player);
            // 重置计数以避免连续触发
            sneakCount.put(playerId, 0);
        }
    }

    private void handleSneakEffect(Player player) {
        Location eyeLoc = player.getEyeLocation();
        // 获取玩家前方5格内的实体
        Entity target = getTargetEntity(player, 5.0);

        if (target != null) {
            // 对目标造成1点伤害
            if (target instanceof LivingEntity livingTarget) {
                // 使用兼容的方式创建伤害源（不设置攻击者）
                DamageSource damageSource = DamageSource.builder(DamageType.PLAYER_ATTACK).build();
                livingTarget.damage(1.0, damageSource);

                // 在玩家和目标之间生成放射状白色粒子
                spawnParticles(player.getLocation(), target.getLocation());

                // 如果目标是玩家，处理额外逻辑
                if (target instanceof Player targetPlayer) {
                    handlePlayerTarget(player, targetPlayer);
                }
            }
        }
    }

    private void handlePlayerTarget(Player attacker, Player target) {
        // 7%概率生成小猫
        if (random.nextDouble() <= 0.07) {
            Location midpoint = getMidpoint(attacker.getLocation(), target.getLocation());
            target.getWorld().spawnEntity(midpoint, EntityType.CAT);
            target.getWorld().playSound(midpoint, Sound.ENTITY_CAT_AMBIENT, 1.0f, 1.0f);
        }

        // 检查目标是否因这次伤害死亡
        if (target.getHealth() <= 1.0) {
            String deathMessage = plugin.getConfig().getString("death-message",
                    "%attacker% 悄悄地从背后击败了 %victim%");
            deathMessage = deathMessage.replace("%attacker%", attacker.getName())
                    .replace("%victim%", target.getName());

            // 使用现代Adventure API发送广播消息
            Bukkit.getServer().broadcast(Component.text(deathMessage).color(NamedTextColor.RED));
        }
    }

    private Entity getTargetEntity(Player player, double maxDistance) {
        Location loc = player.getEyeLocation();
        for (Entity entity : player.getNearbyEntities(maxDistance, maxDistance, maxDistance)) {
            if (entity instanceof LivingEntity &&
                    !(entity instanceof Player && ((Player) entity).getGameMode() == GameMode.CREATIVE)) {
                if (isFacingEntity(player, entity, maxDistance)) {
                    return entity;
                }
            }
        }
        return null;
    }

    private boolean isFacingEntity(Player player, Entity entity, double maxDistance) {
        Location eyeLoc = player.getEyeLocation();
        Vector toEntity = entity.getLocation().subtract(eyeLoc).toVector();
        return eyeLoc.getDirection().dot(toEntity.normalize()) > 0.85 && // 视角前方
                eyeLoc.distance(entity.getLocation()) <= maxDistance;
    }

    private void spawnParticles(Location loc1, Location loc2) {
        World world = loc1.getWorld();
        if (world == null) return;

        Location midpoint = getMidpoint(loc1, loc2);
        Vector direction = loc2.subtract(loc1).toVector().normalize();

        // 生成放射状粒子
        for (int i = 0; i < 50; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = random.nextDouble() * loc1.distance(loc2);

            double x = midpoint.getX() + Math.cos(angle) * 0.5;
            double y = midpoint.getY() + random.nextDouble() * 1.5;
            double z = midpoint.getZ() + Math.sin(angle) * 0.5;

            Location particleLoc = new Location(world, x, y, z);
            world.spawnParticle(Particle.WHITE_ASH, particleLoc, 1, 0, 0, 0, 0);
        }
    }

    // 实现中点计算方法
    private Location getMidpoint(Location loc1, Location loc2) {
        double x = (loc1.getX() + loc2.getX()) / 2;
        double y = (loc1.getY() + loc2.getY()) / 2;
        double z = (loc1.getZ() + loc2.getZ()) / 2;
        return new Location(loc1.getWorld(), x, y, z);
    }
}
    