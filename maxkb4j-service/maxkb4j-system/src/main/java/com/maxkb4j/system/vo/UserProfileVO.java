package com.maxkb4j.system.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
public class UserProfileVO {

    private String id;
    private String email;
    private String phone;
    private String nickname;
    private String username;
    private Set<String> role;
    private Set<String> roles;
    private Set<String> roleName;
    private Boolean isActive;
    private String source;
    private String language;
    private List<String> permissions;
    // 是否需要编辑密码
    private Boolean isEditPassword;
    private List<Map<String, String>> workspaceList;
    private Date createTime;
}
