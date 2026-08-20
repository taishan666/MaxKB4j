package com.maxkb4j.system.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.maxkb4j.common.annotation.CurrentUserId;
import com.maxkb4j.common.api.R;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.constant.LoginType;
import com.maxkb4j.common.constant.RoleConst;
import com.maxkb4j.common.util.I18nUtil;
import com.maxkb4j.system.dto.UserLanguageDTO;
import com.maxkb4j.system.entity.UserEntity;
import com.maxkb4j.system.service.IUserInternalService;
import com.maxkb4j.system.vo.UserNameVO;
import com.maxkb4j.system.vo.UserProfileVO;
import com.maxkb4j.user.dto.PasswordDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-25 11:17:00
 */
@RestController
@RequestMapping(AppConst.ADMIN_API)
@RequiredArgsConstructor
public class UserController {

    private final IUserInternalService userService;

    @SaCheckRole(type = LoginType.ADMIN, value = {RoleConst.ADMIN, RoleConst.USER}, mode = SaMode.OR)
    @GetMapping("user/profile")
    public R<UserProfileVO> getUserProfile(@CurrentUserId String userId){
        return R.data(userService.getUserProfileById(userId));
    }

    @SaCheckRole(type= LoginType.ADMIN,value = {RoleConst.ADMIN, RoleConst.USER},mode = SaMode.OR)
    @GetMapping("/user/list")
    public R<List<UserNameVO>> userList(){
        return R.data(userService.listActiveUserNames());
    }

    @SaCheckRole(type = LoginType.ADMIN, value = RoleConst.ADMIN)
    @PostMapping("/user/language")
    public R<Boolean> language(@Valid @RequestBody UserLanguageDTO dto) {
        UserEntity user = new UserEntity();
        user.setLanguage(dto.getLanguage());
        return R.status(userService.updateLanguage(user));
    }

    @SaCheckRole(type = LoginType.ADMIN, value = RoleConst.ADMIN)
    @PostMapping("/user/current/send_email")
    public R<Boolean> sendEmail(@CurrentUserId String userId) {
        String email = userService.getEmail(userId);
        return R.status(userService.sendEmailCode(email, I18nUtil.get("email.subject.modify.password")));
    }

    @SaCheckRole(type = LoginType.ADMIN, value = RoleConst.ADMIN)
    @PostMapping("/user/current/reset_password")
    public R<Boolean> resetPassword(@Valid @RequestBody PasswordDTO dto) {
        return R.status(userService.resetPassword(dto));
    }

}
