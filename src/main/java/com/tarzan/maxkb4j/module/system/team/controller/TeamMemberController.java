package com.tarzan.maxkb4j.module.system.team.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.tarzan.maxkb4j.module.system.team.dto.TeamMemberPermissionDTO;
import com.tarzan.maxkb4j.module.system.team.service.TeamMemberService;
import com.tarzan.maxkb4j.module.system.team.vo.MemberVO;
import com.tarzan.maxkb4j.module.system.team.vo.TeamMemberPermissionVO;
import com.tarzan.maxkb4j.tool.api.R;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author tarzan
 * @date 2024-12-25 12:42:39
 */
@RestController
@AllArgsConstructor
public class TeamMemberController{

    @Autowired
    private TeamMemberService teamMemberService;

    @GetMapping("api/team/member")
    public R<List<MemberVO>> teamMembers(){
        return R.success(teamMemberService.getByUserId(UUID.fromString(StpUtil.getLoginIdAsString())));
    }

    @GetMapping("api/team/member/root")
    public R<Map<String, List<TeamMemberPermissionVO>>> teamRootMember(){
        UUID userId = UUID.fromString(StpUtil.getLoginIdAsString());
        return R.success(teamMemberService.getMemberPermissions(userId,UUID.randomUUID()));
    }

    @GetMapping("api/team/member/{teamMemberId}")
    public R<Map<String, List<TeamMemberPermissionVO>>> getTeamMemberById(@PathVariable("teamMemberId") UUID teamMemberId){
        return R.success(teamMemberService.getPermissionByMemberId(teamMemberId));
    }

    @PutMapping("api/team/member/{teamMemberId}")
    public R<Map<String, List<TeamMemberPermissionVO>>> updateTeamMemberById(@PathVariable("teamMemberId") UUID teamMemberId,@RequestBody TeamMemberPermissionDTO dto){
        return R.success(teamMemberService.updateTeamMemberById(teamMemberId,dto));
    }

    @DeleteMapping("api/team/member/{teamMemberId}")
    public R<Boolean> deleteTeamMemberById(@PathVariable("teamMemberId") UUID teamMemberId){
        return R.success(teamMemberService.removeById(teamMemberId));
    }

    @PostMapping("api/team/member/_batch")
    public R<Boolean> addBatchTeamMember(@RequestBody List<UUID> userIds){
        if(teamMemberService.isExist(userIds)){
            return R.fail("团队中已存在当前成员,不要重复添加");
        }
        return R.success(teamMemberService.addBatchTeamMember(userIds,UUID.fromString(StpUtil.getLoginIdAsString())));
    }
}
