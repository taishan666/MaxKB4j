package com.tarzan.maxkb4j.core.workflow.parser.impl;

import com.tarzan.maxkb4j.common.util.IoUtil;
import com.tarzan.maxkb4j.core.workflow.parser.DocumentParser;
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import com.vladsch.flexmark.util.data.MutableDataSet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@RequiredArgsConstructor
@Component
public class MarkDownParser implements DocumentParser {

    @Override
    public boolean support(String fileName) {
        return fileName.endsWith(".md");
    }

    @Override
    public String handle(InputStream inputStream) {
        return IoUtil.readToString(inputStream);
    }
}
