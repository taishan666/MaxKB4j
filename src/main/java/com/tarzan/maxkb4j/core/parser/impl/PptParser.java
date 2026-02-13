package com.tarzan.maxkb4j.core.parser.impl;

import com.tarzan.maxkb4j.core.parser.DocumentParser;
import lombok.RequiredArgsConstructor;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.microsoft.OfficeParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RequiredArgsConstructor
@Component
public class PptParser implements DocumentParser {


    @Override
    public List<String> getExtensions() {
        return List.of(".ppt", ".pptx");
    }

    @Override
    public String handle(InputStream inputStream) {
        // 默认限制10k字符，可传入 -1 取消限制
        ContentHandler handler = new BodyContentHandler();
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();
        Parser parser = new OfficeParser();
        try {
            parser.parse(inputStream, handler, metadata, context);
        } catch (IOException | SAXException | TikaException e) {
            throw new RuntimeException(e);
        }
        return handler.toString();
    }

}