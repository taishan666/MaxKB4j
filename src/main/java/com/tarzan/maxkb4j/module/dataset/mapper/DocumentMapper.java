package com.tarzan.maxkb4j.module.dataset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tarzan.maxkb4j.core.common.dto.Query;
import com.tarzan.maxkb4j.module.dataset.domain.entity.DocumentEntity;
import com.tarzan.maxkb4j.module.dataset.domain.vo.DocumentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-25 17:00:26
 */
@Mapper
public interface DocumentMapper extends BaseMapper<DocumentEntity>{

    IPage<DocumentVO> selectDocPage(Page<DocumentVO> docPage, String datasetId,@Param("query") Query query);

    void updateStatusMetaById(String id);

    void updateStatusById(String id, int type, int status, int up, int next);

    boolean updateCharLengthById(String id);

    void updateStatusMetaByIds(List<String> ids);

    void updateStatusByIds(List<String> ids, int type, int status, int up, int next);
}
