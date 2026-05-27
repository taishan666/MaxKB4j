package com.maxkb4j.knowledge.service;

import ai.docling.serve.api.DoclingServeApi;
import ai.docling.serve.api.convert.request.options.ConvertDocumentOptions;
import ai.docling.serve.api.convert.request.options.InputFormat;
import ai.docling.serve.api.convert.request.options.OutputFormat;
import com.maxkb4j.knowledge.parser.DocumentParser;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.docling.DoclingDocumentParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DoclingParseService implements IDocumentParseService{

    private final List<DocumentParser> parsers;

    public String extractText(String fileName,InputStream inputStream) {
        DoclingServeApi api = DoclingServeApi.builder()
                .baseUrl("http://127.0.0.1:5015")
                .build();
        List<InputFormat> inputFormats=List.of(InputFormat.DOCX,InputFormat.CSV,InputFormat.PDF,InputFormat.XLSX,InputFormat.DOCX);
        ConvertDocumentOptions options = ConvertDocumentOptions.builder()
                .fromFormats(inputFormats)
                .toFormat(OutputFormat.MARKDOWN)
                .build();
        DoclingDocumentParser parser = new DoclingDocumentParser(api,options);
        Document document = parser.parse(inputStream);
        String text = document.text();
        System.out.println(text);
        return text;
    }

}
