package com.tarzan.maxkb4j.module.system.user.domain.dto;

import lombok.Data;
import lombok.NonNull;

@Data
public class UserLoginDTO {
    @NonNull
    private String username;
    @NonNull
    private String password;
    private String captcha;
}
