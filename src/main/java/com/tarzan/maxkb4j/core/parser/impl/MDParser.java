package com.tarzan.maxkb4j.core.parser.impl;

import com.tarzan.maxkb4j.common.util.IoUtil;
import com.tarzan.maxkb4j.core.parser.DocumentParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@RequiredArgsConstructor
@Component
public class MDParser implements DocumentParser {

    @Override
    public List<String> getExtensions() {
        return List.of(".md");
    }

    @Override
    public String handle(InputStream inputStream) {
        return IoUtil.readToString(inputStream);
    }
}
