package org.mule.extension.mulechain.vectors.internal.helper.provider;

import java.util.Set;

import org.mule.extension.mulechain.vectors.internal.constant.Constants;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.values.ValueBuilder;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;

public class VectorStoreProvider implements ValueProvider {

  @Override
  public Set<Value> resolve() throws ValueResolvingException {

     return ValueBuilder.getValuesFor(
            Constants.VECTOR_STORE_PGVECTOR,
            Constants.VECTOR_STORE_ELASTICSEARCH,
            Constants.VECTOR_STORE_MILVUS,
            Constants.VECTOR_STORE_CHROMA,
            Constants.VECTOR_STORE_PINECONE,
            Constants.VECTOR_STORE_WEAVIATE,
            Constants.VECTOR_STORE_AI_SEARCH
    ); // MuleChainVectorsConstants.VECTOR_STORE_NEO4J
  }

}
