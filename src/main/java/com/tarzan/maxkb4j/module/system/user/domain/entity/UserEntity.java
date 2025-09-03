package com.tarzan.maxkb4j.module.system.user.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.core.common.entity.BaseEntity;
import com.tarzan.maxkb4j.core.handler.type.StringSetTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Set;

/**
 * @author tarzan
 * @date 2024-12-25 11:27:27
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "user",autoResultMap = true)
public class UserEntity extends BaseEntity {

    private String email;

    private String phone;

    private String nickname;

    private String username;

    private String password;
    @TableField(typeHandler = StringSetTypeHandler.class)
    private Set<String> role;

    private Boolean isActive;

    private String source;

    private String language;


} 
