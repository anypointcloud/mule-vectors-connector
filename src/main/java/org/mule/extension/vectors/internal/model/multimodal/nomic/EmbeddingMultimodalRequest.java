package org.mule.extension.vectors.internal.model.multimodal.nomic;

import java.util.List;

public class EmbeddingMultimodalRequest {

  private String model;
  private List<byte[]> images;

  EmbeddingMultimodalRequest(String model, List<byte[]> images) {
    this.model = model;
    this.images = images;
  }

  public static EmbeddingMultimodalRequestBuilder builder() {
    return new EmbeddingMultimodalRequestBuilder();
  }

  public String getModel() {
    return this.model;
  }

  public List<byte[]> getImages() {
    return this.images;
  }

  public static class EmbeddingMultimodalRequestBuilder {

    private String model;
    private List<byte[]> images;

    EmbeddingMultimodalRequestBuilder() {
    }

    public EmbeddingMultimodalRequestBuilder model(String model) {
      this.model = model;
      return this;
    }

    public EmbeddingMultimodalRequestBuilder images(List<byte[]> images) {
      this.images = images;
      return this;
    }


    public EmbeddingMultimodalRequest build() {
      return new EmbeddingMultimodalRequest(this.model, this.images);
    }

    public String toString() {
      return "EmbeddingRequest.EmbeddingRequestBuilder(model=" + this.model + ")";
    }
  }
}
