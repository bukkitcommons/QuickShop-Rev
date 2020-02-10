package cc.bukkit.shop.integration;

import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.utils.Util;

@Getter
public class IntegrationHelper {
  private Set<IntegratedPlugin> integrations = new HashSet<>();

  public void register(@NotNull IntegratedPlugin clazz) {
    if (!isIntegrationClass(clazz)) {
      throw new InvaildIntegratedException();
    }
    Util.debug("Registering " + clazz.getName());
    integrations.add(clazz);
  }

  public void unregister(@NotNull IntegratedPlugin clazz) {
    if (!isIntegrationClass(clazz)) {
      throw new InvaildIntegratedException();
    }
    Util.debug("Unregistering " + clazz.getName());
    integrations.remove(clazz);
  }

  public void callIntegrationsLoad(@NotNull IntegrateStage stage) {
    integrations.forEach(integratedPlugin -> {
      if (integratedPlugin.getClass().getDeclaredAnnotation(IntegrationStage.class)
          .loadStage() == stage) {
        Util.debug("Calling for load " + integratedPlugin.getName());
        integratedPlugin.load();
      } else {
        Util.debug("Ignored calling because " + integratedPlugin.getName() + " stage is "
            + integratedPlugin.getClass().getDeclaredAnnotation(IntegrationStage.class)
                .loadStage());
      }
    });
  }

  public void callIntegrationsUnload(@NotNull IntegrateStage stage) {
    integrations.forEach(integratedPlugin -> {
      if (integratedPlugin.getClass().getDeclaredAnnotation(IntegrationStage.class)
          .unloadStage() == stage) {
        Util.debug("Calling for unload " + integratedPlugin.getName());
        integratedPlugin.unload();
      } else {
        Util.debug("Ignored calling because " + integratedPlugin.getName() + " stage is "
            + integratedPlugin.getClass().getDeclaredAnnotation(IntegrationStage.class)
                .unloadStage());
      }
    });
  }

  public boolean callIntegrationsCanCreate(@NotNull Player player, @NotNull Location location) {
    for (IntegratedPlugin plugin : integrations) {
      if (!plugin.canCreateShopHere(player, location)) {
        Util.debug("Cancelled by " + plugin.getName());
        return false;
      }
    }
    return true;
  }

  public boolean callIntegrationsCanTrade(@NotNull Player player, @NotNull Location location) {
    for (IntegratedPlugin plugin : integrations) {
      if (!plugin.canTradeShopHere(player, location)) {
        Util.debug("Cancelled by " + plugin.getName());
        return false;
      }
    }
    return true;
  }

  private boolean isIntegrationClass(@NotNull IntegratedPlugin clazz) {
    return clazz.getClass().getDeclaredAnnotation(IntegrationStage.class) != null;
  }
}


class InvaildIntegratedException extends IllegalArgumentException {
  private static final long serialVersionUID = 1L;
}
