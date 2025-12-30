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
public class HtmlParser implements DocumentParser {

    @Override
    public boolean support(String fileName) {
        return fileName.endsWith(".html");
    }

    @Override
    public String handle(InputStream inputStream) {
        String html = IoUtil.readToString(inputStream);
        // 可选：配置选项
        MutableDataSet options = new MutableDataSet();
        // 例如：设置缩进、换行风格等（见下文配置说明）
        FlexmarkHtmlConverter converter = FlexmarkHtmlConverter.builder(options).build();
        return converter.convert(html);
    }
}
