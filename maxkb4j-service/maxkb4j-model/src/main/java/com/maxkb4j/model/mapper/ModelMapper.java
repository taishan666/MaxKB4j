package com.maxkb4j.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.maxkb4j.model.dto.ModelQuery;
import com.maxkb4j.model.entity.ModelEntity;
import com.maxkb4j.model.vo.ModelVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


/**
 * @author tarzan
 * @date 2024-12-25 12:22:22
 */
@Mapper
public interface ModelMapper extends BaseMapper<ModelEntity>{

    List<ModelVO> models(@Param("query") ModelQuery query);

    List<ModelVO> modelList(@Param("query") ModelQuery query);
}
