package com.maxkb4j.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.maxkb4j.common.mp.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

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

    @TableField(select = false)
    private String password;
    private String role;

    private Boolean isActive;

    private String source;

    private String language;


} 
