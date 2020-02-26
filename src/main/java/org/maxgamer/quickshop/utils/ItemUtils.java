package org.maxgamer.quickshop.utils;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.configuration.BaseConfig;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import cc.bukkit.shop.BasicShop;
import cc.bukkit.shop.ChestShop;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.hologram.DisplayAttribute;
import cc.bukkit.shop.hologram.DisplayInfo;
import cc.bukkit.shop.logger.ShopLogger;
import cc.bukkit.shop.viewer.ShopViewer;
import lombok.SneakyThrows;

public class ItemUtils {
    public final static Gson GSON = new Gson();
    
    public static boolean isDisplayItem(@Nullable ItemStack itemStack) {
        return isDisplayItem(itemStack, null);
    }
    
    @SneakyThrows
    public static void fixesDisplayItem(@Nullable Item item) {
        ItemStack itemStack = item.getItemStack();
        if (itemStack == null || !itemStack.hasItemMeta())
            return;
        
        ItemMeta iMeta = itemStack.getItemMeta();
        if (!iMeta.hasLore())
            return;
        
        for (String lore : iMeta.getLore()) {
            try {
                if (!lore.startsWith("{")) {
                    continue;
                }
                DisplayInfo shopProtectionFlag = GSON.fromJson(lore, DisplayInfo.class);
                if (shopProtectionFlag == null)
                    continue;
                
                if (shopProtectionFlag.getShopLocationData() != null) {
                    ShopViewer viewer = Shop.getManager().getLoadedShopAt(BlockUtils.deserializeLocation(shopProtectionFlag.getShopLocationData()));
                    
                    viewer.ifPresent((BasicShop shop) -> {
                        if (!(shop instanceof ChestShop))
                            return;
                        
                        ChestShop chest = (ChestShop) shop;
                        if (chest.data().get(DisplayAttribute.LOCATION, chest.location().bukkit()).distance(item.getLocation()) > 0.6) {
                            item.remove();
                            Util.trace("Removed a duped item display entity by distance > 0.6");
                            return;
                        }
                        
                        if (chest.display().stack() == null)
                            return;
                        
                        if (!chest.display().<Entity>stack().getUniqueId().equals(item.getUniqueId())) {
                            item.remove();
                            Util.trace("Removed a duped item display entity by uuid not equals");
                            return;
                        }
                        
                        if (chest.display().<Entity>sample().getType() != item.getType()) {
                            item.remove();
                            Util.trace("Removed a duped item display entity by type not equals");
                            return;
                        }
                    });
                } else
                    if (shopProtectionFlag.getShopItemStackData() != null) {
                        ItemStack displayItem = ItemUtils.deserialize(shopProtectionFlag.getShopItemStackData());
                        
                        if (!QuickShop.instance().getItemMatcher().matches(itemStack, displayItem)) {
                            item.remove();
                            Util.trace("Removed a duped item display entity by not matches");
                        }
                    }
            } catch (JsonSyntaxException e) {
                return;
            }
        }
    }
    
    /**
     * Check the itemStack is contains protect flag.
     *
     * @param itemStack Target ItemStack
     * @return Contains protect flag.
     */
    public static boolean isDisplayItem(@Nullable ItemStack itemStack, @Nullable ChestShop shop) {
        if (itemStack == null || !itemStack.hasItemMeta())
            return false;
        
        ItemMeta iMeta = itemStack.getItemMeta();
        if (!iMeta.hasLore())
            return false;
        
        String defaultMark = DisplayInfo.defaultMark();
        for (String lore : iMeta.getLore()) {
            try {
                if (!lore.startsWith("{")) {
                    continue;
                }
                DisplayInfo shopProtectionFlag = GSON.fromJson(lore, DisplayInfo.class);
                if (shopProtectionFlag == null) {
                    continue;
                }
                if (shop == null && defaultMark.equals(shopProtectionFlag.getMark())) {
                    return true;
                }
                if (shopProtectionFlag.getShopLocationData() != null) {
                    return shop == null ? true : shopProtectionFlag.getShopLocationData().equals(shop.location().toString());
                }
                if (shop == null && shopProtectionFlag.getShopItemStackData() != null) {
                    return true;
                }
            } catch (JsonSyntaxException e) {
                // Ignore
            }
        }
        
        return false;
    }
    
    /**
     * Create a new itemStack with protect flag.
     *
     * @param itemStack Old itemStack
     * @param shop      The shop
     * @return New itemStack with protect flag.
     */
    public static ItemStack createGuardItemStack(@NotNull ItemStack itemStack, @NotNull ChestShop shop) {
        itemStack = new ItemStack(itemStack);
        itemStack.setAmount(1);
        
        ItemMeta meta = itemStack.getItemMeta();
        if (BaseConfig.displayNameVisible) {
            if (meta.hasDisplayName())
                meta.setDisplayName(meta.getDisplayName());
            else
                meta.setDisplayName(ItemUtils.getItemStackName(itemStack));
        } else {
            meta.setDisplayName("");
        }
        
        DisplayInfo shopProtectionFlag = DisplayInfo.from(itemStack, shop);
        meta.setLore(Collections.singletonList(GSON.toJson(shopProtectionFlag)));
        
        itemStack.setItemMeta(meta);
        return itemStack;
    }
    
    /**
     * Covert YAML string to ItemStack.
     *
     * @param config serialized ItemStack
     * @return ItemStack iStack
     * @throws InvalidConfigurationException when failed deserialize config
     */
    @SuppressWarnings({ "deprecation", "unchecked" })
    @Nullable
    public static ItemStack deserialize(@NotNull String config) throws InvalidConfigurationException {
        DumperOptions yamlOptions = new DumperOptions();
        yamlOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        yamlOptions.setIndent(2);
        Yaml yaml = new Yaml(yamlOptions);
        YamlConfiguration yamlConfiguration = new YamlConfiguration();
        Map<Object, Object> root = yaml.load(config);
        // noinspection unchecked
        Map<String, Object> item = (Map<String, Object>) root.get("item");
        try {
            Object version = item.get("v");
            
            try {
                int itemDataVersion = version == null ? Bukkit.getUnsafe().getDataVersion() : Integer.parseInt(String.valueOf(version));
                // Try load the itemDataVersion to do some checks.
                // noinspection deprecation
                if (itemDataVersion > Bukkit.getUnsafe().getDataVersion()) {
                    Util.trace("WARNING: DataVersion not matched with ItemStack: " + config);
                    // okay we need some things to do
                    if (BaseConfig.forceLoadDowngradeItems) {
                        // okay it enabled
                        Util.trace("QuickShop is trying force loading " + config);
                        if (BaseConfig.forceLoadDowngradeItemsMethod == 0) { // Mode 0
                            // noinspection deprecation
                            item.put("v", Bukkit.getUnsafe().getDataVersion() - 1);
                        } else { // Mode other
                            // noinspection deprecation
                            item.put("v", Bukkit.getUnsafe().getDataVersion());
                        }
                        // Okay we have hacked the dataVersion, now put it back
                        root.put("item", item);
                        config = yaml.dump(root);
                        
                        Util.trace("Updated, we will try load as hacked ItemStack: " + config);
                    } else {
                        ShopLogger.instance().warning("Cannot load ItemStack " + config + " because it saved from higher Minecraft server version, the action will fail and you will receive a exception, PLELASE DON'T REPORT TO QUICKSHOP!");
                        ShopLogger.instance().warning("You can try force load this ItemStack by our hacked ItemStack read util(shop.force-load-downgrade-items), but beware, the data may damaged if you load on this lower Minecraft server version, Please backup your world and database before enable!");
                    }
                }
            } catch (NoSuchMethodError e) {
                ; // Legacy versions (1.12.2), getDataVersion
            }
            
            yamlConfiguration.loadFromString(config);
            return yamlConfiguration.getItemStack("item");
        } catch (Exception e) {
            e.printStackTrace();
            yamlConfiguration.loadFromString(config);
            return yamlConfiguration.getItemStack("item");
        }
    }
    
    public static String getItemStackName(@NotNull ItemStack itemStack) {
        if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName())
            return itemStack.getItemMeta().getDisplayName();
        
        return Shop.getLocaleManager().get(itemStack.getType());
    }
    
    /**
     * Gets the percentage (Without trailing %) damage on a tool.
     *
     * @param item The ItemStack of tools to check
     * @return The percentage 'health' the tool has. (Opposite of total damage)
     */
    public static String getToolPercentage(@NotNull ItemStack item) {
        if (!(item.getItemMeta() instanceof Damageable)) {
            Util.trace(item.getType().name() + " not Damageable.");
            return "Error: NaN";
        }
        double dura = ((Damageable) item.getItemMeta()).getDamage();
        double max = item.getType().getMaxDurability();
        DecimalFormat formatter = new DecimalFormat("0");
        return formatter.format((1 - dura / max) * 100.0);
    }
    
    /**
     * Get a material is a dye
     *
     * @param material The material
     * @return yes or not
     */
    public static boolean isDyes(@Nullable Material material) {
        return material.name().endsWith("_DYE");
    }
    
    /**
     * Call this to check items in inventory and remove it.
     *
     * @param inv inv
     */
    public static void inventoryCheck(@Nullable Inventory inv) {
        if (inv == null) {
            return;
        }
        if (inv.getHolder() == null) {
            Util.trace("Skipped plugin gui inventory check.");
            return;
        }
        try {
            for (int i = 0; i < inv.getSize(); i++) {
                ItemStack itemStack = inv.getItem(i);
                if (itemStack == null) {
                    continue;
                }
                if (isDisplayItem(itemStack, null)) {
                    // Found Item and remove it.
                    Location location = inv.getLocation();
                    if (location == null) {
                        return; // Virtual GUI
                    }
                    inv.setItem(i, new ItemStack(Material.AIR));
                    Util.trace("Found a displayitem in an inventory, Scheduling to removal...");
                    Shop.getLocaleManager().sendGlobalAlert("[InventoryCheck] Found displayItem in inventory at " + location + ", Item is " + itemStack.getType().name());
                }
            }
        } catch (Throwable t) {
            // Ignore
        }
    }
    
    /**
     * @param mat The material to check
     * @return Returns true if the item is a tool (Has durability) or false if it
     *         doesn't.
     */
    public static boolean hasDurability(@NotNull Material mat) {
        return !(mat.getMaxDurability() == 0);
    }
}
