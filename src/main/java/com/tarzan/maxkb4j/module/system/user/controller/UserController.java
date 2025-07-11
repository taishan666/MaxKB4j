package com.tarzan.maxkb4j.module.system.user.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tarzan.maxkb4j.constant.AppConst;
import com.tarzan.maxkb4j.core.api.R;
import com.tarzan.maxkb4j.module.system.user.domain.dto.PasswordDTO;
import com.tarzan.maxkb4j.module.system.user.domain.dto.ResetPasswordDTO;
import com.tarzan.maxkb4j.module.system.user.domain.dto.UserDTO;
import com.tarzan.maxkb4j.module.system.user.domain.dto.UserLoginDTO;
import com.tarzan.maxkb4j.module.system.user.domain.entity.UserEntity;
import com.tarzan.maxkb4j.module.system.user.service.UserService;
import com.tarzan.maxkb4j.module.system.user.domain.vo.UserVO;
import com.tarzan.maxkb4j.util.StringUtil;
import com.wf.captcha.SpecCaptcha;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-25 11:17:00
 */
@RestController
@RequestMapping(AppConst.BASE_PATH)
@AllArgsConstructor
public class UserController{

	private final UserService userService;

	@GetMapping("/profile")
	public R<JSONObject> getProfile(){
		JSONObject json=new JSONObject();
		json.put("VERSION",null);
		json.put("IS_XPACK",false);
		json.put("XPACK_LICENSE_IS_VALID",false);
        return R.data(json);

	}

	@SaCheckLogin
	@GetMapping("/user")
	public R<UserVO> getUser(){
		return R.data(userService.getUserById(StpUtil.getLoginIdAsString()));
	}

	@PostMapping("/user/login")
	public R<String> login(@RequestBody UserLoginDTO dto,HttpServletRequest request){
		return R.data(userService.login(dto,request));
	}

	@GetMapping("/user/captcha")
	public R<String> captcha(HttpServletRequest request){
		SpecCaptcha specCaptcha = new SpecCaptcha(130, 48, 4);
		String verCode = specCaptcha.text().toLowerCase();
		//String key = UUID.randomUUID().toString();
		// 存入redis并设置过期时间为1分钟
		//bladeRedis.setEx(CacheNames.CAPTCHA_KEY + key, verCode, Duration.ofMinutes(3));
		// 5. 将验证码存入 session
		HttpSession session = request.getSession();
		session.setAttribute("captcha", verCode);
		// 将key和base64返回给前端
		return R.data(specCaptcha.toBase64());
	}

	@PostMapping("/user/send_email")
	public R<Boolean> sendEmail(@RequestBody ResetPasswordDTO dto){
		//todo
		System.out.println(dto);
		return R.status(true);
	}

	@PostMapping("/user/check_code")
	public R<Boolean> checkCode(@RequestBody ResetPasswordDTO dto){
		//todo
		System.out.println(dto);
		return R.status(true);
	}

	@PostMapping("/user/re_password")
	public R<Boolean> rePassword(@RequestBody ResetPasswordDTO dto){
		//todo
		System.out.println(dto);
		return R.status(true);
	}

	@SaCheckLogin
	@PostMapping("/user/logout")
	public R<Boolean> logout(){
		if(StpUtil.isLogin()){
			StpUtil.logout();
		}
		return R.status(true);
	}

	@SaCheckPermission("USER:READ")
	@GetMapping("/user/list")
	public R<List<UserEntity>> userList(String email_or_username){
		return R.data(userService.lambdaQuery().like(UserEntity::getUsername,email_or_username).or().like(UserEntity::getEmail,email_or_username).list());
	}

	@SaCheckPermission("USER:READ")
	@GetMapping("/user/list/{type}")
	public R<List<UserDTO>> userDatasets(@PathVariable("type")String type){
		return R.data(userService.listByType(type));
	}

	@SaCheckPermission("USER:READ")
	@GetMapping("/user_manage/{page}/{size}")
	public R<IPage<UserEntity>> userManage(@PathVariable("page")int page, @PathVariable("size")int size, String email_or_username){
		return R.data(userService.selectUserPage(page,size,email_or_username));
	}
	@SaCheckPermission("USER:EDIT")
	@PostMapping("/user/language")
	public R<Boolean> language(@RequestBody UserEntity user){
		return R.status(userService.updateLanguage(user));
	}

	@SaCheckPermission("USER:CREATE")
	@PostMapping("/user_manage")
	public R<Boolean> createUser(@RequestBody UserEntity user){
		return R.status(userService.createUser(user));
	}

	@SaCheckPermission("USER:EDIT")
	@PutMapping("/user_manage/{id}")
	public R<Boolean> updateUserById(@PathVariable("id")String id,@RequestBody UserEntity user){
		user.setId(id);
		return R.status(userService.updateById(user));
	}

	@SaCheckPermission("USER:DELETE")
	@DeleteMapping("/user_manage/{id}")
	public R<Boolean> deleteUserById(@PathVariable("id")String id){
		return R.status(userService.deleteUserById(id));
	}

	@SaCheckPermission("USER:EDIT")
	@PutMapping("/user_manage/{id}/re_password")
	public R<Boolean> updatePassword(@PathVariable("id")String id,@RequestBody PasswordDTO dto){
		if (StringUtil.isBlank(dto.getPassword())){
			return R.fail("密码不能为空");
		}
		if(!dto.getPassword().equals(dto.getRePassword())){
			return R.fail("密码输入不一致");
		}
		return R.status(userService.updatePassword(id,dto));
	}

	@PostMapping("/api/user/current/send_email")
	public R<Boolean> sendEmail() throws MessagingException {
		return R.status(userService.sendEmailCode());
	}

	@SaCheckPermission("USER:EDIT")
	@PostMapping("/api/user/current/reset_password")
	public R<Boolean> resetPassword(@RequestBody PasswordDTO dto) {
		return R.status(userService.resetPassword(dto));
	}


}
