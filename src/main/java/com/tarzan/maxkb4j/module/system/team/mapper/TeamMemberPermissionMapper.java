package com.tarzan.maxkb4j.module.system.team.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tarzan.maxkb4j.module.system.team.entity.TeamMemberPermissionEntity;
import com.tarzan.maxkb4j.module.system.team.vo.MemberPermissionVO;

import java.util.List;
import java.util.UUID;

/**
 * @author tarzan
 * @date 2024-12-27 14:06:50
 */
public interface TeamMemberPermissionMapper extends BaseMapper<TeamMemberPermissionEntity>{

   List<MemberPermissionVO> getPermissionByMemberId(UUID teamId, UUID memberId);
}
