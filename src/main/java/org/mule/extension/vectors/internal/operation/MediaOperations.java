package org.mule.extension.vectors.internal.operation;

import org.json.JSONObject;
import org.mule.extension.vectors.api.metadata.MediaResponseAttributes;
import org.mule.extension.vectors.internal.config.StorageConfiguration;
import org.mule.extension.vectors.internal.connection.storage.BaseStorageConnection;
import org.mule.extension.vectors.internal.data.Media;
import org.mule.extension.vectors.internal.error.MuleVectorsErrorType;
import org.mule.extension.vectors.internal.error.provider.MediaErrorTypeProvider;
import org.mule.extension.vectors.internal.helper.parameter.MediaParameters;
import org.mule.extension.vectors.internal.storage.BaseStorage;
import org.mule.extension.vectors.internal.util.JsonUtils;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.metadata.fixed.OutputJsonType;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.HashMap;

import static org.mule.extension.vectors.internal.helper.ResponseHelper.createMediaResponse;
import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;

public class MediaOperations {

  private static final Logger LOGGER = LoggerFactory.getLogger(MediaOperations.class);

  /**
   * Loads a single media from the storage specified by the {@code contextPath} and returns its content
   * in JSON format. The media is processed into segments based on the provided segmentation parameters.
   *
   * @param storageConfiguration the configuration for accessing the media.
   * @param storageConnection      the connection to the media storage.
   * @param mediaParameters     parameters for specifying the media location and type.
   * @return a {@link Result} containing the media's content as an {@link InputStream} and
   *         additional metadata in {@link MediaResponseAttributes}.
   * @throws ModuleException if an error occurs while loading or processing the media.
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("Media-load-single")
  @DisplayName("[Media] Load single")
  @Throws(MediaErrorTypeProvider.class)
  //@OutputJsonType(schema = "api/metadata/MediaLoadSingleResponse.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, MediaResponseAttributes>
  loadSingleMedia(@Config StorageConfiguration storageConfiguration,
                     @Connection BaseStorageConnection storageConnection,
                     @ParameterGroup(name = "Media") MediaParameters mediaParameters) {

    try {

      BaseStorage baseStorage = BaseStorage.builder()
          .configuration(storageConfiguration)
          .connection(storageConnection)
          .contextPath(mediaParameters.getContextPath())
          .fileType(mediaParameters.getMediaType())
          .build();

      Media media = baseStorage.getSingleMedia();

      JSONObject jsonObject = JsonUtils.mediaToJson(media);

      return createMediaResponse(
          jsonObject.toString(),
          new HashMap<String, Object>() {{
            put("mediaType", mediaParameters.getMediaType());
            put("contextPath", mediaParameters.getContextPath());
          }});

    } catch (ModuleException me) {
      throw me;

    } catch (Exception e) {
      throw new ModuleException(
          String.format("Error while loading media at '%s'.", mediaParameters.getContextPath()),
          MuleVectorsErrorType.MEDIA_OPERATIONS_FAILURE,
          e);
    }
  }
}
