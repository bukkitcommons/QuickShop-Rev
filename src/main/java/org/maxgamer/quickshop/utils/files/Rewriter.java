package org.maxgamer.quickshop.utils.files;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

public final class Rewriter implements Consumer<InputStream> {

  @NotNull
  private final File file;

  public Rewriter(@NotNull File file) {
    this.file = file;
  }

  @Override
  public void accept(@NotNull InputStream inputStream) {
    try (final OutputStream out = new FileOutputStream(file)) {
      final byte[] buf = new byte[1024];
      int len;

      while ((len = inputStream.read(buf)) > 0) {
        out.write(buf, 0, len);
      }

      out.close();
      inputStream.close();
    } catch (Exception exception) {
      throw new IllegalStateException(exception);
    }
  }
}
