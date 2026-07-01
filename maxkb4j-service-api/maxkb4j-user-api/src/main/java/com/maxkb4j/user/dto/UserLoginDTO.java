package com.maxkb4j.user.dto;

import lombok.Data;
import lombok.NonNull;

@Data
public class UserLoginDTO {
    @NonNull
    private String username;
    private String password;
    private String encryptedData;
    private String captcha;
}
