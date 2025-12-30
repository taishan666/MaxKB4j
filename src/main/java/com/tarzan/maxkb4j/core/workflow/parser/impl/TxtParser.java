package com.tarzan.maxkb4j.core.workflow.parser.impl;

import com.tarzan.maxkb4j.core.workflow.parser.DocumentParser;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@RequiredArgsConstructor
@Component
public class TxtParser implements DocumentParser {

    @Override
    public boolean support(String fileName) {
        return fileName.endsWith(".txt");
    }

    @Override
    public String handle(InputStream inputStream) {
        TextDocumentParser  parser = new TextDocumentParser();
        Document document = parser.parse(inputStream);
        return document.text();
    }
}
