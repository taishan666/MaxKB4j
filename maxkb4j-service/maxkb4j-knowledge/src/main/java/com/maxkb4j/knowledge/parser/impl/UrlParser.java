package com.maxkb4j.knowledge.parser.impl;

import com.maxkb4j.common.util.IoUtil;
import com.maxkb4j.knowledge.parser.DocumentParser;
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@RequiredArgsConstructor
@Component
public class UrlParser implements DocumentParser {

    private final FlexmarkHtmlConverter converter = FlexmarkHtmlConverter.builder().build();

    @Override
    public List<String> getExtensions() {
        return List.of(".url");
    }

    @Override
    public String handle(InputStream inputStream) {
        return converter.convert(IoUtil.readToString(inputStream));
    }
}
