package com.maxkb4j.oss.service;


import com.maxkb4j.common.domain.dto.OssFile;
import com.maxkb4j.oss.support.FileResponseHelper;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.gridfs.model.GridFSFile;
import jakarta.activation.MimetypesFileTypeMap;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class MongoFileService implements IOssService{

    /** 文件访问 URL 前缀。 */
    private static final String FILE_URL_PREFIX = "./oss/file/";

    /** 文件类型解析器（线程安全，复用实例）。 */
    private static final MimetypesFileTypeMap MIME_TYPES = new MimetypesFileTypeMap();

    private final GridFsTemplate gridFsTemplate;

    /**
     * 根据文件 ID 构造访问 URL。
     */
    private static String buildFileUrl(String fileId) {
        return FILE_URL_PREFIX + fileId;
    }

    public String uploadAndGetFileUrl(MultipartFile file) throws IOException {
        String fileId = storeFile(file);
        return buildFileUrl(fileId);
    }

    public OssFile uploadFile(String fileName, byte[] fileBytes)  {
        OssFile fileVO = new OssFile();
        InputStream ins = new ByteArrayInputStream(fileBytes);
        fileVO.setName(fileName);
        fileVO.setSize((long) fileBytes.length);
        String contentType = MIME_TYPES.getContentType(fileName);
        DBObject metadata = new BasicDBObject();
        ObjectId objectId = gridFsTemplate.store(ins, fileName, contentType, metadata);
        fileVO.setFileId(objectId.toString());
        fileVO.setUrl(buildFileUrl(objectId.toString()));
        return fileVO;
    }


    public String storeFile(MultipartFile file) throws IOException {
        // 新文件名
        String originalFilename = file.getOriginalFilename();
        // 获得文件类型
        String contentType = file.getContentType();
        return storeFile(file.getBytes(),originalFilename,contentType);
    }

    public String storeFile(byte[] bytes,String fileName,String contentType) {
        ObjectId objectId =  gridFsTemplate.store(new ByteArrayInputStream(bytes), fileName, contentType);
        return objectId.toString();
    }


    public OssFile getFile(String id) {
        GridFSFile file = this.getById(id);
        if (file == null || file.getLength() <= 0){
            return null;
        }
        OssFile fileVO = new OssFile();
        String fileId = file.getId().toString();
        // 新文件名
        fileVO.setFileId(fileId);
        fileVO.setName(file.getFilename());
        fileVO.setSize(file.getLength());
        fileVO.setUrl(buildFileUrl(fileId));
        return fileVO;
    }

    public void downloadFile(String id, HttpServletResponse response) {
        try {
            GridFSFile file = this.getById(id);
            if (file == null || file.getLength() <= 0) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            Document doc = file.getMetadata();
            String contentType = (doc != null) ? doc.getString("_contentType") : null;
            // 响应头（Content-Type / Content-Disposition / Content-Length）必须在获取输出流之前完成
            FileResponseHelper.configureHeaders(response, file.getFilename(), contentType, file.getLength());

            try (InputStream inputStream = this.getStream(file)) {
                if (inputStream == null) {
                    log.warn("Input stream is null for file ID: {}", id);
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                FileResponseHelper.writeStream(response, inputStream);
            }

        } catch (Exception e) {
            log.error("Error serving file with ID: {}", id, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    public GridFSFile getById(String fileId) {
        return gridFsTemplate.findOne(new Query(Criteria.where("_id").is(fileId)));
    }

    public byte[] getBytes(String fileId) {
        GridFsResource resource = gridFsTemplate.getResource(getById(fileId));
        if (!resource.exists()) {
            log.error("File not found for ID: {}", fileId);
        }
        try {
            return resource.getContentAsByteArray();
        } catch (IOException e) {
            log.error("Failed to read file content for ID: {}", fileId, e);
            throw new IllegalStateException("Failed to read file content: " + fileId, e);
        }
    }

    public InputStream getStream(String fileId) throws IOException {
        GridFSFile file=getById(fileId);
        return getStream(file);
    }

    public InputStream getStream(GridFSFile file) throws IOException {
        GridFsResource resource = gridFsTemplate.getResource(file);
        return resource.getInputStream();
    }

}


