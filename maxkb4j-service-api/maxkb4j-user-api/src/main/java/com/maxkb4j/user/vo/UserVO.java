package com.maxkb4j.user.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
public class UserVO  {

    private String id;
    private String email;
    private String phone;
    private String nickname;
    private String username;
    private Set<String> role;
    private Set<String> roleName;
    private Boolean isActive;
    private String source;
    private String language;
    private List<String> permissions;
    private Boolean isEditPassword;
    private List<Map<String, String>> workspaceList;
}
