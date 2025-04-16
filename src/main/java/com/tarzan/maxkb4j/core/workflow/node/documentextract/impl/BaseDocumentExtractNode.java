package com.tarzan.maxkb4j.core.workflow.node.documentextract.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.dto.ChatFile;
import com.tarzan.maxkb4j.core.workflow.node.documentextract.input.DocumentExtractParams;
import com.tarzan.maxkb4j.module.resource.service.MongoFileService;
import com.tarzan.maxkb4j.util.SpringUtil;
import org.apache.tika.exception.TikaException;
import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.microsoft.OfficeParserConfig;
import org.apache.tika.sax.ContentHandlerDecorator;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.DOCUMENT_EXTRACT;

public class BaseDocumentExtractNode extends INode {

    private final MongoFileService fileService;

    public BaseDocumentExtractNode() {
        super();
        this.type = DOCUMENT_EXTRACT.getKey();
        this.fileService = SpringUtil.getBean(MongoFileService.class);
    }

    String splitter = "\n-----------------------------------\n";

    @Override
    public NodeResult execute() {
        // 假设我们有一个 Supplier<ContentHandler>
        DocumentExtractParams nodeParams=super.nodeParams.toJavaObject(DocumentExtractParams.class);
        List<String> documentList=nodeParams.getDocumentList();
        Object res=super.getWorkflowManage().getReferenceField(documentList.get(0),documentList.subList(1,documentList.size()));
        List<ChatFile> documents= (List<ChatFile>) res;
        StringBuilder sb=new StringBuilder();
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
                    if(this.localName.equals("h1")){
                       // markdown.append("# ").append(text);
                    }else if(this.localName.equals("p")){
                        markdown.append("\n").append(text);
                    }else {
                        markdown.append(text);
                    }
                }

                @Override
                public void startElement(String uri, String localName, String qName, Attributes attrs) {
                    this.localName=localName;
                    //  System.out.println("localName="+localName+"  qName="+qName+"  text="+text);
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
                public void parseEmbedded(InputStream inputStream, ContentHandler embeddedHandler, Metadata metadata, boolean b) throws IOException, SAXException {
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
            System.out.println(text);
            sb.append(text);
        }
        return new NodeResult(Map.of("content",sb.toString()),Map.of());
    }


    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        String content= (String) context.get("content");
        String[] fileContent= content.split(splitter);
        for (int i = 0; i < fileContent.length; i++) {
            String text = fileContent[i];
            int endIndex=Math.min(500,text.length());
            fileContent[i]= text.substring(0, endIndex);
        }
        detail.put("content",fileContent);
        return detail;
    }

}
