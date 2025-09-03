package com.tarzan.maxkb4j.module.system.team.controller;

import com.tarzan.maxkb4j.constant.AppConst;
import com.tarzan.maxkb4j.core.api.R;
import com.tarzan.maxkb4j.module.system.team.service.TeamMemberService;
import com.tarzan.maxkb4j.module.system.user.domain.entity.UserEntity;
import com.tarzan.maxkb4j.module.system.user.service.UserService;
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
    private final UserService userService;

 /*   @GetMapping("/user_member")
    public R<List<MemberVO>> teamMembers(){
        return R.success(teamMemberService.getByUserId(StpUtil.getLoginIdAsString()));
    }
*/
    @GetMapping("/user_member")
    public R<List<UserEntity>> teamMembers(){
        return R.success(userService.lambdaQuery().eq(UserEntity::getRole,"USER").list());
    }

}
