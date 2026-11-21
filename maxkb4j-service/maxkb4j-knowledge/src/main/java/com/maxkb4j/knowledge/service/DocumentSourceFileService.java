package com.maxkb4j.knowledge.service;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.util.IoUtil;
import com.maxkb4j.knowledge.entity.DocumentEntity;
import com.maxkb4j.oss.service.IOssService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Component
@RequiredArgsConstructor
public class DocumentSourceFileService {

    private static final String META_SOURCE_FILE_ID = "sourceFileId";
    private static final String META_ALLOW_DOWNLOAD = "allow_download";

    private final IDocumentInternalService documentService;
    private final IOssService ossService;

    public boolean download(String knowledgeId, String docId, HttpServletResponse response) throws IOException {
        DocumentEntity doc = documentService.getByKnowledgeIdAndId(knowledgeId, docId);
        if (doc == null || doc.getMeta() == null) return false;

        String fileId = doc.getMeta().getString(META_SOURCE_FILE_ID);
        if (StringUtils.isBlank(fileId)) return false;

        try (InputStream in = ossService.getStream(fileId)) {
            IoUtil.copy(in, response.getOutputStream());
            return true;
        }
    }

    public boolean replace(String knowledgeId, String docId, MultipartFile file) throws IOException {
        DocumentEntity doc = documentService.getByKnowledgeIdAndId(knowledgeId, docId);
        if (doc == null) return false;
        String fileId = ossService.storeFile(file.getBytes(),file.getOriginalFilename(),file.getContentType());
        if (StringUtils.isBlank(fileId)) return false;
        // 在原 meta 上合并，保留 Web 文档的 sourceUrl/selector 等字段，避免 syncWebDoc 失效
        JSONObject meta = doc.getMeta() == null ? new JSONObject() : doc.getMeta();
        meta.put(META_SOURCE_FILE_ID, fileId);
        meta.put(META_ALLOW_DOWNLOAD, true);
        doc.setMeta(meta);
        return documentService.updateById(doc);
    }
}
