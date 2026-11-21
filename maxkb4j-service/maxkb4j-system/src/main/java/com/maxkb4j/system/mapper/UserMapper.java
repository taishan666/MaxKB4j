package com.maxkb4j.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.maxkb4j.system.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-25 11:27:27
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {

    /**
     * 统计用户名下自有资源数量（应用/知识库/模型/工具）。
     * <p>用于删除用户前校验：这些表以 user_id 记录归属，且 knowledge/tool 的
     * 外键为 NO ACTION，直接删除用户必然失败，名下资源需先行删除或转移。
     */
    long countOwnedResource(@Param("userId") String userId);

    /**
     * 删除指定用户创建的应用 API Key。
     * <p>application_api_key.user_id 外键引用用户（NO ACTION），删除用户前必须清理，
     * 否则触发外键约束异常。
     */
    int deleteApplicationApiKeyByUserIds(@Param("userIds") List<String> userIds);
}
