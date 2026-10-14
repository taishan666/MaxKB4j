package com.maxkb4j.system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetPasswordDTO {
    @NotBlank(message = "验证码不能为空")
    private String code;
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    @NotBlank(message = "类型不能为空")
    private String type;
    @NotBlank(message = "密码不能为空")
    private String password;
    @NotBlank(message = "确认密码不能为空")
    private String rePassword;
    /**
     * 修改密码时验证旧口令，防止未授权修改
     */
    private String oldPassword;
}
