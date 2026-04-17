package com.maxkb4j.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.folder.entity.FolderEntity;
import com.maxkb4j.folder.mapper.FolderMapper;
import com.maxkb4j.folder.service.IFolderService;
import com.maxkb4j.folder.vo.FolderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class FolderServiceImpl extends ServiceImpl<FolderMapper, FolderEntity> implements IFolderService {

    public List<FolderVO> tree(String source) {
        return List.of(new FolderVO("default", ""));
    }

}
