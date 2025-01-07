package com.tarzan.maxkb4j.module.dataset.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.common.dto.QueryDTO;
import com.tarzan.maxkb4j.module.dataset.entity.ParagraphEntity;
import com.tarzan.maxkb4j.module.dataset.vo.DocumentVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.tarzan.maxkb4j.module.dataset.mapper.DocumentMapper;
import com.tarzan.maxkb4j.module.dataset.entity.DocumentEntity;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * @author tarzan
 * @date 2024-12-25 17:00:26
 */
@Service
public class DocumentService extends ServiceImpl<DocumentMapper, DocumentEntity>{

    @Autowired
    private ParagraphService paragraphService;

    public IPage<DocumentVO> selectDocPage(Page<DocumentVO> docPage, UUID datasetId, QueryDTO query) {
        return baseMapper.selectDocPage(docPage, datasetId,query);
    }

    public List<ParagraphEntity> getParagraphsByDocIds(List<UUID> docIds) {
        if(!CollectionUtils.isEmpty(docIds)){
            return paragraphService.lambdaQuery().in(ParagraphEntity::getDocumentId,docIds).list();
        }
        return Collections.emptyList();
    }

    public void updateStatusMetaById(UUID id){
        baseMapper.updateStatusMetaById(id);
    }

    //type 1向量化 2 生成问题 3同步
    public void updateStatusById(UUID docId, int type,int status) {
        baseMapper.updateStatusById(docId,type,status,type-1,type+1);
    }

    public void updateStatusMetaByIds(List<UUID> ids) {
        baseMapper.updateStatusMetaByIds(ids);
    }
}
