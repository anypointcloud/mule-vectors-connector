package org.mule.extension.mulechain.vectors.internal.helpers;

import java.util.Set;

import org.mule.extension.mulechain.vectors.internal.constants.MuleChainVectorsConstants;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.values.ValueBuilder;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;

public class MuleChainVectorsEmbeddingModelTypeProvider implements ValueProvider {

  @Override
  public Set<Value> resolve() throws ValueResolvingException {

    return ValueBuilder.getValuesFor(
            MuleChainVectorsConstants.OPENAI,
            MuleChainVectorsConstants.MISTRAL_AI,
            MuleChainVectorsConstants.NOMIC,
            MuleChainVectorsConstants.HUGGING_FACE,
            MuleChainVectorsConstants.AZURE_OPENAI); //"OLLAMA", "COHERE", "AZURE_OPENAI", "HUGGING_FACE";
}

}
