package cc.bukkit.shop.event;

public enum ProtectionCheckStatus {
  BEGIN(0), END(1);

  int statusCode;

  ProtectionCheckStatus(int statusCode) {
    this.statusCode = statusCode;
  }
}
