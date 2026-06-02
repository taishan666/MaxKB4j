package com.maxkb4j.knowledge.service;

import com.maxkb4j.knowledge.engine.DocumentParseEngine;
import com.maxkb4j.knowledge.engine.impl.LocalDParseEngine;
import com.maxkb4j.knowledge.engine.impl.MinerUParseEngine;
import com.maxkb4j.knowledge.engine.props.MinerUProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentParseService implements IDocumentParseService {

    private final LocalDParseEngine localEngine;
    private final MinerUParseEngine minerUEngine;
    private final MinerUProperties minerUProperties;

    @Override
    public String extractText(String fileName, InputStream inputStream) {
        DocumentParseEngine engine = selectEngine(fileName);
        if (engine instanceof MinerUParseEngine) {
            byte[] bytes;
            try {
                bytes = inputStream.readAllBytes();
            } catch (IOException e) {
                throw new RuntimeException("读取文件流失败: " + fileName, e);
            }
            try {
                return engine.extractText(fileName, new ByteArrayInputStream(bytes));
            } catch (Exception e) {
                log.warn("MinerU 解析 {} 失败, 回退本地引擎: {}", fileName, e.getMessage());
                return localEngine.extractText(fileName, new ByteArrayInputStream(bytes));
            }
        }
        return engine.extractText(fileName, inputStream);
    }

    private DocumentParseEngine selectEngine(String fileName) {
        if (minerUProperties.isEnabled() && minerUEngine.support(fileName)) {
            return minerUEngine;
        }
        return localEngine;
    }
}
