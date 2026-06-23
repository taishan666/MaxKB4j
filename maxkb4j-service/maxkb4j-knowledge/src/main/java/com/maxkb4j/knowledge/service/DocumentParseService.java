package com.maxkb4j.knowledge.service;

import com.maxkb4j.knowledge.engine.DocumentParseEngine;
import com.maxkb4j.knowledge.engine.impl.DocLingParseEngine;
import com.maxkb4j.knowledge.engine.impl.LocalParseEngine;
import com.maxkb4j.knowledge.engine.props.DocLingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentParseService implements IDocumentParseService {

    private final LocalParseEngine localEngine;
    private final DocLingParseEngine docLingParseEngine;
    private final DocLingProperties docLingProperties;

    @Override
    public String extractText(String fileName, InputStream inputStream) {
        DocumentParseEngine engine = selectEngine(fileName);
        return engine.extractText(fileName, inputStream);
    }

    private DocumentParseEngine selectEngine(String fileName) {
        if (docLingProperties.isEnabled() && docLingParseEngine.support(fileName)) {
            return docLingParseEngine;
        }
        return localEngine;
    }
}
