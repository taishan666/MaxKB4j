package com.maxkb4j.user.dto;

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
