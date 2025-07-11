package com.tarzan.maxkb4j.module.system.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tarzan.maxkb4j.module.system.user.domain.entity.UserEntity;
import com.tarzan.maxkb4j.module.system.user.domain.vo.PermissionVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;


/**
 * @author tarzan
 * @date 2024-12-25 11:27:27
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity>{
    List<PermissionVO> getUserPermissionById(String userId);
}
