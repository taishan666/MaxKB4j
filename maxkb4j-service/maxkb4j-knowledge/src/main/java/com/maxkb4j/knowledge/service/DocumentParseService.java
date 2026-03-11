package com.maxkb4j.knowledge.service;

import com.maxkb4j.knowledge.parser.DocumentParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentParseService implements IDocumentParseService{

    private final List<DocumentParser> parsers;

    public String extractText(String fileName,InputStream inputStream) {
        for (DocumentParser parser : parsers) {
            if (parser.support(fileName)) {
                return parser.handle(inputStream);
            }
        }
        return "";
    }

}
