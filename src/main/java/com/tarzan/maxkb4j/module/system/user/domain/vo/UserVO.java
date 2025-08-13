package com.tarzan.maxkb4j.module.system.user.domain.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.module.system.user.domain.entity.UserEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserVO extends UserEntity {

    private List<String> permissions;
    private Boolean isEditPassword;
    @JsonProperty("workspace_list")
    private List<Map<String, String>> workspaceList;
}
