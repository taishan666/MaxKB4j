package com.tarzan.maxkb4j.module.system.team.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.system.team.domain.entity.TeamEntity;
import com.tarzan.maxkb4j.module.system.team.mapper.TeamMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author tarzan
 * @date 2024-12-27 14:03:01
 */
@Service
@AllArgsConstructor
public class TeamService extends ServiceImpl<TeamMapper, TeamEntity> {

    private final TeamMemberService teamMemberService;

    @Transactional
    public boolean deleteUserById(String userId) {
        teamMemberService.deleteByUserId(userId);
        return this.lambdaUpdate().eq(TeamEntity::getUserId, userId).remove();
    }


}
