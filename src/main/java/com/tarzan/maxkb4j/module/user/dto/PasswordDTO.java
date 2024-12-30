package com.tarzan.maxkb4j.module.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PasswordDTO {
    private String password;
    @JsonProperty("re_password")
    private String rePassword;

    public static void main(String[] args) {
        System.out.println();
    }
}
