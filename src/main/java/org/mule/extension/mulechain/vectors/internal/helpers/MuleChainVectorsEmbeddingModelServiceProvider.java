package org.mule.extension.mulechain.vectors.internal.helpers;

import java.util.Set;

import org.mule.extension.mulechain.vectors.internal.constants.MuleChainVectorsConstants;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.values.ValueBuilder;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;

public class MuleChainVectorsEmbeddingModelServiceProvider implements ValueProvider {

  @Override
  public Set<Value> resolve() throws ValueResolvingException {

    return ValueBuilder.getValuesFor(
            MuleChainVectorsConstants.EMBEDDING_MODEL_SERVICE_OPENAI,
            MuleChainVectorsConstants.EMBEDDING_MODEL_SERVICE_MISTRAL_AI,
            MuleChainVectorsConstants.EMBEDDING_MODEL_SERVICE_NOMIC,
            MuleChainVectorsConstants.EMBEDDING_MODEL_SERVICE_HUGGING_FACE,
            MuleChainVectorsConstants.EMBEDDING_MODEL_SERVICE_AZURE_OPENAI); //"OLLAMA", "COHERE", "AZURE_OPENAI", "HUGGING_FACE";
}

}
