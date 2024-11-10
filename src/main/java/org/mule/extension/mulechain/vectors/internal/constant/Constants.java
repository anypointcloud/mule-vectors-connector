/**
 * (c) 2003-2024 MuleSoft, Inc. The software in this package is published under the terms of the Commercial Free Software license V.1 a copy of which has been included with this distribution in the LICENSE.md file.
 */
package org.mule.extension.mulechain.vectors.internal.constant;

public class Constants {

  private Constants() {}

  public static final String FILE_TYPE_ANY = "any";
  public static final String FILE_TYPE_TEXT = "text";
  public static final String FILE_TYPE_CRAWL = "crawl";
  public static final String FILE_TYPE_URL = "url";

  public static final String STORAGE_TYPE_LOCAL = "Local";
  public static final String STORAGE_TYPE_S3 = "S3";
  public static final String STORAGE_TYPE_AZURE_BLOB = "AZURE_BLOB";

  public static final String EMBEDDING_MODEL_SERVICE_OPENAI = "OPENAI";
  public static final String EMBEDDING_MODEL_SERVICE_MISTRAL_AI = "MISTRAL_AI";
  public static final String EMBEDDING_MODEL_SERVICE_NOMIC = "NOMIC";
  public static final String EMBEDDING_MODEL_SERVICE_HUGGING_FACE = "HUGGING_FACE";
  public static final String EMBEDDING_MODEL_SERVICE_AZURE_OPENAI = "AZURE_OPENAI";

  public static final String EMBEDDING_MODEL_NAME_TEXT_EMBEDDING_3_SMALL = "text-embedding-3-small";
  public static final String EMBEDDING_MODEL_NAME_TEXT_EMBEDDING_3_LARGE = "text-embedding-3-large";
  public static final String EMBEDDING_MODEL_NAME_TEXT_EMBEDDING_ADA_002 = "text-embedding-ada-002";
  public static final String EMBEDDING_MODEL_NAME_MISTRAL_EMBED = "mistral-embed";
  public static final String EMBEDDING_MODEL_NAME_NOMIC_EMBED_TEXT = "nomic-embed-text";
  public static final String EMBEDDING_MODEL_NAME_FALCON_7B_INSTRUCT = "tiiuae/falcon-7b-instruct";
  public static final String EMBEDDING_MODEL_NAME_MINI_LM_L6_V2 = "sentence-transformers/all-MiniLM-L6-v2";

  public static final String VECTOR_STORE_PGVECTOR = "PGVECTOR";
  public static final String VECTOR_STORE_ELASTICSEARCH = "ELASTICSEARCH";
  public static final String VECTOR_STORE_MILVUS = "MILVUS";
  public static final String VECTOR_STORE_CHROMA = "CHROMA";
  public static final String VECTOR_STORE_PINECONE = "PINECONE";
  public static final String VECTOR_STORE_WEAVIATE = "WEAVIATE";
  public static final String VECTOR_STORE_AI_SEARCH = "AI_SEARCH";
  public static final String VECTOR_STORE_NEO4J = "NEO4J";

  public static final String METADATA_KEY_SOURCE_ID = "source_id";
  public static final String METADATA_KEY_INDEX = "index";
  public static final String METADATA_KEY_FILE_NAME = "file_name";
  public static final String METADATA_KEY_FILE_TYPE = "file_type";
  public static final String METADATA_KEY_FULL_PATH = "full_path";
  public static final String METADATA_KEY_ABSOLUTE_DIRECTORY_PATH = "absolute_directory_path";
  public static final String METADATA_KEY_URL = "url";
  public static final String METADATA_KEY_SOURCE = "source";
  public static final String METADATA_KEY_TITLE = "title";
  public static final String METADATA_KEY_INGESTION_DATETIME = "ingestion_datetime";

  public static final String METADATA_FILTER_METHOD_IS_EQUAL_TO = "isEqualTo";
  public static final String METADATA_FILTER_METHOD_IS_NOT_EQUAL_TO = "isNotEqualTo";
  public static final String METADATA_FILTER_METHOD_IS_GREATER_THAN = "isGreaterThan";
  public static final String METADATA_FILTER_METHOD_IS_LESS_THAN = "isLessThan";

  public static final Double EMBEDDING_SEARCH_REQUEST_DEFAULT_MIN_SCORE = 0.7;

  public static final String EMBEDDING_OPERATION_TYPE_STORE_METADATA = "STORE_METADATA";
  public static final String EMBEDDING_OPERATION_TYPE_FILTER_BY_METADATA = "FILTER_BY_METADATA";
  public static final String EMBEDDING_OPERATION_TYPE_REMOVE_EMBEDDINGS = "REMOVE_EMBEDDINGS";
  public static final String EMBEDDING_OPERATION_TYPE_QUERY_ALL = "QUERY_ALL";
}
