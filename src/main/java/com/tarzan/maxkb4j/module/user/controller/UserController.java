package com.tarzan.maxkb4j.module.user.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tarzan.maxkb4j.module.user.dto.PasswordDTO;
import com.tarzan.maxkb4j.module.user.dto.UserDTO;
import com.tarzan.maxkb4j.module.user.dto.UserLoginDTO;
import com.tarzan.maxkb4j.module.user.entity.UserEntity;
import com.tarzan.maxkb4j.module.user.service.UserService;
import com.tarzan.maxkb4j.tool.api.R;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

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

	@GetMapping("api/user")
	public R<JSONObject> getUser(HttpServletRequest request){
		JSONObject json=new JSONObject();
		json.put("id","f0dd8f71-e4ee-11ee-8c84-a8a1595801ab");
		List<String> permissions=new ArrayList<String>();
		permissions.add("APPLICATION:MANAGE:0e3eee95-c0cc-11ef-8f1c-bad5470d815f");
		permissions.add("APPLICATION:USE:0e3eee95-c0cc-11ef-8f1c-bad5470d815f");
		permissions.add("APPLICATION:DELETE:0e3eee95-c0cc-11ef-8f1c-bad5470d815f");
		permissions.add("APPLICATION:MANAGE:0e3eee95-c0cc-11ef-8f1c-bad5470d815f");
		permissions.add("DATASET:MANAGE:712fe46c-c00d-11ef-88ed-bad5470d815f");
		permissions.add("USER:READ");
		permissions.add("USER:EDIT");
		permissions.add("DATASET:CREATE");
		permissions.add("DATASET:USE");
		permissions.add("DATASET:READ");
		permissions.add("DATASET:EDIT");
		permissions.add("DATASET:DELETE");
		permissions.add("APPLICATION:READ");
		permissions.add("APPLICATION:CREATE");
		permissions.add("APPLICATION:DELETE");
		permissions.add("APPLICATION:EDIT");
		permissions.add("SETTING:READ");
		permissions.add("MODEL:READ");
		permissions.add("MODEL:CREATE");
		permissions.add("MODEL:DELETE");
		permissions.add("MODEL:EDIT");
		permissions.add("TEAM:READ");
		permissions.add("TEAM:CREATE");
		permissions.add("TEAM:DELETE");
		permissions.add("TEAM:EDIT");
		json.put("permissions",permissions);
		json.put("role","ADMIN");
		json.put("username","admin");
		json.put("email","");
		json.put("is_edit_password",false);
		return R.success(json);
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

	@PostMapping("api/user_manage")
	public R<Boolean> createUser(@RequestBody UserEntity user){
		return R.success(userService.createUser(user));
	}

	@PutMapping("api/user_manage/{id}")
	public R<Boolean> updateUserById(@PathVariable("id")String id,@RequestBody UserEntity user){
		user.setId(UUID.fromString(id));
		return R.success(userService.updateById(user));
	}

	@DeleteMapping("api/user_manage/{id}")
	public R<Boolean> deleteUserById(@PathVariable("id")UUID id){
		return R.success(userService.deleteUserById(id));
	}

	@PutMapping("api/user_manage/{id}/re_password")
	public R<Boolean> updatePassword(@PathVariable("id")String id,@RequestBody PasswordDTO dto){
		if(!Objects.equals(dto.getPassword(), dto.getRePassword())){
			return R.fail("密码输入不一致");
		}
		UserEntity user = new UserEntity();
		user.setId(UUID.fromString(id));
		user.setPassword(dto.getPassword());
		return R.success(userService.updateById(user));
	}
}
