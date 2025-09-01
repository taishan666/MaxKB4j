package com.tarzan.maxkb4j.module.system.user.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tarzan.maxkb4j.constant.AppConst;
import com.tarzan.maxkb4j.core.api.R;
import com.tarzan.maxkb4j.module.system.user.domain.dto.PasswordDTO;
import com.tarzan.maxkb4j.module.system.user.domain.dto.ResetPasswordDTO;
import com.tarzan.maxkb4j.module.system.user.domain.dto.UserDTO;
import com.tarzan.maxkb4j.module.system.user.domain.dto.UserLoginDTO;
import com.tarzan.maxkb4j.module.system.user.domain.entity.UserEntity;
import com.tarzan.maxkb4j.module.system.user.domain.vo.UserVO;
import com.tarzan.maxkb4j.module.system.user.service.UserService;
import com.tarzan.maxkb4j.util.StringUtil;
import com.wf.captcha.SpecCaptcha;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author tarzan
 * @date 2024-12-25 11:17:00
 */
@RestController
@RequestMapping(AppConst.ADMIN_PATH)
@AllArgsConstructor
public class UserController{

	private final UserService userService;

	@GetMapping("/profile")
	public R<JSONObject> getProfile(){
		JSONObject json=new JSONObject();
		json.put("edition","CE");
		json.put("VERSION","v2.0.1 (build at 2025-07-18T15:28, commit: 6e16c74)");
	    json.put("license_is_valid",false);
		return R.data(json);
	}

	@GetMapping("user/profile")
	public R<UserVO> getUserProfile(){
		return R.data(userService.getUserById(StpUtil.getLoginIdAsString()));
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
	public R<Map<String, String>> captcha(HttpServletRequest request){
		SpecCaptcha specCaptcha = new SpecCaptcha(130, 48, 4);
		String verCode = specCaptcha.text().toLowerCase();
		//String key = UUID.randomUUID().toString();
		// 存入redis并设置过期时间为1分钟
		//bladeRedis.setEx(CacheNames.CAPTCHA_KEY + key, verCode, Duration.ofMinutes(3));
		// 5. 将验证码存入 session
		HttpSession session = request.getSession();
		session.setAttribute("captcha", verCode);
		// 将key和base64返回给前端
		return R.data(Map.of("captcha",specCaptcha.toBase64()));
	}

	@PostMapping("/user/send_email")
	public R<Boolean> sendEmail(@RequestBody ResetPasswordDTO dto) throws MessagingException {
		return R.status(userService.sendEmailCode(dto.getEmail(),"【智能知识库问答系统-忘记密码】"));
	}

	// 创建缓存并配置
	private static final Cache<String, String> AUTH_CODE_CACHE = Caffeine.newBuilder()
			.initialCapacity(5)
			// 超出最大容量时淘汰
			.maximumSize(100000)
			//设置写缓存后n秒钟过期
			.expireAfterWrite(10, TimeUnit.MINUTES)
			.expireAfterAccess(3, TimeUnit.MINUTES) // 最近访问后5分钟过期
			.build();

	@PostMapping("/user/check_code")
	public R<Boolean> checkCode(@RequestBody ResetPasswordDTO dto){
		String code=AUTH_CODE_CACHE.getIfPresent(dto.getEmail());
		return R.status(dto.getCode().equals(code));
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
	public R<List<UserEntity>> userList1(String email_or_username){
		return R.data(userService.lambdaQuery().like(UserEntity::getUsername,email_or_username).or().like(UserEntity::getEmail,email_or_username).list());
	}

	@GetMapping("/workspace/default/user_list")
	public R<List<UserEntity>> userList(String email_or_username){
		return R.data(userService.lambdaQuery().like(UserEntity::getUsername,email_or_username).or().like(UserEntity::getEmail,email_or_username).list());
	}

	@SaCheckPermission("USER:READ")
	@GetMapping("/user/list/{type}")
	public R<List<UserDTO>> userDatasets(@PathVariable("type")String type){
		return R.data(userService.listByType(type));
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
		if (StringUtil.isBlank(dto.getPassword())){
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


}
