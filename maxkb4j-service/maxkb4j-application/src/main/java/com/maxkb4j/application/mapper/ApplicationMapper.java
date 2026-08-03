package com.maxkb4j.application.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.maxkb4j.application.dto.ApplicationQuery;
import com.maxkb4j.application.entity.ApplicationEntity;
import com.maxkb4j.application.vo.ApplicationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author tarzan
 * @date 2024-12-25 13:09:54
 */
@Mapper
public interface ApplicationMapper extends BaseMapper<ApplicationEntity>{

    IPage<ApplicationVO> pageList(Page<ApplicationVO> page, @Param("query") ApplicationQuery query);
}
