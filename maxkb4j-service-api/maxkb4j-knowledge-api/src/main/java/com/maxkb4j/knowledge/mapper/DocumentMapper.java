package com.maxkb4j.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.maxkb4j.knowledge.dto.DocQuery;
import com.maxkb4j.knowledge.entity.DocumentEntity;
import com.maxkb4j.knowledge.vo.DocumentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-25 17:00:26
 */
@Mapper
public interface DocumentMapper extends BaseMapper<DocumentEntity>{

    IPage<DocumentVO> selectDocPage(Page<DocumentVO> docPage, String knowledgeId, @Param("query") DocQuery query);

    void updateStatusByIds(List<String> ids, int type, int status);

    void updateStatusMetaByIds(List<String> ids);

    boolean updateCharLengthById(String id);

}
