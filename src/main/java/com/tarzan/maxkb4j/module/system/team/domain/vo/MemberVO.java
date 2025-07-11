package com.tarzan.maxkb4j.module.system.team.domain.vo;

import com.tarzan.maxkb4j.module.system.team.domain.entity.TeamMemberEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class MemberVO extends TeamMemberEntity {

    private String type;
    private String username;
    private String email;
}
