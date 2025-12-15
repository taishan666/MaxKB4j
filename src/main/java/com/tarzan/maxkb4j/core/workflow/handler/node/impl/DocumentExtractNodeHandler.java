package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.tarzan.maxkb4j.core.workflow.annotation.NodeHandlerType;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.model.ChatFile;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import com.tarzan.maxkb4j.core.workflow.node.impl.DocumentExtractNode;
import com.tarzan.maxkb4j.core.workflow.model.NodeResult;
import com.tarzan.maxkb4j.module.oss.service.MongoFileService;
import lombok.RequiredArgsConstructor;
import org.apache.tika.exception.TikaException;
import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.microsoft.OfficeParserConfig;
import org.apache.tika.sax.ContentHandlerDecorator;
import org.springframework.stereotype.Component;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@NodeHandlerType(NodeType.DOCUMENT_EXTRACT)
@RequiredArgsConstructor
@Component
public class DocumentExtractNodeHandler implements INodeHandler {

    private final MongoFileService fileService;

    @SuppressWarnings("unchecked")
    @Override
    public NodeResult execute(Workflow workflow, INode node) throws Exception {
        String splitter = "\n-----------------------------------\n";
        DocumentExtractNode.NodeParams nodeParams=node.getNodeData().toJavaObject(DocumentExtractNode.NodeParams.class);
        List<String> documentList=nodeParams.getDocumentList();
        List<String> content=new LinkedList<>();
        Object res=workflow.getReferenceField(documentList.get(0),documentList.get(1));
        List<ChatFile> documents= res==null?List.of():(List<ChatFile>) res;
        for (ChatFile chatFile : documents) {
            byte[] data= fileService.getBytes(chatFile.getFileId());
            // 初始化解析器、元数据和上下文
            Parser parser = new AutoDetectParser();
            Metadata metadata = new Metadata();
            ParseContext parseContext = new ParseContext();
            OfficeParserConfig officeParserConfig = new OfficeParserConfig();
            //忽略页眉页脚
            officeParserConfig.setIncludeHeadersAndFooters(false);
            parseContext.set(OfficeParserConfig.class, officeParserConfig);
            Map<String,String> imageMap=new LinkedHashMap<>();
            // 自定义ContentHandler用于插入占位符
            class MarkdownImageHandler extends ContentHandlerDecorator {
                private final StringBuilder markdown = new StringBuilder();

                private String localName=null;

                @Override
                public void characters(char[] ch, int start, int length) {
                    String text= new String(ch, start, length);
                    System.err.println("localName="+localName+"  text="+text);
                    if(this.localName.equals("h1")){
                        markdown.append("# ").append(text).append("\n");
                    }else {
                        markdown.append(text);
                    }
                }

                @Override
                public void startElement(String uri, String localName, String qName, Attributes attrs) {
                    this.localName=localName;
                    if ("img".equals(localName)) { // 捕获图片节点
                        String src = attrs.getValue("src");
                        if (src != null && src.startsWith("embedded:")) {
                            String imageName = src.split(":")[1];
                            ChatFile image=fileService.uploadFile(imageName, new byte[0]);
                            imageMap.put(imageName, image.getFileId());
                            markdown.append("\n").append("![").append(imageName).append("](").append(image.getUrl()).append(")\n");
                        }
                    }
                }

                public String getMarkdown() {
                    return markdown.toString();
                }

            }
            MarkdownImageHandler contentHandler = new MarkdownImageHandler();
            EmbeddedDocumentExtractor extractor=new EmbeddedDocumentExtractor() {
                @Override
                public boolean shouldParseEmbedded(Metadata metadata) {
                    // 只处理图片类型
                    return metadata.get(Metadata.CONTENT_TYPE) != null &&
                            metadata.get(Metadata.CONTENT_TYPE).startsWith("image/");
                }

                @Override
                public void parseEmbedded(InputStream inputStream, ContentHandler embeddedHandler, Metadata metadata, boolean b) {
                    String fileName = metadata.get("resourceName");
                    String fileId=imageMap.get(fileName);
                    fileService.updateFile(fileId,inputStream);
                }
            };
            parseContext.set(EmbeddedDocumentExtractor.class, extractor);
            // 开始解析文档
            try {
                parser.parse(new ByteArrayInputStream(data), contentHandler, metadata, parseContext);
            } catch (IOException | SAXException | TikaException e) {
                throw new RuntimeException(e);
            }
            String text = "### "+chatFile.getName()+"\n"+contentHandler.getMarkdown()+splitter;
            content.add(text);
        }
        return new NodeResult(Map.of("content",String.join(splitter, content),"documentList",documents));
    }
}
