package org.maxgamer.quickshop.command.sub;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.command.QuickShopCommand;
import org.maxgamer.quickshop.utils.Util;
import cc.bukkit.paste.Paste;
import cc.bukkit.shop.Shop;

public class CommandPaste extends QuickShopCommand {
    @Override
    public List<String> permissions() {
        return Collections.singletonList("quickshop.paste");
    }
    
    @Override
    public void onCommand(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] cmdArg) {
        // do actions
        new BukkitRunnable() {
            @Override
            public void run() {
                sender.sendMessage("Please wait, we're uploading the data to the pastebin...");
                final Paste paste = new Paste((QuickShop) Shop.instance());
                final String pasteText = paste.genNewPaste();
                String pasteResult = paste.paste(pasteText);
                if (pasteResult != null) {
                    sender.sendMessage(pasteResult);
                } else {
                    sender.sendMessage("The paste failed, saving the paste at local location...");
                    File file = new File(Shop.instance().getDataFolder(), "paste");
                    file.mkdirs();
                    file = new File(file, "paste-" + UUID.randomUUID().toString().replaceAll("-", "") + ".txt");
                    try {
                        final boolean createResult = file.createNewFile();
                        Util.debug("Create paste file: " + file.getCanonicalPath() + " " + createResult);
                        final FileWriter fwriter = new FileWriter(file);
                        fwriter.write(pasteText);
                        fwriter.flush();
                        fwriter.close();
                        sender.sendMessage("Paste was saved to your server at: " + file.getAbsolutePath());
                    } catch (IOException e) {
                        ((QuickShop) Shop.instance()).getSentryErrorReporter().ignoreThrow();
                        e.printStackTrace();
                        sender.sendMessage("Saving failed, output to console...");
                        Shop.instance().getLogger().info(pasteText);
                    }
                }
            }
        }.runTaskAsynchronously(Shop.instance());
    }
}
