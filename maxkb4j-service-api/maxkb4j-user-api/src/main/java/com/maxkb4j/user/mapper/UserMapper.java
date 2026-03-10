package com.maxkb4j.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.maxkb4j.user.entity.UserEntity;
import com.maxkb4j.user.vo.PermissionVO;
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
