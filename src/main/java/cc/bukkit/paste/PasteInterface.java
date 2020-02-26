package cc.bukkit.paste;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface PasteInterface {
    String pasteTheText(@NotNull String text) throws Exception;
}
