package com.tarzan.maxkb4j.module.oss.service;


import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.tarzan.maxkb4j.common.util.IoUtil;
import com.tarzan.maxkb4j.core.workflow.model.SysFile;
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
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MongoFileService {

    private final GridFsTemplate gridFsTemplate;

    public SysFile uploadFile(MultipartFile file) throws IOException {
        SysFile fileVO = new SysFile();
        // 新文件名
        fileVO.setName(file.getOriginalFilename());
        fileVO.setSize(file.getSize());
        // 获得文件类型
        fileVO.setType(file.getContentType());
        String fileId = storeFile(file);
        fileVO.setFileId(fileId);
        fileVO.setUploadTime(new Date());
        fileVO.setUrl("./oss/file/" + fileId);
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

    public SysFile uploadFile(String fileName, byte[] fileBytes)  {
        SysFile fileVO = new SysFile();
        InputStream ins = new ByteArrayInputStream(fileBytes);
        // 新文件名
        fileVO.setName(fileName);
        fileVO.setSize((long) fileBytes.length);
        String contentType = new MimetypesFileTypeMap().getContentType(fileName);
        // 获得文件类型
        fileVO.setType(contentType);
        DBObject metadata = new BasicDBObject();
        ObjectId objectId = gridFsTemplate.store(ins, fileName, contentType, metadata);
        fileVO.setFileId(objectId.toString());
        fileVO.setUploadTime(new Date());
        fileVO.setUrl("./oss/file/" + objectId);
        return fileVO;
    }

    public void getFile(String id, HttpServletResponse response) {
        try {
            GridFSFile file = this.getById(id);
            if (file == null || file.getLength() <= 0) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            Document doc = file.getMetadata();
            String contentType = (doc != null) ? doc.getString("_contentType") : "application/octet-stream";
            response.setContentType(contentType);

            boolean previewAble = contentType != null &&
                    (contentType.startsWith("image/") || "application/pdf".equals(contentType));

            if (previewAble) {
                response.setHeader("Content-Disposition", "inline");
            } else {
                String encodedFileName = URLEncoder.encode(file.getFilename(), StandardCharsets.UTF_8)
                        .replace("+", "%20");
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedFileName);
            }

            // ✅ 必须在 getOutputStream() 之前设置！
            response.setContentLengthLong(file.getLength());

            try (InputStream inputStream = this.getStream(file);
                 OutputStream outputStream = response.getOutputStream()) {

                if (inputStream == null) {
                    log.warn("Input stream is null for file ID: {}", id);
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }

                IoUtil.copy(inputStream, outputStream);
                outputStream.flush();
            }

        } catch (Exception e) {
            log.error("Error serving file with ID: {}", id, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    public List<GridFSFile> list() {
        return  gridFsTemplate.find(new Query()).into(new ArrayList<>());
    }

    public GridFSFile getById(String fileId) {
        return gridFsTemplate.findOne(new Query(Criteria.where("_id").is(fileId)));
    }

    public byte[] getBytes(String fileId) {
        GridFsResource resource = gridFsTemplate.getResource(getById(fileId));
        try {
            return resource.getContentAsByteArray();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return new byte[0];
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


