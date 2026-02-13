package com.tarzan.maxkb4j.core.parser;

import java.io.InputStream;
import java.util.List;

public interface DocumentParser {

    List<String> getExtensions();

    default boolean support(String fileName) {
        if (fileName == null) return false;
        String lowerName = fileName.toLowerCase();
        return getExtensions().stream().anyMatch(lowerName::endsWith);
    }

    String handle(InputStream inputStream);
}
