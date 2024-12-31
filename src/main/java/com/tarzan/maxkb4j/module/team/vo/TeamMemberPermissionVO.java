package com.tarzan.maxkb4j.module.team.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.module.common.dto.MemberOperate;
import lombok.Data;

import java.util.UUID;

@Data
public class TeamMemberPermissionVO {

    private UUID id;
    private String name;
    private String type;
    private UUID userId;
    @JsonProperty("target_id")
    private UUID targetId;
    private UUID memberId;
    private MemberOperate operate;
}
