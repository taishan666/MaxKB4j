package com.tarzan.maxkb4j.core.workflow.parser;

import java.io.InputStream;

public interface DocumentParser {

    boolean support(String fileName);

    String handle(InputStream inputStream);
}
