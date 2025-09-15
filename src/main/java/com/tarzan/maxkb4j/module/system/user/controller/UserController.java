package com.tarzan.maxkb4j.module.system.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tarzan.maxkb4j.constant.AppConst;
import com.tarzan.maxkb4j.core.api.R;
import com.tarzan.maxkb4j.module.system.user.domain.dto.PasswordDTO;
import com.tarzan.maxkb4j.module.system.user.domain.dto.UserDTO;
import com.tarzan.maxkb4j.module.system.user.domain.entity.UserEntity;
import com.tarzan.maxkb4j.module.system.user.service.UserService;
import com.tarzan.maxkb4j.util.StringUtil;
import jakarta.mail.MessagingException;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @author tarzan
 * @date 2024-12-25 11:17:00
 */
@RestController
@RequestMapping(AppConst.ADMIN_API)
@AllArgsConstructor
public class UserController{

	private final UserService userService;

	@GetMapping("/workspace/default/user_list")
	public R<List<UserEntity>> userList(){
		return R.data(userService.lambdaQuery().eq(UserEntity::getIsActive, true).list());
	}

	//@SaCheckPermission("USER:READ")
	@GetMapping("/user_manage/{page}/{size}")
	public R<IPage<UserEntity>> userManage(@PathVariable("page")int page, @PathVariable("size")int size, UserDTO dto){
		return R.data(userService.selectUserPage(page,size,dto));
	}
	//@SaCheckPermission("USER:EDIT")
	@PostMapping("/user/language")
	public R<Boolean> language(@RequestBody UserEntity user){
		return R.status(userService.updateLanguage(user));
	}

//	@SaCheckPermission("USER:CREATE")
	@PostMapping("/user_manage")
	public R<Boolean> createUser(@RequestBody UserEntity user){
		return R.status(userService.createUser(user));
	}

	@GetMapping("/user_manage/password")
	public R<Map<String, String>> password(){
		return R.data(Map.of("password","123456"));
	}

	//@SaCheckPermission("USER:EDIT")
	@PutMapping("/user_manage/{id}")
	public R<Boolean> updateUserById(@PathVariable("id")String id,@RequestBody UserEntity user){
		user.setId(id);
		return R.status(userService.updateById(user));
	}

//	@SaCheckPermission("USER:DELETE")
	@DeleteMapping("/user_manage/{id}")
	public R<Boolean> deleteUserById(@PathVariable("id")String id){
		return R.status(userService.deleteUserById(id));
	}

//	@SaCheckPermission("USER:EDIT")
	@PutMapping("/user_manage/{id}/re_password")
	public R<Boolean> updatePassword(@PathVariable("id")String id,@RequestBody PasswordDTO dto){
		if (StringUtil.isBlank(dto.getPassword())||StringUtil.isBlank(dto.getRePassword())){
			return R.fail("密码不能为空");
		}
		if(!dto.getPassword().equals(dto.getRePassword())){
			return R.fail("密码输入不一致");
		}
		return R.status(userService.updatePassword(id,dto));
	}

	@PostMapping("/user/current/send_email")
	public R<Boolean> sendEmail() throws MessagingException {
		return R.status(userService.sendEmailCode());
	}

	//@SaCheckPermission("USER:EDIT")
	@PostMapping("/user/current/reset_password")
	public R<Boolean> resetPassword(@RequestBody PasswordDTO dto) {
		return R.status(userService.resetPassword(dto));
	}


	@GetMapping("/workspace/default/user_member")
	public R<List<UserEntity>> userMembers(){
		return R.success(userService.lambdaQuery().eq(UserEntity::getRole,"USER").list());
	}


}
