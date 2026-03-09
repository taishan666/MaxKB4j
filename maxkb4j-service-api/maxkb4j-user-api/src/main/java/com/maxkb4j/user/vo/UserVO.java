package com.maxkb4j.user.vo;

import com.maxkb4j.user.entity.UserEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserVO extends UserEntity {

    private List<String> permissions;
    private Boolean isEditPassword;
    private List<Map<String, String>> workspaceList;
}
