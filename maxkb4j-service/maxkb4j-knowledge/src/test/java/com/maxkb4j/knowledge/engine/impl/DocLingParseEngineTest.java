package com.maxkb4j.knowledge.engine.impl;

import com.maxkb4j.knowledge.engine.props.DocLingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocLingParseEngineTest {

    private DocLingProperties properties;
    private DocLingParseEngine engine;

    @BeforeEach
    void setUp() {
        properties = new DocLingProperties();
        properties.setEnabled(true);
        properties.setBaseUrl("http://localhost:5001");
        engine = new DocLingParseEngine(properties);
    }

    @Test
    void shouldMatchLowercaseFileAgainstUppercaseExtension() {
        // 回归：配置扩展名大写（如 .HTML）时也应命中小写文件名
        properties.setSupportedExtensions(List.of(".pdf", ".HTML"));
        assertTrue(engine.support("index.html"));
    }

    @Test
    void shouldMatchUppercaseFileName() {
        assertTrue(engine.support("REPORT.PDF"));
    }

    @Test
    void shouldIgnoreBlankAndSpacedEntries() {
        properties.setSupportedExtensions(List.of("  ", " .pdf "));
        assertTrue(engine.support("a.pdf"));
    }

    @Test
    void shouldRejectUnsupportedExtension() {
        assertFalse(engine.support("a.txt"));
    }

    @Test
    void shouldRejectWhenDisabled() {
        properties.setEnabled(false);
        assertFalse(engine.support("a.pdf"));
    }

    @Test
    void shouldRejectWhenBaseUrlBlank() {
        properties.setBaseUrl("");
        assertFalse(engine.support("a.pdf"));
    }

    @Test
    void shouldRejectNullFileName() {
        assertFalse(engine.support(null));
    }
}
