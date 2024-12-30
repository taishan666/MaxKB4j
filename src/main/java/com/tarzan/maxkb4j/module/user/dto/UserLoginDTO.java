package com.tarzan.maxkb4j.module.user.dto;

import lombok.Data;
import lombok.NonNull;

@Data
public class UserLoginDTO {
    @NonNull
    private String username;
    @NonNull
    private String password;
}
