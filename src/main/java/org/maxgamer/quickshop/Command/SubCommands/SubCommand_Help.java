package org.maxgamer.quickshop.Command.SubCommands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.*;
import org.maxgamer.quickshop.Command.CommandContainer;
import org.maxgamer.quickshop.Command.CommandProcesser;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.Util.MsgUtil;

public class SubCommand_Help implements CommandProcesser {
    private QuickShop plugin = QuickShop.instance;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] cmdArg) {
        sendHelp(sender, commandLabel);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] cmdArg) {
        return new ArrayList<>();
    }

    private void sendHelp(@NotNull CommandSender s, @NotNull String commandLabel) {
        s.sendMessage(MsgUtil.getMessage("command.description.title"));
        for (CommandContainer container : plugin.getCommandManager().getCmds()) {
            if (container.getPermission() == null || container.getPermission().isEmpty() || s.hasPermission(container
                    .getPermission())) {
                if (!MsgUtil.getMessage("command.description." + container.getPrefix()).equals(MsgUtil.invaildMsg))
                    s.sendMessage(ChatColor.GREEN + "/" + commandLabel + " " + container
                            .getPrefix() + ChatColor.YELLOW + " - "
                            + MsgUtil.getMessage("command.description." + container.getPrefix()));
            }
        }
    }
}