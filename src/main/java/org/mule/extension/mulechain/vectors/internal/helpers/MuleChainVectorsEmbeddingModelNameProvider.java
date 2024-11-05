package org.mule.extension.mulechain.vectors.internal.helpers;

import java.util.Collections;
import java.util.Set;

import org.mule.extension.mulechain.vectors.internal.config.MuleChainVectorsConfiguration;
import org.mule.extension.mulechain.vectors.internal.constants.MuleChainVectorsConstants;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.values.ValueBuilder;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;

public class MuleChainVectorsEmbeddingModelNameProvider implements ValueProvider {

  @Config
  private MuleChainVectorsConfiguration configuration;

  private static final Set<Value> VALUES_FOR_AZURE_OPENAI = ValueBuilder.getValuesFor(
          MuleChainVectorsConstants.EMBEDDING_MODEL_NAME_TEXT_EMBEDDING_3_SMALL,
          MuleChainVectorsConstants.EMBEDDING_MODEL_NAME_TEXT_EMBEDDING_3_LARGE,
          MuleChainVectorsConstants.EMBEDDING_MODEL_NAME_TEXT_EMBEDDING_ADA_002
  );

  private static final Set<Value> VALUES_FOR_OPENAI = ValueBuilder.getValuesFor(
          MuleChainVectorsConstants.EMBEDDING_MODEL_NAME_TEXT_EMBEDDING_3_SMALL,
          MuleChainVectorsConstants.EMBEDDING_MODEL_NAME_TEXT_EMBEDDING_3_LARGE,
          MuleChainVectorsConstants.EMBEDDING_MODEL_NAME_TEXT_EMBEDDING_ADA_002
  );

  private static final Set<Value> VALUES_FOR_MISTRAL_AI = ValueBuilder.getValuesFor(
          MuleChainVectorsConstants.EMBEDDING_MODEL_NAME_MISTRAL_EMBED
  );

  private static final Set<Value> VALUES_FOR_NOMIC = ValueBuilder.getValuesFor(
          MuleChainVectorsConstants.EMBEDDING_MODEL_NAME_NOMIC_EMBED_TEXT
  );

  private static final Set<Value> VALUES_FOR_HUGGING_FACE = ValueBuilder.getValuesFor(
          MuleChainVectorsConstants.EMBEDDING_MODEL_NAME_FALCON_7B_INSTRUCT,
          MuleChainVectorsConstants.EMBEDDING_MODEL_NAME_MINI_LM_L6_V2
  );

  @Override
  public Set<Value> resolve() throws ValueResolvingException {

    String embeddingModelService = configuration.getEmbeddingModelService();
    switch (embeddingModelService) {
      case MuleChainVectorsConstants.EMBEDDING_MODEL_SERVICE_OPENAI:
        return VALUES_FOR_OPENAI;
      case MuleChainVectorsConstants.EMBEDDING_MODEL_SERVICE_MISTRAL_AI:
        return VALUES_FOR_MISTRAL_AI;
      case MuleChainVectorsConstants.EMBEDDING_MODEL_SERVICE_NOMIC:
        return VALUES_FOR_NOMIC;
      case MuleChainVectorsConstants.EMBEDDING_MODEL_SERVICE_HUGGING_FACE:
        return VALUES_FOR_HUGGING_FACE;
      default:
        return Collections.emptySet();
    }
  }

}
