package org.mule.extension.vectors.internal.helper.provider;

import java.util.Set;

import org.mule.extension.vectors.internal.constant.Constants;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.values.ValueBuilder;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;

public class EmbeddingModelServiceProvider implements ValueProvider {

  @Override
  public Set<Value> resolve() throws ValueResolvingException {

    return ValueBuilder.getValuesFor(
            Constants.EMBEDDING_MODEL_SERVICE_OPENAI,
            Constants.EMBEDDING_MODEL_SERVICE_MISTRAL_AI,
            Constants.EMBEDDING_MODEL_SERVICE_NOMIC,
            Constants.EMBEDDING_MODEL_SERVICE_HUGGING_FACE,
            Constants.EMBEDDING_MODEL_SERVICE_AZURE_OPENAI); //"OLLAMA", "COHERE", "AZURE_OPENAI", "HUGGING_FACE";
}

}
