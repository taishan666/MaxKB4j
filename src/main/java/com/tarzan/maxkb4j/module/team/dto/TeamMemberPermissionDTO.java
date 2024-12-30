package com.tarzan.maxkb4j.module.team.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.module.team.vo.TeamMemberPermissionVO;
import lombok.Data;

import java.util.List;

@Data
public class TeamMemberPermissionDTO {

    @JsonProperty("team_member_permission_list")
    private List<TeamMemberPermissionVO> teamMemberPermissionList;
}
