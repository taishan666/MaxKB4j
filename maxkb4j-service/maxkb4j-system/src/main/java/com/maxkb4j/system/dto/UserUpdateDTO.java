package com.maxkb4j.system.dto;

import lombok.Data;

/**
 * 管理员更新用户入参：仅包含可编辑字段；
 * 口令修改走专用接口，id 来自路径参数。
 *
 * @author tarzan
 */
@Data
public class UserUpdateDTO {

    private String email;

    private String phone;

    private String nickname;

    private String role;

    private Boolean isActive;

    private String language;
}
