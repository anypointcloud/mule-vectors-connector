package org.mule.extension.vectors.internal.model.einstein;

import java.util.HashMap;
import java.util.Map;

public enum EinsteinEmbeddingModelName {

  ANTHROPIC_CLAUDE_3_HAIKU_ON_AMAZON("sfdc_ai__DefaultBedrockAnthropicClaude3Haiku", 0),
  AZURE_OPENAI_ADA_002("sfdc_ai__DefaultAzureOpenAITextEmbeddingAda_002", 1536),
  AZURE_OPENAI_GPT_3_5_TURBO("sfdc_ai__DefaultAzureOpenAIGPT35Turbo", 4096),
  AZURE_OPENAI_GPT_3_5_TURBO_16K("sfdc_ai__DefaultAzureOpenAIGPT35Turbo_16k", 16384),
  AZURE_OPENAI_GPT_4_TURBO("sfdc_ai__DefaultAzureOpenAIGPT4Turbo", 8192),
  OPENAI_ADA_002("sfdc_ai__DefaultOpenAITextEmbeddingAda_002", 1536),
  OPENAI_GPT_3_5_TURBO("sfdc_ai__DefaultOpenAIGPT35Turbo", 4096),
  OPENAI_GPT_3_5_TURBO_16K("sfdc_ai__DefaultOpenAIGPT35Turbo_16k", 16384),
  OPENAI_GPT_4("sfdc_ai__DefaultOpenAIGPT4", 8192),
  OPENAI_GPT_4_32K("sfdc_ai__DefaultOpenAIGPT4_32k", 32768),
  OPENAI_GPT_4O_OMNI("sfdc_ai__DefaultOpenAIGPT4Omni", 8192),
  OPENAI_GPT_4_TURBO("sfdc_ai__DefaultOpenAIGPT4Turbo", 8192);

  private final String stringValue;
  private final Integer dimension;
  private static final Map<String, Integer> KNOWN_DIMENSION = new HashMap(values().length);

  private EinsteinEmbeddingModelName(String stringValue, Integer dimension) {
    this.stringValue = stringValue;
    this.dimension = dimension;
  }

  public String toString() {
    return this.stringValue;
  }

  public Integer dimension() {
    return this.dimension;
  }

  public static Integer knownDimension(String modelName) {
    return (Integer)KNOWN_DIMENSION.get(modelName);
  }

  static {
    EinsteinEmbeddingModelName[] var0 = values();
    int var1 = var0.length;

    for(int var2 = 0; var2 < var1; ++var2) {
      EinsteinEmbeddingModelName embeddingModelName = var0[var2];
      KNOWN_DIMENSION.put(embeddingModelName.toString(), embeddingModelName.dimension());
    }
  }
}
