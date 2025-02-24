package com.tarzan.maxkb4j.module.file.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.file.entity.FileEntity;
import com.tarzan.maxkb4j.module.file.mapper.FileMapper;
import com.tarzan.maxkb4j.module.file.vo.FileVO;
import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * @author tarzan
 * @date 2025-01-21 09:34:51
 */
@Service
@AllArgsConstructor
public class FileService extends ServiceImpl<FileMapper, FileEntity>{

    private final JdbcTemplate jdbcTemplate;


    public String uploadAudio(byte[] bytes) {
        try {
            // 确保上传目录存在
            String uploadDir = "uploads";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            String fileName =UUID.randomUUID()+".mp3";
            // 构建文件路径
            Path filePath = uploadPath.resolve(fileName);
            File file = filePath.toFile();

            // 保存文件
            FileCopyUtils.copy(bytes, file);

            // 生成访问URL
            return "http://127.0.0.1:8080/"+uploadDir+"/"+fileName;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to upload audio file", e);
        }
    }

    public FileVO uploadFile(MultipartFile file) {
        FileVO vo=new FileVO();
        String sql = "SELECT lo_from_bytea(?, ?::bytea) AS loid";
        int loid = 0;
        try {
            loid = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> rs.getInt("loid"), 0, file.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        FileEntity fileEntity=new FileEntity();
        fileEntity.setFileName(file.getOriginalFilename());
        fileEntity.setLoid(loid);
        fileEntity.setMeta(new JSONObject());
        save(fileEntity);
        vo.setFileId(fileEntity.getId());
        vo.setName(file.getOriginalFilename());
        vo.setUrl("/api/file/"+fileEntity.getId());
        return vo;
    }

    public byte[] getBytes(String id) {
        FileEntity file=getById(id);
        String sql = "SELECT lo_get(?) AS data";
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> rs.getBytes("data"), file.getLoid());
    }

}
