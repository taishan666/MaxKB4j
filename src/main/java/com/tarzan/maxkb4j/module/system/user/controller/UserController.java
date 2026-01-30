package com.tarzan.maxkb4j.module.system.user.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tarzan.maxkb4j.common.domain.api.R;
import com.tarzan.maxkb4j.common.constant.AppConst;
import com.tarzan.maxkb4j.common.props.SystemProperties;
import com.tarzan.maxkb4j.module.system.user.constants.LoginType;
import com.tarzan.maxkb4j.module.system.user.constants.RoleType;
import com.tarzan.maxkb4j.module.system.user.domain.dto.PasswordDTO;
import com.tarzan.maxkb4j.module.system.user.domain.dto.UserDTO;
import com.tarzan.maxkb4j.module.system.user.domain.entity.UserEntity;
import com.tarzan.maxkb4j.module.system.user.service.UserService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @author tarzan
 * @date 2024-12-25 11:17:00
 */
@RestController
@RequestMapping(AppConst.ADMIN_API)
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
	private final SystemProperties systemProperties;

    @SaCheckRole(type = LoginType.ADMIN, value = RoleType.ADMIN)
    @GetMapping("/user_manage/{page}/{size}")
    public R<IPage<UserEntity>> userManage(@PathVariable("page") int page, @PathVariable("size") int size, UserDTO dto) {
        return R.data(userService.selectUserPage(page, size, dto));
    }

    @SaCheckRole(type = LoginType.ADMIN, value = RoleType.ADMIN)
    @PostMapping("/user/language")
    public R<Boolean> language(@RequestBody UserEntity user) {
        return R.status(userService.updateLanguage(user));
    }

    @SaCheckRole(type = LoginType.ADMIN, value = RoleType.ADMIN)
    @PostMapping("/user_manage")
    public R<Boolean> createUser(@RequestBody UserEntity user) {
        return R.status(userService.createUser(user));
    }

    @SaCheckRole(type = LoginType.ADMIN, value = RoleType.ADMIN)
    @GetMapping("/user_manage/password")
    public R<Map<String, String>> password() {
        return R.data(Map.of("password", systemProperties.getDefaultPassword()));
    }

    @SaCheckRole(type = LoginType.ADMIN, value = RoleType.ADMIN)
    @PutMapping("/user_manage/{id}")
    public R<Boolean> updateUserById(@PathVariable("id") String id, @RequestBody UserEntity user) {
        user.setId(id);
        return R.status(userService.updateById(user));
    }

    @SaCheckRole(type = LoginType.ADMIN, value = RoleType.ADMIN)
    @DeleteMapping("/user_manage/{id}")
    public R<Boolean> deleteUserById(@PathVariable("id") String id) {
        return R.status(userService.deleteUserById(id));
    }

    @SaCheckRole(type = LoginType.ADMIN, value = RoleType.ADMIN)
    @PutMapping("/user_manage/{id}/re_password")
    public R<Boolean> updatePassword(@PathVariable("id") String id, @RequestBody PasswordDTO dto) {
        if (StringUtils.isBlank(dto.getPassword()) || StringUtils.isBlank(dto.getRePassword())) {
            return R.fail("密码不能为空");
        }
        if (!dto.getPassword().equals(dto.getRePassword())) {
            return R.fail("密码输入不一致");
        }
        return R.status(userService.updatePassword(id, dto));
    }

    @SaCheckRole(type = LoginType.ADMIN, value = {RoleType.ADMIN, RoleType.USER}, mode = SaMode.OR)
    @PostMapping("/user/current/send_email")
    public R<Boolean> sendEmail() throws MessagingException {
        return R.status(userService.sendEmailCode());
    }

    @SaCheckRole(type = LoginType.ADMIN, value = {RoleType.ADMIN, RoleType.USER}, mode = SaMode.OR)
    @PostMapping("/user/current/reset_password")
    public R<Boolean> resetPassword(@RequestBody PasswordDTO dto) {
        return R.status(userService.resetPassword(dto));
    }


}
