package com.maxkb4j.tool.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.maxkb4j.tool.dto.ToolQuery;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.tool.vo.ToolVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author tarzan
 * @date 2025-01-25 22:00:45
 */
public interface ToolMapper extends BaseMapper<ToolEntity>{

    IPage<ToolVO> pageList(IPage<ToolEntity> page,  @Param("query") ToolQuery query);

    List<ToolVO> listTools(@Param("query") ToolQuery query);
}
