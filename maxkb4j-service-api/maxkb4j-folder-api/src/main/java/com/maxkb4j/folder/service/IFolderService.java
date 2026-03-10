package com.maxkb4j.folder.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.folder.entity.FolderEntity;
import com.maxkb4j.folder.vo.FolderVO;

import java.util.List;

public interface IFolderService extends IService<FolderEntity> {

    List<FolderVO> tree(String source);
}
