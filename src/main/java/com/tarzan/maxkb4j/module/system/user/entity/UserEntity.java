package com.tarzan.maxkb4j.module.system.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author tarzan
 * @date 2024-12-25 11:27:27
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("user")
public class UserEntity extends BaseEntity {

    private String email;

    private String phone;

    @JsonProperty("nick_name")
    private String nickName;

    private String username;

    private String password;

    private String role;

    @JsonProperty("is_active")
    private Boolean isActive;

    private String source;
} 
