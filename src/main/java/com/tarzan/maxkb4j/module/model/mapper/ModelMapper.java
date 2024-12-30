package com.tarzan.maxkb4j.module.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tarzan.maxkb4j.module.model.entity.ModelEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.UUID;

/**
 * @author tarzan
 * @date 2024-12-25 12:22:22
 */
@Mapper
public interface ModelMapper extends BaseMapper<ModelEntity>{

}
