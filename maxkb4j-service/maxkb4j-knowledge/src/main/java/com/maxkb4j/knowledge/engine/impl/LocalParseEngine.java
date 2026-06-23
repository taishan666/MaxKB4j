package com.maxkb4j.knowledge.engine.impl;

import com.maxkb4j.knowledge.engine.DocumentParseEngine;
import com.maxkb4j.knowledge.parser.DocumentParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalParseEngine implements DocumentParseEngine {

    public static final String NAME = "local";

    private final List<DocumentParser> parsers;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean support(String fileName) {
        for (DocumentParser parser : parsers) {
            if (parser.support(fileName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String extractText(String fileName, InputStream inputStream) {
        for (DocumentParser parser : parsers) {
            if (parser.support(fileName)) {
                return parser.handle(inputStream);
            }
        }
        return "";
    }
}
