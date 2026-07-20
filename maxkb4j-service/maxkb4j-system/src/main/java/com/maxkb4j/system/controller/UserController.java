package com.maxkb4j.system.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.maxkb4j.common.annotation.CurrentUserId;
import com.maxkb4j.common.api.R;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.constant.LoginType;
import com.maxkb4j.common.constant.RoleType;
import com.maxkb4j.common.util.I18nUtil;
import com.maxkb4j.system.entity.UserEntity;
import com.maxkb4j.system.service.impl.UserServiceImpl;
import com.maxkb4j.system.dto.PasswordDTO;
import com.maxkb4j.system.vo.UserVO;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @author tarzan
 * @date 2024-12-25 11:17:00
 */
@RestController
@RequestMapping(AppConst.ADMIN_API)
@RequiredArgsConstructor
public class UserController {

    private final UserServiceImpl userService;

    @GetMapping("user/profile")
    public R<UserVO> getUserProfile(@CurrentUserId String userId){
        return R.data(userService.getUserById(userId));
    }

    @SaCheckRole(type = LoginType.ADMIN, value = RoleType.ADMIN)
    @PostMapping("/user/language")
    public R<Boolean> language(@RequestBody UserEntity user) {
        return R.status(userService.updateLanguage(user));
    }

    @SaCheckRole(type = LoginType.ADMIN, value = RoleType.ADMIN)
    @PostMapping("/user/current/send_email")
    public R<Boolean> sendEmail(@CurrentUserId String userId) throws MessagingException {
        String email = userService.getEmail(userId);
        return R.status(userService.sendEmailCode(email, I18nUtil.get("email.subject.modify.password")));
    }

    @SaCheckRole(type = LoginType.ADMIN, value = RoleType.ADMIN)
    @PostMapping("/user/current/reset_password")
    public R<Boolean> resetPassword(@Valid @RequestBody PasswordDTO dto) {
        return R.status(userService.resetPassword(dto));
    }



}
