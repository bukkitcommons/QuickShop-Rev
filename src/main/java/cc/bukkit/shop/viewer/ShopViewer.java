package cc.bukkit.shop.viewer;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.utils.Util;
import cc.bukkit.shop.Shop;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ShopViewer {
  @Nullable
  private final Shop shop;
  
  public static ShopViewer of(@Nullable Shop shop) {
    return new ShopViewer(shop);
  }
  
  public static ShopViewer empty() {
    return new ShopViewer(null);
  }
  
  private boolean fails;
  
  public ShopViewer reset() {
    Util.debug("Viewer reset, before: " + fails);
    fails = false;
    return this;
  }
  
  public ShopViewer nonNull() {
    if (shop == null)
      Util.debug("Shop not found, before: " + fails);
    
    fails = shop == null;
    return this;
  }
  
  public ShopViewer accept(Consumer<Shop> consumer) {
    Util.debug("Accept, fails: " + fails);
    
    if (!fails) consumer.accept(shop);
    return this;
  }
  
  public boolean test(Predicate<Shop> predicate, boolean def) {
    Util.debug("Test, fails: " + fails);
    
    return fails ? def : !predicate.test(shop);
  }
  
  public <R> R apply(Function<Shop, R> function, R def) {
    Util.debug("Apply, fails: " + fails);
    
    return fails ? def : function.apply(shop);
  }
  
  public ShopViewer filter(Predicate<Shop> predicate) {
    boolean before = fails;
    fails = fails ? true : !predicate.test(shop);
    
    Util.debug("Filter, fails: " + fails + ", before: " + fails);
    return this;
  }
  
  public boolean isEmpty() {
    return shop == null;
  }
  
  public ShopViewer ifPresent(Consumer<Shop> consumer) {
    if (!fails && shop != null)
      consumer.accept(shop);
    return this;
  }
  
  public ShopViewer ifPresent(Runnable runnable) {
    if (!fails && shop != null)
      runnable.run();
    return this;
  }
  
  public boolean isPresent() {
    return shop != null;
  }

  public Shop get() {
    return shop;
  }
}
