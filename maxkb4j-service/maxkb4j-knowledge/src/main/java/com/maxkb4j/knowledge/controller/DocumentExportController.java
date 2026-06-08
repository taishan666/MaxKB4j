package com.maxkb4j.knowledge.controller;

import com.maxkb4j.common.annotation.SaCheckPerm;
import com.maxkb4j.common.api.R;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.enums.PermissionEnum;
import com.maxkb4j.knowledge.service.DocumentExportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-25 16:00:15
 */
@RestController
@RequestMapping(AppConst.ADMIN_WORKSPACE_API)
@RequiredArgsConstructor
public class DocumentExportController {

    private final DocumentExportService documentExportService;


    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_EXPORT)
    @GetMapping("/knowledge/{id}/document/{docId}/export")
    public void export(@PathVariable("id") String id, @PathVariable("docId") String docId, HttpServletResponse response) {
        documentExportService.exportExcelByDocId(docId, response);
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_EXPORT)
    @PostMapping("/knowledge/{id}/document/batch_export")
    public void batchExport(@PathVariable("id") String id, @RequestBody List<String> docIds, HttpServletResponse response) throws IOException {
        documentExportService.exportExcelByDocIds(id,docIds, response);
    }


    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_EXPORT)
    @GetMapping("/knowledge/{id}/document/{docId}/export_zip")
    public void exportZip(@PathVariable("id") String id, @PathVariable("docId") String docId, HttpServletResponse response) throws IOException {
        documentExportService.exportExcelZipByDocId(docId, response);
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_EXPORT)
    @PostMapping("/knowledge/{id}/document/batch_export_zip")
    public void batchExportZip(@PathVariable("id") String id, @RequestBody List<String> docIds, HttpServletResponse response) throws IOException {
        documentExportService.exportExcelZipByDocIds(id,docIds, response);
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_DOWNLOAD)
    @GetMapping("/knowledge/{id}/document/{docId}/download_source_file")
    public R<String> downloadSourceFile(@PathVariable String id, @PathVariable String docId, HttpServletResponse response) throws IOException {
        boolean flag = documentExportService.downloadSourceFile(docId, response);
        return flag ? R.success() : R.fail("文件不存在, 仅支持手动上传的文档");
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_REPLACE)
    @PostMapping("/knowledge/{id}/document/{docId}/replace_source_file")
    public R<String> replaceSourceFile(@PathVariable String id, @PathVariable String docId, MultipartFile file) throws IOException {
        boolean flag = documentExportService.replaceSourceFile(id,docId,file);
        return flag ? R.success() : R.fail("文件不存在, 仅支持手动上传的文档");
    }

}
