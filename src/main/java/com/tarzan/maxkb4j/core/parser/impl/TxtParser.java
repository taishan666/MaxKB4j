package com.tarzan.maxkb4j.core.parser.impl;

import com.tarzan.maxkb4j.core.parser.DocumentParser;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@RequiredArgsConstructor
@Component
public class TxtParser implements DocumentParser {

    @Override
    public List<String> getExtensions() {
        return List.of(".txt");
    }


    @Override
    public String handle(InputStream inputStream) {
        TextDocumentParser  parser = new TextDocumentParser();
        Document document = parser.parse(inputStream);
        return document.text();
    }
}
