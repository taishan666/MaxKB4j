package com.tarzan.maxkb4j.core.workflow.parser.impl;

import com.tarzan.maxkb4j.common.util.IoUtil;
import com.tarzan.maxkb4j.core.workflow.model.SysFile;
import com.tarzan.maxkb4j.core.workflow.parser.DocumentParser;
import com.tarzan.maxkb4j.module.oss.service.MongoFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@RequiredArgsConstructor
@Component
public class HtmlParser implements DocumentParser {
    private final MongoFileService fileService;

    @Override
    public boolean support(SysFile sysFile) {
        return sysFile.getName().endsWith(".html");
    }

    @Override
    public String handle(SysFile sysFile) {
        try {
            InputStream inputStream = fileService.getStream(sysFile.getFileId());
            return IoUtil.readToString(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
