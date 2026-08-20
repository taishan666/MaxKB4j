package com.maxkb4j.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建用户入参：仅包含客户端可提供的字段，
 * 角色 / 状态 / 来源等由服务端强制设置，防止越权赋值。
 *
 * @author tarzan
 */
@Data
public class UserCreateDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    private String email;

    private String phone;

    private String nickname;

    private String language;
}
