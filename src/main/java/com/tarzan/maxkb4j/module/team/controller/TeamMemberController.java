package com.tarzan.maxkb4j.module.team.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.team.dto.TeamMemberPermissionDTO;
import com.tarzan.maxkb4j.module.team.service.TeamMemberService;
import com.tarzan.maxkb4j.module.team.vo.MemberVO;
import com.tarzan.maxkb4j.module.team.vo.TeamMemberPermissionVO;
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
    public R<JSONObject> teamRootMember(){
        String res="{\n" +
                "        \"APPLICATION\": [\n" +
                "            {\n" +
                "                \"id\": \"0e3eee95-c0cc-11ef-8f1c-bad5470d815f\",\n" +
                "                \"name\": \"\\u5de5\\u4f5c\\u6d4166\",\n" +
                "                \"type\": \"APPLICATION\",\n" +
                "                \"user_id\": \"f0dd8f71-e4ee-11ee-8c84-a8a1595801ab\",\n" +
                "                \"member_id\": \"root\",\n" +
                "                \"operate\": {\n" +
                "                    \"USE\": true,\n" +
                "                    \"MANAGE\": true\n" +
                "                }\n" +
                "            },\n" +
                "            {\n" +
                "                \"id\": \"4368e2cb-c4e1-11ef-912f-d8808373b6e1\",\n" +
                "                \"name\": \"DSADSA\",\n" +
                "                \"type\": \"APPLICATION\",\n" +
                "                \"user_id\": \"f0dd8f71-e4ee-11ee-8c84-a8a1595801ab\",\n" +
                "                \"member_id\": \"root\",\n" +
                "                \"operate\": {\n" +
                "                    \"USE\": true,\n" +
                "                    \"MANAGE\": true\n" +
                "                }\n" +
                "            },\n" +
                "            {\n" +
                "                \"id\": \"5959452f-bf8d-11ef-ac9b-bad5470d815f\",\n" +
                "                \"name\": \"\\u5b66\\u6821\\u5ba2\\u670d\",\n" +
                "                \"type\": \"APPLICATION\",\n" +
                "                \"user_id\": \"f0dd8f71-e4ee-11ee-8c84-a8a1595801ab\",\n" +
                "                \"member_id\": \"root\",\n" +
                "                \"operate\": {\n" +
                "                    \"USE\": true,\n" +
                "                    \"MANAGE\": true\n" +
                "                }\n" +
                "            }\n" +
                "        ],\n" +
                "        \"DATASET\": [\n" +
                "            {\n" +
                "                \"id\": \"712fe46c-c00d-11ef-88ed-bad5470d815f\",\n" +
                "                \"name\": \"\\u77f3\\u5bb6\\u5e84\\u533b\\u4e13\\u5ba2\\u670d\\u77e5\\u8bc6\\u5e93\",\n" +
                "                \"type\": \"DATASET\",\n" +
                "                \"user_id\": \"f0dd8f71-e4ee-11ee-8c84-a8a1595801ab\",\n" +
                "                \"member_id\": \"root\",\n" +
                "                \"operate\": {\n" +
                "                    \"USE\": true,\n" +
                "                    \"MANAGE\": true\n" +
                "                }\n" +
                "            }\n" +
                "        ]\n" +
                "    }";
        return R.success(JSONObject.parseObject(res));
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
