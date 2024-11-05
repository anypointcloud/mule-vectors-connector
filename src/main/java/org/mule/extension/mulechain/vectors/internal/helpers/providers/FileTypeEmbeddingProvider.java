package org.mule.extension.mulechain.vectors.internal.helpers.providers;


import java.util.Set;

import org.mule.extension.mulechain.vectors.internal.constants.MuleChainVectorsConstants;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.values.ValueBuilder;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;

public class FileTypeEmbeddingProvider implements ValueProvider {

  @Override
  public Set<Value> resolve() throws ValueResolvingException {

    return ValueBuilder.getValuesFor(
            MuleChainVectorsConstants.FILE_TYPE_ANY,
            MuleChainVectorsConstants.FILE_TYPE_TEXT,
            MuleChainVectorsConstants.FILE_TYPE_URL,
            MuleChainVectorsConstants.FILE_TYPE_CRAWL);
  }

}
