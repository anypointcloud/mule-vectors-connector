package org.mule.extension.vectors.internal.storage.local;

import org.mule.extension.vectors.internal.constant.Constants;
import org.mule.extension.vectors.internal.storage.BaseStorageConfiguration;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

public class LocalStorageConfiguration implements BaseStorageConfiguration {

  @Parameter
  @Alias("workingDirecotry")
  @DisplayName("Working Directory")
  @Summary("The working directory. It represents the root for context paths.")
  @Placement(order = 1)
  @Expression(ExpressionSupport.SUPPORTED)
  @Optional
  private String workingDirectory;

  public String getWorkingDirectory() {
    return workingDirectory;
  }

  @Override
  public String getStorageType() { return Constants.STORAGE_TYPE_LOCAL; }

  public void setWorkingDirectory(String workingDirectory) {
    this.workingDirectory = workingDirectory;
  }
}
