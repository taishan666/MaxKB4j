package com.tarzan.maxkb4j.module.system.user.domain.vo;

import com.tarzan.maxkb4j.module.system.user.domain.entity.UserEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserVO extends UserEntity {

    private List<String> permissions;
    private Boolean isEditPassword;
}
