package org.mule.extension.vectors.internal.model.multimodal.azureaivision;

public class AzureAIVisionImageEmbeddingRequestBody {

  private byte[] image;

  public AzureAIVisionImageEmbeddingRequestBody(byte[] image) {
    this.image = image;
  }

  public byte[] getImage() {
    return image;
  }

  public void setImage(byte[] image) {
    this.image = image;
  }
}
