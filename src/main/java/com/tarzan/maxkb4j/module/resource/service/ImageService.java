package com.tarzan.maxkb4j.module.resource.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import com.tarzan.maxkb4j.module.resource.mapper.ImageMapper;
import com.tarzan.maxkb4j.module.resource.entity.ImageEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * @author tarzan
 * @date 2025-01-21 09:35:03
 */
@Service
public class ImageService extends ServiceImpl<ImageMapper, ImageEntity>{

    public String upload(MultipartFile file) {
        ImageEntity image=new ImageEntity();
        try {
            image.setImage(file.getBytes());
            image.setImageName(file.getOriginalFilename());
            save(image);
            return "/api/image/"+image.getId();
        } catch (IOException e) {
            log.error("图片上传失败",e);
        }
        return "";
    }
}
