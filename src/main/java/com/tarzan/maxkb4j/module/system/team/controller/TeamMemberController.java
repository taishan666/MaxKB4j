package com.tarzan.maxkb4j.module.system.team.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.tarzan.maxkb4j.module.system.team.dto.TeamMemberPermissionDTO;
import com.tarzan.maxkb4j.module.system.team.service.TeamMemberService;
import com.tarzan.maxkb4j.module.system.team.vo.MemberPermissionVO;
import com.tarzan.maxkb4j.module.system.team.vo.MemberVO;
import com.tarzan.maxkb4j.tool.api.R;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @author tarzan
 * @date 2024-12-25 12:42:39
 */
@RestController
@AllArgsConstructor
public class TeamMemberController{

    private final TeamMemberService teamMemberService;

    @GetMapping("api/team/member")
    public R<List<MemberVO>> teamMembers(){
        return R.success(teamMemberService.getByUserId(StpUtil.getLoginIdAsString()));
    }

    @GetMapping("api/team/member/{teamMemberId}")
    public R<Map<String, List<MemberPermissionVO>>> getTeamMemberById(@PathVariable("teamMemberId") String teamMemberId){
        return R.success(teamMemberService.getPermissionByMemberId(teamMemberId));
    }

    @PutMapping("api/team/member/{teamMemberId}")
    public R<Map<String, List<MemberPermissionVO>>> updateTeamMemberById(@PathVariable("teamMemberId") String teamMemberId, @RequestBody TeamMemberPermissionDTO dto){
        return R.success(teamMemberService.updateTeamMemberById(teamMemberId,dto));
    }

    @DeleteMapping("api/team/member/{teamMemberId}")
    public R<Boolean> deleteTeamMemberById(@PathVariable("teamMemberId") String teamMemberId){
        return R.success(teamMemberService.removeById(teamMemberId));
    }

    @PostMapping("api/team/member/_batch")
    public R<Boolean> addBatchTeamMember(@RequestBody List<String> userIds){
        if(teamMemberService.isExist(userIds)){
            return R.fail("团队中已存在当前成员,不要重复添加");
        }
        return R.success(teamMemberService.addBatchTeamMember(userIds, StpUtil.getLoginIdAsString()));
    }
}
