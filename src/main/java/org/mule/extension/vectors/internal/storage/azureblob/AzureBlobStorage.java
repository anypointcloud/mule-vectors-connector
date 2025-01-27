package org.mule.extension.vectors.internal.storage.azureblob;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.common.StorageSharedKeyCredential;
import dev.langchain4j.data.document.BlankDocumentException;
import dev.langchain4j.data.document.loader.azure.storage.blob.AzureBlobStorageDocumentLoader;

import java.util.Iterator;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.image.Image;
import org.mule.extension.vectors.internal.config.StorageConfiguration;
import org.mule.extension.vectors.internal.connection.storage.azureblob.AzureBlobStorageConnection;
import org.mule.extension.vectors.internal.constant.Constants;
import org.mule.extension.vectors.internal.data.Media;
import org.mule.extension.vectors.internal.error.MuleVectorsErrorType;
import org.mule.extension.vectors.internal.helper.media.MediaProcessor;
import org.mule.extension.vectors.internal.storage.BaseStorage;
import org.mule.extension.vectors.internal.util.MetadataUtils;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AzureBlobStorage extends BaseStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureBlobStorage.class);

    private final String azureName;
    private final String azureKey;

    private StorageSharedKeyCredential getCredentials() {
        return new StorageSharedKeyCredential(azureName, azureKey);
    }

    private BlobServiceClient blobServiceClient;

    private BlobServiceClient getBlobServiceClient() {

        if(this.blobServiceClient == null) {

            // Azure SDK client builders accept the credential as a parameter
            this.blobServiceClient = new BlobServiceClientBuilder()
                .endpoint(String.format("https://%s.blob.core.windows.net/", azureName))
                .credential(getCredentials())
                .buildClient();
        }
        return this.blobServiceClient;
    }

    private AzureBlobStorageDocumentLoader loader;

    private AzureBlobStorageDocumentLoader getLoader() {

        if(this.loader == null) {

            this.loader = new AzureBlobStorageDocumentLoader(getBlobServiceClient());
        }
        return this.loader;
    }

    private Iterator<BlobItem> blobIterator;

    private Iterator<BlobItem> getBlobIterator() {

        if(blobIterator == null) {

            // Get a BlobContainerClient
            BlobContainerClient containerClient = getBlobServiceClient().getBlobContainerClient(contextPath);
            // Get an iterator for all blobs in the container
            this.blobIterator = containerClient.listBlobs().iterator();

        }
        return blobIterator;
    }

    public AzureBlobStorage(StorageConfiguration storageConfiguration, AzureBlobStorageConnection azureBlobStorageConnection,
                            String contextPath, String fileType, String mediaType, MediaProcessor mediaProcessor) {

        super(storageConfiguration, azureBlobStorageConnection, contextPath, fileType, mediaType, mediaProcessor);
        this.azureName = azureBlobStorageConnection.getAzureName();
        this.azureKey = azureBlobStorageConnection.getAzureKey();
        this.blobServiceClient = azureBlobStorageConnection.getBlobServiceClient();
    }

    @Override
    public boolean hasNext() {
        return getBlobIterator().hasNext();
    }

    @Override
    public Document next() {

        BlobItem blobItem = blobIterator.next();
        LOGGER.debug("Blob name: " + blobItem.getName());
        Document document;
        try {
            document = getLoader().loadDocument(contextPath, blobItem.getName(), documentParser);
        } catch(BlankDocumentException bde) {

            LOGGER.warn(String.format("BlankDocumentException: Error while parsing document %s.", contextPath));
            throw bde;
        } catch (Exception e) {
            throw new ModuleException(
                String.format("Error while parsing document %s.", contextPath),
                MuleVectorsErrorType.DOCUMENT_PARSING_FAILURE,
                e);
        }
        MetadataUtils.addMetadataToDocument(document, fileType, blobItem.getName());
        return document;
    }

    public Document getSingleDocument() {

        String[] parts = contextPath.split("/", 2);
        String containerName = parts[0];
        String blobName = parts[1];
        LOGGER.debug("Blob name: " + blobName);
        Document document = getLoader().loadDocument(containerName, blobName, documentParser);
        MetadataUtils.addMetadataToDocument(document, fileType, blobName);
        return document;
    }


    public Media getSingleMedia() {


        Media media;

        switch (mediaType) {

            case Constants.MEDIA_TYPE_IMAGE:

                //MetadataUtils.addImageMetadataToMedia(media, mediaType);
                break;

            default:
                throw new IllegalArgumentException("Unsupported Media Type: " + mediaType);
        }
        return null;
    }

    private Image loadImage() {

        Image image;

        try {



        } catch (Exception ioe) {

            throw new ModuleException(String.format("Impossible to load the image from %s", ""),
                                      MuleVectorsErrorType.STORAGE_SERVICES_FAILURE,
                                      ioe);
        }
        return null;
    }
}
