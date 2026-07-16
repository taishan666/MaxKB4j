package com.maxkb4j.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserLoginDTO {
    @NotBlank(message = "用户名不能为空")
    private String username;
    private String password;
    private String encryptedData;
    @NotBlank(message = "验证码不能为空")
    private String captcha;
}
