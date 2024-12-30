package com.tarzan.maxkb4j.module.team.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.module.team.entity.TeamMemberEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class MemberVO extends TeamMemberEntity {

    private String type;
    private String username;
    private String email;
}
