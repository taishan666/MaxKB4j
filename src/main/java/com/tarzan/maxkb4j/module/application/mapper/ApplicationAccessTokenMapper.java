package com.tarzan.maxkb4j.module.application.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tarzan.maxkb4j.module.application.entity.ApplicationAccessTokenEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.UUID;

/**
 * @author tarzan
 * @date 2024-12-25 18:05:58
 */
@Mapper
public interface ApplicationAccessTokenMapper extends BaseMapper<ApplicationAccessTokenEntity>{
}
