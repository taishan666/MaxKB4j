package com.tarzan.maxkb4j.module.system.user.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.secure.SaSecureUtil;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.common.api.R;
import com.tarzan.maxkb4j.common.constant.AppConst;
import com.tarzan.maxkb4j.common.util.StpKit;
import com.tarzan.maxkb4j.module.system.user.domain.dto.ResetPasswordDTO;
import com.tarzan.maxkb4j.module.system.user.domain.dto.UserLoginDTO;
import com.tarzan.maxkb4j.module.system.user.domain.entity.UserEntity;
import com.tarzan.maxkb4j.module.system.user.domain.vo.UserVO;
import com.tarzan.maxkb4j.module.system.user.service.UserService;
import com.wf.captcha.SpecCaptcha;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @author tarzan
 * @date 2024-12-25 11:17:00
 */
@RestController
@RequestMapping(AppConst.ADMIN_API)
@AllArgsConstructor
public class AuthController {

	private final UserService userService;

	@GetMapping("/profile")
	public R<JSONObject> getProfile(){
		JSONObject json=new JSONObject();
		json.put("edition","CE");
		json.put("version","v2.0.1 (build at 2025-07-18T15:28)");
	    json.put("license_is_valid",false);
		return R.data(json);
	}

	@GetMapping("user/profile")
	public R<UserVO> getUserProfile(){
		String userId = StpKit.ADMIN.getLoginIdAsString();
		return R.data(userService.getUserById(userId));
	}

	@SaCheckLogin
	@GetMapping("/user")
	public R<UserVO> getUser(){
		return R.data(userService.getUserById(StpKit.ADMIN.getLoginIdAsString()));
	}

	@PostMapping("/user/login")
	public R<String> login(@RequestBody UserLoginDTO dto,HttpServletRequest request){
		return R.data(userService.login(dto,request));
	}

	@GetMapping("/user/captcha")
	public R<Map<String, String>> captcha(HttpServletRequest request){
		SpecCaptcha specCaptcha = new SpecCaptcha(130, 48, 4);
		String verCode = specCaptcha.text().toLowerCase();
		//  将验证码存入 session
		HttpSession session = request.getSession();
		session.setAttribute("captcha", verCode);
		// 将key和base64返回给前端
		return R.data(Map.of("captcha",specCaptcha.toBase64()));
	}

	@PostMapping("/user/send_email")
	public R<Boolean> sendEmail(@RequestBody ResetPasswordDTO dto) throws MessagingException {
		return R.status(userService.sendEmailCode(dto.getEmail(),"【智能知识库问答系统-忘记密码】"));
	}


	@PostMapping("/user/check_code")
	public R<Boolean> checkCode(@RequestBody ResetPasswordDTO dto){
		return R.status(userService.checkCode(dto.getEmail(),dto.getCode()));
	}

	@PostMapping("/user/rePassword")
	public R<Boolean> rePassword(@RequestBody ResetPasswordDTO dto){
		 String password=dto.getPassword();
		 String rePassword=dto.getRePassword();
		if (password.equals(rePassword)){
			UserEntity user=new UserEntity();
			user.setId(StpKit.ADMIN.getLoginIdAsString());
			user.setPassword(SaSecureUtil.md5(password));
			return R.status(userService.updateById(user));
		}
		return R.status(false);
	}

	@SaCheckLogin
	@PostMapping("/user/logout")
	public R<Boolean> logout(){
		if(StpKit.ADMIN.isLogin()){
			StpKit.ADMIN.logout();
		}
		return R.status(true);
	}

}
