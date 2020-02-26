package org.maxgamer.quickshop.permission;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import cc.bukkit.shop.permission.PermissionProvider;
import cc.bukkit.shop.permission.ProviderType;
import net.milkbowl.vault.permission.Permission;

public class VaultPermsProvider implements PermissionProvider {
    private net.milkbowl.vault.permission.Permission provider;
    
    public VaultPermsProvider() {
        RegisteredServiceProvider<Permission> permissionProvider = Bukkit.getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        
        if (permissionProvider != null)
            provider = permissionProvider.getProvider();
    }
    
    @Override
    public boolean hasPermission(@NotNull CommandSender sender, @NotNull String permission) {
        return provider.has(sender, permission);
    }
    
    @Override
    @NotNull
    public ProviderType getType() {
        return ProviderType.VAULT;
    }
}
