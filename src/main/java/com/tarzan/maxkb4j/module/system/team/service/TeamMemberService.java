package com.tarzan.maxkb4j.module.system.team.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.system.team.dto.TeamMemberPermissionDTO;
import com.tarzan.maxkb4j.module.system.team.entity.TeamMemberEntity;
import com.tarzan.maxkb4j.module.system.team.entity.TeamMemberPermissionEntity;
import com.tarzan.maxkb4j.module.system.team.mapper.TeamMemberMapper;
import com.tarzan.maxkb4j.module.system.team.vo.MemberPermissionVO;
import com.tarzan.maxkb4j.module.system.team.vo.MemberVO;
import com.tarzan.maxkb4j.module.system.user.entity.UserEntity;
import com.tarzan.maxkb4j.module.system.user.mapper.UserMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author tarzan
 * @date 2024-12-25 12:42:39
 */
@Service
@AllArgsConstructor
public class TeamMemberService extends ServiceImpl<TeamMemberMapper, TeamMemberEntity>{

    private final UserMapper userMapper;
    private final TeamMemberPermissionService teamMemberPermissionService;

    public List<MemberVO> getByUserId(String userId) {
        List<MemberVO> result= new ArrayList<>();
        MemberVO manageMember = new MemberVO();
        UserEntity user= userMapper.selectById(userId);
        manageMember.setId(null);
        manageMember.setUserId(userId);
        manageMember.setTeamId(userId);
        manageMember.setUsername(user.getUsername());
        manageMember.setEmail(user.getEmail());
        manageMember.setType("manage");
        result.add(manageMember);
        List<MemberVO> members= baseMapper.getByUserId(userId);
        result.addAll(members);
        return result;
    }

    public boolean deleteByUserId(String userId) {
        return this.lambdaUpdate().eq(TeamMemberEntity::getUserId, userId).remove();
    }

    public boolean isExist(List<String> userIds) {
        long count=this.lambdaQuery().in(TeamMemberEntity::getUserId, userIds).count();
        return count>0;
    }

    @Transactional
    public boolean addBatchTeamMember(List<String> userIds,String manageUserId) {
        if(CollectionUtils.isEmpty(userIds)) {
            return false;
        }
        List<TeamMemberEntity> teamMemberEntities=userIds.stream().map(userId->{
            TeamMemberEntity teamMemberEntity=new TeamMemberEntity();
            teamMemberEntity.setUserId(userId);
            teamMemberEntity.setTeamId(manageUserId);
            return teamMemberEntity;
        }).toList();
        return this.saveBatch(teamMemberEntities);
    }

    public Map<String, List<MemberPermissionVO>> getPermissionByMemberId(String memberId) {
        if("root".equals(memberId)) {
            List<MemberPermissionVO> list=teamMemberPermissionService.getPermissionByMemberId(StpUtil.getLoginIdAsString(),null);
            list.forEach(e->{
                e.getOperate().setManage(true);
                e.getOperate().setUse(true);
            });
            return list.stream().collect(Collectors.groupingBy(MemberPermissionVO::getType));
        }else {
            TeamMemberEntity entity=this.getById(memberId);
            List<MemberPermissionVO> list=teamMemberPermissionService.getPermissionByMemberId(entity.getTeamId(),entity.getId());
            return list.stream().collect(Collectors.groupingBy(MemberPermissionVO::getType));
        }
    }



    @Transactional
    public Map<String, List<MemberPermissionVO>> updateTeamMemberById(String teamMemberId, TeamMemberPermissionDTO dto) {
        List<MemberPermissionVO> permissions=dto.getTeamMemberPermissionList();
        if(!CollectionUtils.isEmpty(permissions)) {
            teamMemberPermissionService.remove(Wrappers.<TeamMemberPermissionEntity>lambdaUpdate().eq(TeamMemberPermissionEntity::getMemberId,teamMemberId));
            List<TeamMemberPermissionEntity> entities=permissions.stream().map(e->{
                TeamMemberPermissionEntity entity=new TeamMemberPermissionEntity();
                entity.setMemberId(teamMemberId);
                entity.setAuthTargetType(e.getType());
                entity.setOperate(e.getOperate());
                entity.setTarget(e.getTargetId());
                return entity;
            }).toList();
            teamMemberPermissionService.saveBatch(entities);
        }
        return getPermissionByMemberId(teamMemberId);
    }

}
