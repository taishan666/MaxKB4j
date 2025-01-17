package com.tarzan.maxkb4j.module.system.setting.controller;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.system.setting.entity.SystemSettingEntity;
import com.tarzan.maxkb4j.tool.api.R;
import org.springframework.web.bind.annotation.*;
import lombok.AllArgsConstructor;
import com.tarzan.maxkb4j.module.system.setting.service.SystemSettingService;
/**
 * @author tarzan
 * @date 2024-12-31 17:33:32
 */
@RestController
@AllArgsConstructor
public class SystemSettingController{

	private	final SystemSettingService systemSettingService;

	@GetMapping("api/email_setting")
	public R<JSONObject> getEmailSetting(){
		SystemSettingEntity systemSetting=systemSettingService.lambdaQuery().eq(SystemSettingEntity::getType,0).one();
		JSONObject json=systemSetting==null?new JSONObject():systemSetting.getMeta();
		return R.success(json);
	}

	@PostMapping("api/email_setting")
	public R<Boolean> testEmail(@RequestBody JSONObject meta){
		if(systemSettingService.testConnect(meta)){
			return R.success(true);
		}else {
			return R.fail("测试连接失败");
		}
	}

	@PutMapping("api/email_setting")
	public R<Boolean> saveEmailSetting(@RequestBody JSONObject meta){
		return R.status(systemSettingService.saveEmailSetting(meta));
	}
}
