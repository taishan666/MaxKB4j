package com.tarzan.maxkb4j.module.system.user.dto;

import lombok.Data;

@Data
public class ResetPasswordDTO {
    private String code;
    private String email;
    private String type;
    private String password;
    private String rePassword;
}
