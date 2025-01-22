package com.tarzan.maxkb4j.module.dataset.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.common.dto.QueryDTO;
import com.tarzan.maxkb4j.module.dataset.entity.DocumentEntity;
import com.tarzan.maxkb4j.module.dataset.mapper.DocumentMapper;
import com.tarzan.maxkb4j.module.dataset.vo.DocumentVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-25 17:00:26
 */
@Service
public class DocumentService extends ServiceImpl<DocumentMapper, DocumentEntity>{

    @Autowired
    private ParagraphService paragraphService;

    public IPage<DocumentVO> selectDocPage(Page<DocumentVO> docPage, String datasetId, QueryDTO query) {
        return baseMapper.selectDocPage(docPage, datasetId,query);
    }

    public void updateStatusMetaById(String id){
        baseMapper.updateStatusMetaByIds(List.of(id));
    }

    //type 1向量化 2 生成问题 3同步
    public void updateStatusById(String docId, int type,int status) {
        updateStatusByIds(List.of(docId),type,status);
    }
    public void updateStatusByIds(List<String> ids, int type,int status) {
        baseMapper.updateStatusByIds(ids,type,status,type-1,type+1);
    }

    public void updateStatusMetaByIds(List<String> ids) {
        baseMapper.updateStatusMetaByIds(ids);
    }
}
