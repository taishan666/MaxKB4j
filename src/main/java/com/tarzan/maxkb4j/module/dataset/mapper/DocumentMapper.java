package com.tarzan.maxkb4j.module.dataset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tarzan.maxkb4j.common.dto.QueryDTO;
import com.tarzan.maxkb4j.module.dataset.entity.DocumentEntity;
import com.tarzan.maxkb4j.module.dataset.vo.DocumentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author tarzan
 * @date 2024-12-25 17:00:26
 */
@Mapper
public interface DocumentMapper extends BaseMapper<DocumentEntity>{

    IPage<DocumentVO> selectDocPage(Page<DocumentVO> docPage, String datasetId,@Param("query") QueryDTO query);

    void updateStatusMetaById(String id);

    void updateStatusById(String id, int type, int status, int up, int next);

    boolean updateCharLengthById(String id);
}
