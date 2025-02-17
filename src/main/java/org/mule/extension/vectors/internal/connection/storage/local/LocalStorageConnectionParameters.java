package org.mule.extension.vectors.internal.connection.storage.local;

import org.mule.extension.vectors.internal.connection.storage.BaseStorageConnectionParameters;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import static org.mule.runtime.api.meta.model.display.PathModel.Location.EXTERNAL;

public class LocalStorageConnectionParameters extends BaseStorageConnectionParameters {

  @Parameter
  @Optional
  @DisplayName("Working Directory")
  @Summary("Directory to be considered as the root of every relative path used with this connector. Defaults to the user's home")
  @org.mule.runtime.extension.api.annotation.param.display.Path(location = EXTERNAL)
  private String workingDir;

  public String getWorkingDir() {
    return workingDir;
  }
}
