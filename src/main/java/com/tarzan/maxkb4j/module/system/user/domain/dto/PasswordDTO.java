package com.tarzan.maxkb4j.module.system.user.domain.dto;

import lombok.Data;

@Data
public class PasswordDTO {
    private String code;
    private String password;
    private String rePassword;

}
