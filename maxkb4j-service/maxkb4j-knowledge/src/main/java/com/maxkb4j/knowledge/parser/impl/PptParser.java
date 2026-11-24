package com.maxkb4j.knowledge.parser.impl;

import com.maxkb4j.knowledge.parser.DocumentParser;
import lombok.RequiredArgsConstructor;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
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
        // 传入 -1 取消默认 10 万字符的写入上限，避免大文件解析失败
        ContentHandler handler = new BodyContentHandler(-1);
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();
        // OfficeParser 仅支持 OLE2 的 .ppt，.pptx 需走 OOXMLParser，这里用自动检测覆盖两者
        Parser parser = new AutoDetectParser();
        try {
            parser.parse(inputStream, handler, metadata, context);
        } catch (IOException | SAXException | TikaException e) {
            throw new RuntimeException(e);
        }
        return handler.toString();
    }

}