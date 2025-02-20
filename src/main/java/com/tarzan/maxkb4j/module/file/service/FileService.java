package com.tarzan.maxkb4j.module.file.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.file.entity.FileEntity;
import com.tarzan.maxkb4j.module.file.mapper.FileMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

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
public class FileService extends ServiceImpl<FileMapper, FileEntity>{


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

}
