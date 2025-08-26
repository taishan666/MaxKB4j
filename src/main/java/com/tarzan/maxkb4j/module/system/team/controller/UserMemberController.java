package com.tarzan.maxkb4j.module.system.team.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.tarzan.maxkb4j.constant.AppConst;
import com.tarzan.maxkb4j.core.api.R;
import com.tarzan.maxkb4j.module.system.team.domain.vo.MemberVO;
import com.tarzan.maxkb4j.module.system.team.service.TeamMemberService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-25 12:42:39
 */
@RestController
@RequestMapping(AppConst.ADMIN_PATH+"/workspace/default")
@AllArgsConstructor
public class UserMemberController {

    private final TeamMemberService teamMemberService;

    @GetMapping("/user_member")
    public R<List<MemberVO>> teamMembers(){
        return R.success(teamMemberService.getByUserId(StpUtil.getLoginIdAsString()));
    }

}
