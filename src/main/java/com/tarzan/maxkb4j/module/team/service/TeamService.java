package com.tarzan.maxkb4j.module.team.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.team.entity.TeamEntity;
import com.tarzan.maxkb4j.module.team.mapper.TeamMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * @author tarzan
 * @date 2024-12-27 14:03:01
 */
@Service
public class TeamService extends ServiceImpl<TeamMapper, TeamEntity> {

    @Autowired
    private TeamMemberService teamMemberService;

    @Transactional
    public boolean deleteUserById(UUID userId) {
        boolean f1 = teamMemberService.deleteByUserId(userId);
        boolean f2 = this.lambdaUpdate().eq(TeamEntity::getUserId, userId).remove();
        return f1 && f2;
    }


}
