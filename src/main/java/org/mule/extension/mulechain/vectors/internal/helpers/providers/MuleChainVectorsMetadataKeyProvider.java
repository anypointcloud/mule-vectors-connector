package org.mule.extension.mulechain.vectors.internal.helpers.providers;

import org.mule.extension.mulechain.vectors.internal.constants.MuleChainVectorsConstants;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.values.ValueBuilder;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;

import java.util.Set;

public class MuleChainVectorsMetadataKeyProvider implements ValueProvider {

  @Override
  public Set<Value> resolve() throws ValueResolvingException {

    return ValueBuilder.getValuesFor(
            MuleChainVectorsConstants.METADATA_KEY_FILE_NAME,
            MuleChainVectorsConstants.METADATA_KEY_FILE_TYPE,
            MuleChainVectorsConstants.METADATA_KEY_URL,
            MuleChainVectorsConstants.METADATA_KEY_FULL_PATH,
            MuleChainVectorsConstants.METADATA_KEY_ABSOLUTE_DIRECTORY_PATH);// "textSegment"
  }

}
