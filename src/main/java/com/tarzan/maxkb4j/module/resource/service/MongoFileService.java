package com.tarzan.maxkb4j.module.resource.service;


import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.tarzan.maxkb4j.core.workflow.dto.ChatFile;
import jakarta.activation.MimetypesFileTypeMap;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class MongoFileService {

    private final GridFsTemplate gridFsTemplate;

    public ChatFile uploadFile(MultipartFile file) throws IOException {
        ChatFile fileVO = new ChatFile();
        // 新文件名
        String originalFilename = file.getOriginalFilename();
        fileVO.setName(originalFilename);
        fileVO.setSize(file.getSize());
        // 获得文件类型
        String contentType = file.getContentType();
        fileVO.setType(contentType);
        // 将文件存储到mongodb中,mongodb将会返回这个文件的具体信息
        // 上传文件中我们也可以使用DBObject附加一些属性
        // 获得文件输入流
        InputStream ins = file.getInputStream();
        DBObject metadata = new BasicDBObject();
        ObjectId objectId = gridFsTemplate.store(ins, originalFilename, contentType, metadata);
        fileVO.setFileId(objectId.toString());
        fileVO.setUploadTime(new Date());
        fileVO.setUrl("/api/file/" + objectId);
        return fileVO;
    }

    public ChatFile uploadFile(String fileName, byte[] fileBytes)  {
        ChatFile fileVO = new ChatFile();
        InputStream ins = new ByteArrayInputStream(fileBytes);
        // 新文件名
        fileVO.setName(fileName);
        fileVO.setSize((long) fileBytes.length);
        String contentType = new MimetypesFileTypeMap().getContentType(fileName);
        // 获得文件类型
        fileVO.setType(contentType);
        // 将文件存储到mongodb中,mongodb将会返回这个文件的具体信息
        // 上传文件中我们也可以使用DBObject附加一些属性
        // 获得文件输入流
        DBObject metadata = new BasicDBObject();
        ObjectId objectId = gridFsTemplate.store(ins, fileName, null, metadata);
        fileVO.setFileId(objectId.toString());
        fileVO.setUploadTime(new Date());
        fileVO.setUrl("/api/file/" + objectId);
        return fileVO;
    }

    public List<GridFSFile> list() {
        return  gridFsTemplate.find(new Query()).into(new ArrayList<>());
    }

    public void removeById(String fileId) {
        gridFsTemplate.delete(Query.query(Criteria.where("_id").is(fileId)));
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

    public InputStream getStream(GridFSFile file) throws IOException {
        GridFsResource resource = gridFsTemplate.getResource(file);
        return resource.getInputStream();
    }
}


