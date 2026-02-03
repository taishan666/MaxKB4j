package com.tarzan.maxkb4j.module.knowledge.service;

import com.tarzan.maxkb4j.core.parser.DocumentParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentParseService {

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
