package com.maxkb4j.knowledge.service.impl;

import com.maxkb4j.common.exception.FileLimitExceededException;
import com.maxkb4j.knowledge.consts.KnowledgeType;
import com.maxkb4j.knowledge.dto.DocumentSimple;
import com.maxkb4j.knowledge.entity.DocumentEntity;
import com.maxkb4j.knowledge.entity.KnowledgeEntity;
import com.maxkb4j.knowledge.handler.DocumentHandler;
import com.maxkb4j.knowledge.mapper.KnowledgeMapper;
import com.maxkb4j.knowledge.service.DocumentWriteService;
import com.maxkb4j.knowledge.service.IDocumentInternalService;
import com.maxkb4j.knowledge.service.IDocumentParseService;
import com.maxkb4j.knowledge.service.IDocumentSplitService;
import com.maxkb4j.knowledge.service.IDocumentWebService;
import com.maxkb4j.knowledge.util.FilePathSecurityUtil;
import com.maxkb4j.knowledge.vo.DocFileVO;
import com.maxkb4j.knowledge.vo.TextSegmentVO;
import com.maxkb4j.oss.service.IOssService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 文档导入服务：负责文件导入（QA / 表格）、Web 文档创建与同步、切分预览，
 * 自 {@link DocumentServiceImpl} 拆分而来。
 *
 * @author tarzan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentImportService {

    private final IDocumentInternalService documentService;
    private final IDocumentParseService documentParseService;
    private final IDocumentSplitService documentSpiltService;
    private final IOssService ossService;
    private final IDocumentWebService documentWebService;
    private final DocumentWriteService documentWriteService;
    private final DocumentHandler documentHandler;
    private final KnowledgeMapper knowledgeMapper;

    @Transactional(rollbackFor = Exception.class)
    public void importQa(String knowledgeId, MultipartFile[] files) throws IOException {
        if (checkFileLimit(knowledgeId, files)) {
            throw new FileLimitExceededException("common.file.limit.exceeded");
        }
        if (files == null) return;
        List<DocumentSimple> docs = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            String fileName = file.getOriginalFilename();
            if (fileName == null) continue;
            // 验证文件名安全性
            if (FilePathSecurityUtil.illegalityFileName(fileName)) {
                continue; // 跳过非法文件
            }
            if (fileName.toLowerCase().endsWith(".zip")) {
                docs.addAll(documentHandler.processZipQaFile(file));
            } else {
                docs.addAll(documentHandler.processQaFile(file.getBytes(), fileName));
            }
        }
        // 将解析的文档保存到数据库
        if (!docs.isEmpty()) {
            documentWriteService.batchCreateDocs(knowledgeId, KnowledgeType.BASE, docs);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void importTable(String knowledgeId, MultipartFile[] files) throws IOException {
        if (checkFileLimit(knowledgeId, files)) {
            throw new FileLimitExceededException("common.file.limit.exceeded");
        }
        if (files == null) return;
        List<DocumentSimple> docs = new ArrayList<>();
        for (MultipartFile uploadFile : files) {
            if (uploadFile == null || uploadFile.isEmpty()) continue;
            String originalFilename = uploadFile.getOriginalFilename();
            if (originalFilename == null) continue;

            // 验证文件名安全性
            if (FilePathSecurityUtil.illegalityFileName(originalFilename)) {
                continue; // 跳过非法文件
            }

            docs.addAll(documentHandler.processTable(uploadFile.getBytes(), originalFilename));
        }
        // 将解析的文档保存到数据库
        if (!docs.isEmpty()) {
            documentWriteService.batchCreateDocs(knowledgeId, KnowledgeType.BASE, docs);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void createWebDoc(String knowledgeId, List<String> sourceUrlList, String selector) {
        for (String sourceUrl : sourceUrlList) {
            List<DocumentSimple> docs = documentWebService.getWebDocuments(sourceUrl, selector, false);
            documentWriteService.batchCreateDocs(knowledgeId, KnowledgeType.WEB, docs);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void syncWebDoc(String knowledgeId, String docId) {
        DocumentEntity doc = documentService.getById(docId);
        if (doc == null || doc.getMeta() == null) return;
        String sourceUrl = doc.getMeta().getString("sourceUrl");
        String selector = doc.getMeta().getString("selector");
        if (StringUtils.isAnyBlank(sourceUrl, selector)) return;
        documentService.deleteDocByIds(knowledgeId, List.of(docId));
        List<DocumentSimple> docs = documentWebService.getWebDocuments(sourceUrl, selector, false);
        documentWriteService.batchCreateDocs(knowledgeId, KnowledgeType.WEB, docs);
    }

    public List<TextSegmentVO> split(String knowledgeId, MultipartFile[] files, String[] patterns, Integer limit, Boolean withFilter) throws IOException {
        if (checkFileLimit(knowledgeId, files)) {
            throw new FileLimitExceededException("common.file.limit.exceeded");
        }
        List<TextSegmentVO> result = new ArrayList<>();
        List<DocFileVO> fileStreams = new ArrayList<>();
        if (files == null) return result;
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            String name = file.getOriginalFilename();
            if (name == null) continue;
            // 验证文件名安全性
            if (FilePathSecurityUtil.illegalityFileName(name)) {
                log.warn("非法的文件名: {}", name);
                continue; // 跳过非法文件
            }
            if (name.toLowerCase().endsWith(".zip")) {
                try (ZipArchiveInputStream zis = new ZipArchiveInputStream(file.getInputStream())) {
                    ZipArchiveEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        if (!entry.isDirectory()) {
                            // 验证压缩包内文件名的安全性
                            String entryName = FilePathSecurityUtil.normalizeFilePath(entry.getName());
                            if (entryName == null) {
                                log.warn("压缩包中存在非法的文件路径: {}", entry.getName());
                                continue; // 跳过非法文件
                            }
                            try {
                                byte[] bytes = zis.readAllBytes();
                                fileStreams.add(new DocFileVO(entryName, bytes, ""));
                            } catch (java.io.EOFException e) {
                                log.warn("压缩包中文件 {} 读取不完整，已跳过: {}", entryName, e.getMessage());
                            }
                        }
                    }
                } catch (java.io.EOFException e) {
                    log.warn("ZIP文件 {} 格式异常或已损坏，部分文件可能未读取: {}", name, e.getMessage());
                }
            } else {
                fileStreams.add(new DocFileVO(name, file.getBytes(), file.getContentType()));
            }
        }
        for (DocFileVO fs : fileStreams) {
            TextSegmentVO vo = new TextSegmentVO();
            vo.setName(fs.getName());
            String fileId = ossService.storeFile(fs.getBytes(), fs.getName(), fs.getContentType());
            String text = documentParseService.extractText(fs.getName(), new ByteArrayInputStream(fs.getBytes()));
            vo.setContent(documentSpiltService.split(text, patterns, limit, withFilter));
            vo.setSourceFileId(fileId);
            result.add(vo);
        }
        return result;
    }

    private boolean checkFileLimit(String id, MultipartFile[] files) {
        KnowledgeEntity knowledge = knowledgeMapper.selectById(id);
        if (Objects.isNull(knowledge)) {
            return false;
        }
        int fileSizeLimit = knowledge.getFileSizeLimit();
        int fileCountLimit = knowledge.getFileCountLimit();
        // 检查文件数量
        if (files == null || files.length == 0) {
            return false;
        }
        if (files.length > fileCountLimit) {
            return true;
        }
        // 预计算字节上限（避免循环内重复计算）
        long fileSizeLimitBytes = (long) fileSizeLimit * 1024 * 1024;
        // 收集超限文件的序号（从1开始）
        List<Integer> overLimitIndices = new ArrayList<>();
        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            if (file != null && file.getSize() > fileSizeLimitBytes) {
                overLimitIndices.add(i + 1);
            }
        }
        // 若有超限文件，返回提示
        return !overLimitIndices.isEmpty();
    }
}
