package com.tarzan.maxkb4j.module.dataset.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.common.dto.QueryDTO;
import com.tarzan.maxkb4j.module.dataset.vo.DocumentVO;
import org.springframework.stereotype.Service;
import com.tarzan.maxkb4j.module.dataset.mapper.DocumentMapper;
import com.tarzan.maxkb4j.module.dataset.entity.DocumentEntity;

import java.util.UUID;

/**
 * @author tarzan
 * @date 2024-12-25 17:00:26
 */
@Service
public class DocumentService extends ServiceImpl<DocumentMapper, DocumentEntity>{

    public IPage<DocumentVO> selectDocPage(Page<DocumentVO> docPage, UUID datasetId, QueryDTO query) {
        return baseMapper.selectDocPage(docPage, datasetId,query);
    }
}
