package com.tarzan.maxkb4j.core.parser.impl;

import com.tarzan.maxkb4j.common.util.PDFParser;
import com.tarzan.maxkb4j.core.parser.DocumentParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@RequiredArgsConstructor
@Component
public class PdfParser implements DocumentParser {

    @Override
    public boolean support(String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(".pdf");
    }

    @Override
    public String handle(InputStream inputStream) {
        try {
            return PDFParser.parse(inputStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}