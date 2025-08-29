package com.example.ccb;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class CCBPlugin extends JavaPlugin {
    private SneakListener sneakListener;

    @Override
    public void onEnable() {
        // 保存默认配置
        saveDefaultConfig();
        
        // 初始化潜行监听器
        sneakListener = new SneakListener(this);
        
        // 注册事件监听器
        getServer().getPluginManager().registerEvents(sneakListener, this);
        
        getLogger().info("CCB plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("CCB plugin has been disabled!");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        // 处理重载命令
        if (cmd.getName().equalsIgnoreCase("ccbreload")) {
            // 检查是否为管理员
            if (!sender.hasPermission("ccb.reload") && !(sender instanceof Player && ((Player) sender).isOp())) {
                sender.sendMessage("§c你没有权限执行此命令!");
                return true;
            }
            
            // 重载配置
            reloadConfig();
            
            // 重新初始化监听器
            getServer().getPluginManager().registerEvents(new SneakListener(this), this);
            
            sender.sendMessage("§aCCB插件已成功重载!");
            getLogger().info("CCB plugin reloaded by " + sender.getName());
            return true;
        }
        return false;
    }
}
