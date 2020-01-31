package org.maxgamer.quickshop.utils;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.shop.Shop;
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
  
  public ShopViewer nonNull() {
    fails = shop == null;
    return this;
  }
  
  public ShopViewer accept(Consumer<Shop> consumer) {
    consumer.accept(shop);
    return this;
  }
  
  public boolean test(Predicate<Shop> predicate) {
    return predicate.test(shop);
  }
  
  public <R> R apply(Function<Shop, R> function) {
    return function.apply(shop);
  }
  
  public ShopViewer filter(Predicate<Shop> predicate) {
    fails = predicate.test(shop);
    return this;
  }
  
  public boolean isEmpty() {
    return shop == null;
  }
  
  public ShopViewer ifEmpty(Runnable runnable) {
    if (!fails && shop == null)
      runnable.run();
    return this;
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
}
