package com.tarzan.maxkb4j.module.system.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PasswordDTO {
    private String code;
    private String password;
    private String rePassword;

}
