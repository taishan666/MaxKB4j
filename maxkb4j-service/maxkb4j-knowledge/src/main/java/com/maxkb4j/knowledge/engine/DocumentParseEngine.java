package com.maxkb4j.knowledge.engine;

import java.io.InputStream;

public interface DocumentParseEngine {

    String getName();

    boolean support(String fileName);

    String extractText(String fileName, InputStream inputStream);
}
