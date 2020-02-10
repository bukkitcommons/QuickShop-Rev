package cc.bukkit.shop.viewer;

public enum ViewAction {
  /**
   * Equals with `continue` in Java,
   * indicates skip the current viewer action.
   */
  NEXT,
  
  /**
   * Equals with `break` in Java,
   * indicates breaking the current viewer action.
   */
  BREAK;
}
