package org.maxgamer.quickshop.configuration;

public enum NodeType {
  /*
   * A standard config node
   */
  CONFIG,
  
  /*
   * A config node with moving action
   * and will move an other node to here.
   */
  MOVE,
  
  /*
   * A node that will remove the path.
   */
  REMOVE;
}
