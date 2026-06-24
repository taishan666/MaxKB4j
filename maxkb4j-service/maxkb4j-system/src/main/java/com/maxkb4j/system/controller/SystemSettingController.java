package com.maxkb4j.system.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaIgnore;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.api.R;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.constant.LoginType;
import com.maxkb4j.common.constant.RoleType;
import com.maxkb4j.common.util.I18nUtil;
import com.maxkb4j.system.dto.DisplayInfo;
import com.maxkb4j.system.entity.SystemSettingEntity;
import com.maxkb4j.system.enums.SettingType;
import com.maxkb4j.system.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;

/**
 * @author tarzan
 * @date 2024-12-31 17:33:32
 */
@RestController
@RequestMapping(AppConst.ADMIN_API)
@RequiredArgsConstructor
public class SystemSettingController{

	private	final SystemSettingService systemSettingService;

	@SaIgnore
	@GetMapping("/profile")
	public R<JSONObject> getProfile(){
		JSONObject json=new JSONObject();
		json.put("edition","CE");
		json.put("version","v2.10.0 (build at 2026-06-23T15:28)");
		json.put("license_is_valid",false);
		return R.data(json);
	}

	@SaCheckRole(type= LoginType.ADMIN,value = RoleType.ADMIN)
	@GetMapping("/email_setting")
	public R<JSONObject> getEmailSetting(){
		SystemSettingEntity systemSetting=systemSettingService.lambdaQuery().eq(SystemSettingEntity::getType, SettingType.Email.getType()).one();
		JSONObject json=systemSetting==null?new JSONObject():systemSetting.getMeta();
		return R.data(json);
	}

	@SaCheckRole(type=LoginType.ADMIN,value = RoleType.ADMIN)
	@PostMapping("/email_setting")
	public R<Boolean> testEmail(@RequestBody JSONObject meta){
		if(systemSettingService.testConnect(meta)){
			return R.status(true);
		}else {
			return R.fail(I18nUtil.get("email.test.connect.failed"));
		}
	}

	@SaCheckRole(type=LoginType.ADMIN,value = RoleType.ADMIN)
	@PutMapping("/email_setting")
	public R<Boolean> saveEmailSetting(@RequestBody JSONObject meta){
		return R.status(systemSettingService.saveOrUpdate(meta, SettingType.Email.getType()));
	}


	@SaCheckRole(type=LoginType.ADMIN,value = RoleType.ADMIN)
	@PutMapping(value = "/display/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public R<DisplayInfo> displayUpdate(DisplayInfo fromData) throws IOException {
		Object icon= fromData.getIcon();
		if (icon != null) {
			if (icon instanceof MultipartFile file){
				String base64 = Base64.getEncoder().encodeToString(file.getBytes());
				base64 = "data:" + file.getContentType() + ";base64," + base64;
				fromData.setIcon(base64);
			}
		}
		Object loginLogo= fromData.getLoginLogo();
		if (loginLogo != null) {
			if (loginLogo instanceof MultipartFile file){
				String base64 = Base64.getEncoder().encodeToString(file.getBytes());
				base64 = "data:" + file.getContentType() + ";base64," + base64;
				fromData.setLoginLogo(base64);
			}
		}
		Object loginImage= fromData.getLoginImage();
		if (loginImage != null) {
			if (loginImage instanceof MultipartFile file){
				String base64 = Base64.getEncoder().encodeToString(file.getBytes());
				base64 = "data:" + file.getContentType() + ";base64," + base64;
				fromData.setLoginImage(base64);
			}
		}
		JSONObject jsonObject = JSONObject.parseObject(JSONObject.toJSONString(fromData));
		systemSettingService.saveOrUpdate(jsonObject, SettingType.DISPLAY.getType());
		return R.data(fromData);
	}

	@SaIgnore
	@SaCheckRole(type=LoginType.ADMIN,value = RoleType.ADMIN)
	@GetMapping("/display/info")
	public R<DisplayInfo> display(){
		SystemSettingEntity systemSetting=systemSettingService.lambdaQuery().eq(SystemSettingEntity::getType, SettingType.DISPLAY.getType()).one();
		if (systemSetting==null){
			return R.data(null);
		}
		JSONObject meta=systemSetting.getMeta();
		return R.data(meta.toJavaObject(DisplayInfo.class));
	}
}
