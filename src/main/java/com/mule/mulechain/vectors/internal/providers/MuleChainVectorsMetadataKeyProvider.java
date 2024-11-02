package com.mule.mulechain.vectors.internal.providers;

import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.values.ValueBuilder;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;

import java.util.Set;

public class MuleChainVectorsMetadataKeyProvider implements ValueProvider {

  @Override
  public Set<Value> resolve() throws ValueResolvingException {
    // TODO Auto-generated method stub
    return ValueBuilder.getValuesFor("file_name", "url", "full_path", "absolute_directory_path");// "textSegment"
  }

}
