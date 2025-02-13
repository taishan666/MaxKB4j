package com.tarzan.maxkb4j.module.system.user.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tarzan.maxkb4j.module.system.user.dto.PasswordDTO;
import com.tarzan.maxkb4j.module.system.user.dto.UserDTO;
import com.tarzan.maxkb4j.module.system.user.dto.UserLoginDTO;
import com.tarzan.maxkb4j.module.system.user.entity.UserEntity;
import com.tarzan.maxkb4j.module.system.user.service.UserService;
import com.tarzan.maxkb4j.module.system.user.vo.UserVO;
import com.tarzan.maxkb4j.tool.api.R;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

/**
 * @author tarzan
 * @date 2024-12-25 11:17:00
 */
@RestController
public class UserController{

	@Autowired
	private UserService userService;

	@GetMapping("api/profile")
	public R<JSONObject> getProfile(){
		JSONObject json=new JSONObject();
		json.put("version",null);
		json.put("IS_XPACK",false);
		json.put("XPACK_LICENSE_IS_VALID",false);
        return R.success(json);

	}

	@SaCheckLogin
	@GetMapping("api/user")
	public R<UserVO> getUser(){
		return R.success(userService.getUserById(StpUtil.getLoginIdAsString()));
	}

	@PostMapping("api/user/login")
	public R<String> login(@RequestBody UserLoginDTO dto){
		return R.success(userService.login(dto));
	}

	@PostMapping("api/user/logout")
	public R<Boolean> logout(){
		if(StpUtil.isLogin()){
			StpUtil.logout();
		}
		return R.success(true);
	}


	@GetMapping("api/user/list")
	public R<List<UserEntity>> userList(String email_or_username){
		return R.success(userService.lambdaQuery().like(UserEntity::getUsername,email_or_username).or().like(UserEntity::getEmail,email_or_username).list());
	}

	@GetMapping("api/user/list/{type}")
	public R<List<UserDTO>> userDatasets(@PathVariable("type")String type){
		return R.success(userService.listByType(type));
	}


	@GetMapping("api/user_manage/{page}/{size}")
	public R<IPage<UserEntity>> userManage(@PathVariable("page")int page, @PathVariable("size")int size, String email_or_username){
		return R.success(userService.selectUserPage(page,size,email_or_username));
	}

	@PostMapping("api/user/language")
	public R<Boolean> language(@RequestBody UserEntity user){
		return R.success(userService.updateLanguage(user));
	}

	@PostMapping("api/user_manage")
	public R<Boolean> createUser(@RequestBody UserEntity user){
		return R.success(userService.createUser(user));
	}

	@GetMapping("api/valid/user/{id}")
	public R<Boolean> validUserById(@PathVariable("id")String id){
		//todo 验证用户是否存在
		return R.success(true);
	}
	@PutMapping("api/user_manage/{id}")
	public R<Boolean> updateUserById(@PathVariable("id")String id,@RequestBody UserEntity user){
		user.setId(id);
		return R.success(userService.updateById(user));
	}

	@DeleteMapping("api/user_manage/{id}")
	public R<Boolean> deleteUserById(@PathVariable("id")String id){
		return R.success(userService.deleteUserById(id));
	}

	@PutMapping("api/user_manage/{id}/re_password")
	public R<Boolean> updatePassword(@PathVariable("id")String id,@RequestBody PasswordDTO dto){
		if(!Objects.equals(dto.getPassword(), dto.getRePassword())){
			return R.fail("密码输入不一致");
		}
		return R.success(userService.updatePassword(id,dto));
	}

	@PostMapping("/api/user/current/send_email")
	public R<Boolean> sendEmail() throws MessagingException {
		return R.success(userService.sendEmailCode());
	}

	@PostMapping("/api/user/current/reset_password")
	public R<Boolean> resetPassword(@RequestBody PasswordDTO dto) {
		return R.success(userService.resetPassword(dto));
	}


}
