package com.maxkb4j.user.dto;

import lombok.Data;

@Data
public class UserDTO {
    private String id;
    private String email;
    private String phone;
    private String nickname;
    private String username;
    private String password;
    private String role;
    private Boolean isActive;
    private String source;
    private String language;
    private String workspaceName;
}
