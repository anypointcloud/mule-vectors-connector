package org.mule.extension.mulechain.vectors.internal.helpers;

import java.util.Set;

import org.mule.extension.mulechain.vectors.internal.constants.MuleChainVectorsConstants;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.values.ValueBuilder;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;

public class MuleChainVectorsVectorStoreProvider implements ValueProvider {

  @Override
  public Set<Value> resolve() throws ValueResolvingException {

     return ValueBuilder.getValuesFor(
            MuleChainVectorsConstants.VECTOR_STORE_PGVECTOR,
            MuleChainVectorsConstants.VECTOR_STORE_ELASTICSEARCH,
            MuleChainVectorsConstants.VECTOR_STORE_MILVUS,
            MuleChainVectorsConstants.VECTOR_STORE_CHROMA,
            MuleChainVectorsConstants.VECTOR_STORE_PINECONE,
            MuleChainVectorsConstants.VECTOR_STORE_WEAVIATE,
            MuleChainVectorsConstants.VECTOR_STORE_AI_SEARCH
    ); // MuleChainVectorsConstants.VECTOR_STORE_NEO4J
  }

}
