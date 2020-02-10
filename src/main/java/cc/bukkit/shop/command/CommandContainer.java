package cc.bukkit.shop.command;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Data
@Builder
public class CommandContainer {
  private CommandProcesser executor;
  private boolean hidden; // Hide from help, tabcomplete
  @Singular
  private List<String> permissions; // E.g quickshop.unlimited
  private String prefix; // E.g /qs <prefix>
}
