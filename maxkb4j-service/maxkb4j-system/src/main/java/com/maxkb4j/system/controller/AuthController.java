package com.maxkb4j.system.controller;

import cn.dev33.satoken.secure.SaSecureUtil;
import com.maxkb4j.common.api.R;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.util.I18nUtil;
import com.maxkb4j.common.util.StpKit;
import com.maxkb4j.user.dto.ResetPasswordDTO;
import com.maxkb4j.user.dto.UserLoginDTO;
import com.maxkb4j.user.entity.UserEntity;
import com.maxkb4j.user.service.IUserService;
import com.wf.captcha.SpecCaptcha;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;

/**
 * @author tarzan
 * @date 2024-12-25 11:17:00
 */
@RestController
@RequestMapping(AppConst.ADMIN_API)
@RequiredArgsConstructor
public class AuthController {

	private final IUserService userService;

	@PostMapping("/user/login")
	public R<String> login(@RequestBody UserLoginDTO dto, HttpServletRequest request){
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
		return R.status(userService.sendEmailCode(dto.getEmail(), I18nUtil.get("email.subject.forget.password")));
	}


	@PostMapping("/user/check_code")
	public R<Boolean> checkCode(@RequestBody ResetPasswordDTO dto){
		return R.status(userService.checkCode(dto.getEmail(),dto.getCode()));
	}

	@PostMapping("/user/rePassword")
	public R<Boolean> rePassword(@RequestBody ResetPasswordDTO dto){
		 String password=dto.getPassword();
		 String rePassword=dto.getRePassword();
		if (Objects.equals(password, rePassword)){
			UserEntity user=new UserEntity();
			user.setId(StpKit.ADMIN.getLoginIdAsString());
			user.setPassword(SaSecureUtil.md5(password));
			return R.status(userService.updateById(user));
		}
		return R.status(false);
	}

	@PostMapping("/user/logout")
	public R<Boolean> logout(){
		if(StpKit.ADMIN.isLogin()){
			StpKit.ADMIN.logout();
		}
		return R.status(true);
	}

}
