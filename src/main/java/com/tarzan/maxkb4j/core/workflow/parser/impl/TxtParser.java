package com.tarzan.maxkb4j.core.workflow.parser.impl;

import com.tarzan.maxkb4j.core.workflow.model.SysFile;
import com.tarzan.maxkb4j.core.workflow.parser.DocumentParser;
import com.tarzan.maxkb4j.module.oss.service.MongoFileService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@RequiredArgsConstructor
@Component
public class TxtParser implements DocumentParser {

    private final MongoFileService fileService;
    @Override
    public boolean support(SysFile sysFile) {
        return sysFile.getName().endsWith(".txt");
    }

    @Override
    public String handle(SysFile sysFile) {
        TextDocumentParser  parser = new TextDocumentParser();
        try {
            InputStream inputStream = fileService.getStream(sysFile.getFileId());
            Document document = parser.parse(inputStream);
            return document.text();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
