package com.tarzan.maxkb4j.module.system.user.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@AllArgsConstructor
public class UserDTO {
    private String id;
    private String username;
    private String nickname;
    private String email;
    private Boolean isActive;

}
