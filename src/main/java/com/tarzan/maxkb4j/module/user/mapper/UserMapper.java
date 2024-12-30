package com.tarzan.maxkb4j.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tarzan.maxkb4j.module.user.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;


/**
 * @author tarzan
 * @date 2024-12-25 11:27:27
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity>{
}
