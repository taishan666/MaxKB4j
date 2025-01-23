package com.tarzan.maxkb4j.module.system.team.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.system.team.entity.TeamMemberPermissionEntity;
import com.tarzan.maxkb4j.module.system.team.mapper.TeamMemberPermissionMapper;
import com.tarzan.maxkb4j.module.system.team.vo.MemberPermissionVO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-27 14:06:50
 */
@Service
public class TeamMemberPermissionService extends ServiceImpl<TeamMemberPermissionMapper, TeamMemberPermissionEntity>{
   public List<MemberPermissionVO> getPermissionByMemberId(String teamId, String memberId){
       return baseMapper.getPermissionByMemberId(teamId, memberId);
   }

    public List<String> getUseTargets(String type,String userId) {
        return baseMapper.getUseTargets(userId,"USE", type);
    }
}
