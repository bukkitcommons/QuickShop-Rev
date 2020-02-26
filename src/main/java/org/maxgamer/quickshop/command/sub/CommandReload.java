package org.maxgamer.quickshop.command.sub;

import java.util.Collections;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.command.QuickShopCommand;
import cc.bukkit.shop.Shop;

public class CommandReload extends QuickShopCommand {
    private final static List<String> PERMS = Collections.singletonList("quickshop.reload");
    
    @Override
    public List<String> permissions() {
        return PERMS;
    }
    
    @Override
    public void onCommand(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] cmdArg) {
        sender.sendMessage(Shop.getLocaleManager().get("command.reloading"));
        QuickShop.instance().reloadPlugin();
    }
}
