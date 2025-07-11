package com.tarzan.maxkb4j.module.system.team.domain.dto;

import com.tarzan.maxkb4j.module.system.team.domain.vo.MemberPermissionVO;
import lombok.Data;

import java.util.List;

@Data
public class TeamMemberPermissionDTO {

    private List<MemberPermissionVO> teamMemberPermissionList;
}
