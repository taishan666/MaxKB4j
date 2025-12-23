package com.tarzan.maxkb4j.module.system.setting.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.common.api.R;
import com.tarzan.maxkb4j.common.constant.AppConst;
import com.tarzan.maxkb4j.module.system.setting.domain.dto.DisplayInfo;
import com.tarzan.maxkb4j.module.system.setting.domain.entity.SystemSettingEntity;
import com.tarzan.maxkb4j.module.system.setting.enums.SettingType;
import com.tarzan.maxkb4j.module.system.setting.service.SystemSettingService;
import com.tarzan.maxkb4j.module.system.user.constants.RoleType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * @author tarzan
 * @date 2024-12-31 17:33:32
 */
@RestController
@RequestMapping(AppConst.ADMIN_API)
@RequiredArgsConstructor
public class SystemSettingController{

	private	final SystemSettingService systemSettingService;

	@SaCheckRole(RoleType.ADMIN)
	@GetMapping("/email_setting")
	public R<JSONObject> getEmailSetting(){
		SystemSettingEntity systemSetting=systemSettingService.lambdaQuery().eq(SystemSettingEntity::getType, SettingType.Email.getType()).one();
		JSONObject json=systemSetting==null?new JSONObject():systemSetting.getMeta();
		return R.success(json);
	}

	@SaCheckRole(RoleType.ADMIN)
	@PostMapping("/email_setting")
	public R<Boolean> testEmail(@RequestBody JSONObject meta){
		if(systemSettingService.testConnect(meta)){
			return R.success(true);
		}else {
			return R.fail("测试连接失败");
		}
	}

	@SaCheckRole(RoleType.ADMIN)
	@PutMapping("/email_setting")
	public R<Boolean> saveEmailSetting(@RequestBody JSONObject meta){
		return R.status(systemSettingService.saveOrUpdate(meta, SettingType.Email.getType()));
	}

	@SaCheckRole(RoleType.ADMIN)
	@PostMapping(value = "/display/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public R<DisplayInfo> display(DisplayInfo formData){
		JSONObject jsonObject = JSONObject.parseObject(JSONObject.toJSONString(formData));
		systemSettingService.saveOrUpdate(jsonObject, SettingType.DISPLAY.getType());
		return R.data(formData);
	}

	@SaCheckRole(RoleType.ADMIN)
	@GetMapping("/display/info")
	public R<DisplayInfo> display(){
		SystemSettingEntity systemSetting=systemSettingService.lambdaQuery().eq(SystemSettingEntity::getType, SettingType.DISPLAY.getType()).one();
		JSONObject json=systemSetting==null?new JSONObject():systemSetting.getMeta();
		return R.success(json.toJavaObject(DisplayInfo.class));
	}
}
