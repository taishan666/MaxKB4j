package com.maxkb4j.system.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.maxkb4j.common.api.R;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.constant.LoginType;
import com.maxkb4j.common.constant.RoleType;
import com.maxkb4j.common.props.SystemProperties;
import com.maxkb4j.common.util.I18nUtil;
import com.maxkb4j.system.entity.UserEntity;
import com.maxkb4j.system.service.impl.UserServiceImpl;
import com.maxkb4j.system.dto.PasswordDTO;
import com.maxkb4j.system.dto.UserDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @author tarzan
 * @date 2024-12-25 11:17:00
 */
@RestController
@RequestMapping(AppConst.ADMIN_API)
@RequiredArgsConstructor
public class UserManageController {

    private final UserServiceImpl userService;
	private final SystemProperties systemProperties;

    @SaCheckRole(type = LoginType.ADMIN, value = RoleType.ADMIN)
    @GetMapping("/user_manage/{page}/{size}")
    public R<IPage<UserEntity>> userManage(@PathVariable("page") int page, @PathVariable("size") int size, UserDTO dto) {
        return R.data(userService.selectUserPage(page, size, dto));
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
    @PostMapping("/user_manage/batch_delete")
    public R<Boolean> batchDelete(@Valid @RequestBody List<String> ids) {
        return R.status(userService.batchDelete(ids));
    }


    @SaCheckRole(type = LoginType.ADMIN, value = RoleType.ADMIN)
    @PutMapping("/user_manage/{id}/re_password")
    public R<Boolean> updatePassword(@PathVariable("id") String id, @Valid @RequestBody PasswordDTO dto) {
        if (!dto.getPassword().equals(dto.getRePassword())) {
            return R.fail(I18nUtil.get("user.password.not.match"));
        }
        return R.status(userService.updatePassword(id, dto));
    }

}
