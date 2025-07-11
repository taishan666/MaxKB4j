package com.tarzan.maxkb4j.module.system.team.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tarzan.maxkb4j.module.system.team.domain.entity.TeamMemberPermissionEntity;
import com.tarzan.maxkb4j.module.system.team.domain.vo.MemberPermissionVO;

import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-27 14:06:50
 */
public interface TeamMemberPermissionMapper extends BaseMapper<TeamMemberPermissionEntity>{

   List<MemberPermissionVO> getPermissionByMemberId(String teamId, String memberId);

   List<String> getUseTargets(String userId,String operate, String type);
}
