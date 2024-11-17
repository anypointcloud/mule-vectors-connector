package org.mule.extension.mulechain.vectors.internal.operation;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.UrlDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.document.transformer.jsoup.HtmlToTextDocumentTransformer;
import dev.langchain4j.data.segment.TextSegment;
import org.json.JSONObject;
import org.mule.extension.mulechain.vectors.internal.constant.Constants;
import org.mule.extension.mulechain.vectors.internal.helper.parameter.FileTypeParameters;
import org.mule.extension.mulechain.vectors.internal.helper.parameter.SegmentationParameters;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;

public class DocumentOperations {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentOperations.class);

    /**
     * Splits a document provided by full path in to a defined set of chucks and overlaps
     */
    @MediaType(value = APPLICATION_JSON, strict = false)
    @Alias("Document-split-into-chunks")
    public InputStream documentSplitter(@Alias("contextPath") @DisplayName("Context Path") @Summary("The context path.") String contextPath,
                                        @ParameterGroup(name = "Context") FileTypeParameters fileType,
                                        @ParameterGroup(name = "Segmentation") SegmentationParameters segmentationParameters){

        List<TextSegment> segments;
        DocumentSplitter splitter;
        Document document = null;
        switch (fileType.getFileType()) {
            case Constants.FILE_TYPE_TEXT:
                document = loadDocument(contextPath, new TextDocumentParser());
                splitter = DocumentSplitters.recursive(segmentationParameters.getMaxSegmentSizeInChar(), segmentationParameters.getMaxOverlapSizeInChars());
                segments = splitter.split(document);
                break;
            case Constants.FILE_TYPE_ANY:
                document = loadDocument(contextPath, new ApacheTikaDocumentParser());
                splitter = DocumentSplitters.recursive(segmentationParameters.getMaxSegmentSizeInChar(), segmentationParameters.getMaxOverlapSizeInChars());
                segments = splitter.split(document);
                break;
            case Constants.FILE_TYPE_URL:
                URL url = null;
                try {
                    url = new URL(contextPath);
                } catch (MalformedURLException e) {
                    LOGGER.error(e.getMessage() + " " + Arrays.toString(e.getStackTrace()));
                }

                Document htmlDocument = UrlDocumentLoader.load(url, new TextDocumentParser());
                HtmlToTextDocumentTransformer transformer = new HtmlToTextDocumentTransformer(null, null, true);
                document = transformer.transform(htmlDocument);
                document.metadata().put(Constants.METADATA_KEY_URL, contextPath);
                splitter = DocumentSplitters.recursive(segmentationParameters.getMaxSegmentSizeInChar(), segmentationParameters.getMaxOverlapSizeInChars());
                segments = splitter.split(document);
                break;
            default:
                throw new IllegalArgumentException("Unsupported File Type: " + fileType.getFileType());
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("contextPath", contextPath);
        jsonObject.put("fileType", fileType.getFileType());
        jsonObject.put("segments", segments.toString());

        return toInputStream(jsonObject.toString(), StandardCharsets.UTF_8);
    }

    /**
     * Parses a document by filepath and returns the text
     */
    @MediaType(value = APPLICATION_JSON, strict = false)
    @Alias("Document-parser")
    public InputStream documentParser(@Alias("contextPath") @DisplayName("Context Path") @Summary("The context path.") String contextPath,
                                      @ParameterGroup(name = "Context") FileTypeParameters fileType){

        Document document = null;
        switch (fileType.getFileType()) {
            case Constants.FILE_TYPE_TEXT:
                document = loadDocument(contextPath, new TextDocumentParser());
                break;
            case Constants.FILE_TYPE_ANY:
                document = loadDocument(contextPath, new ApacheTikaDocumentParser());
                break;
            case Constants.FILE_TYPE_URL:
                URL url = null;
                try {
                    url = new URL(contextPath);
                } catch (MalformedURLException e) {
                    LOGGER.error(e.getMessage() + " " + Arrays.toString(e.getStackTrace()));
                }

                Document htmlDocument = UrlDocumentLoader.load(url, new TextDocumentParser());
                HtmlToTextDocumentTransformer transformer = new HtmlToTextDocumentTransformer(null, null, true);
                document = transformer.transform(htmlDocument);
                document.metadata().put(Constants.METADATA_KEY_URL, contextPath);

                break;
            default:
                throw new IllegalArgumentException("Unsupported File Type: " + fileType.getFileType());
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("contextPath", contextPath);
        jsonObject.put("fileType", fileType.getFileType());
        jsonObject.put("documentText",document.text());
        jsonObject.put("metadata",document.metadata());


        return toInputStream(jsonObject.toString(), StandardCharsets.UTF_8);
    }
}
