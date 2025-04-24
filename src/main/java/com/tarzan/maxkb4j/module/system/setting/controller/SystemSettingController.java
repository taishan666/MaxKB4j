package com.tarzan.maxkb4j.module.system.setting.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.system.setting.entity.SystemSettingEntity;
import com.tarzan.maxkb4j.module.system.setting.enums.SettingType;
import com.tarzan.maxkb4j.module.system.setting.service.SystemSettingService;
import com.tarzan.maxkb4j.core.api.R;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
/**
 * @author tarzan
 * @date 2024-12-31 17:33:32
 */
@RestController
@AllArgsConstructor
public class SystemSettingController{

	private	final SystemSettingService systemSettingService;

	@SaCheckPermission("SETTING:CREATE")
	@GetMapping("api/email_setting")
	public R<JSONObject> getEmailSetting(){
		SystemSettingEntity systemSetting=systemSettingService.lambdaQuery().eq(SystemSettingEntity::getType,0).one();
		JSONObject json=systemSetting==null?new JSONObject():systemSetting.getMeta();
		return R.success(json);
	}

	@SaCheckPermission("SETTING:EDIT")
	@PostMapping("api/email_setting")
	public R<Boolean> testEmail(@RequestBody JSONObject meta){
		if(systemSettingService.testConnect(meta)){
			return R.success(true);
		}else {
			return R.fail("测试连接失败");
		}
	}

	@SaCheckPermission("SETTING:EDIT")
	@PutMapping("api/email_setting")
	public R<Boolean> saveEmailSetting(@RequestBody JSONObject meta){
		return R.status(systemSettingService.save(meta, SettingType.Email.getType()));
	}

	@GetMapping("api/valid/{type}/{count}")
	public R<Boolean> valid(@PathVariable("type")String type,@PathVariable("count")int count){
		//todo
		return R.status(count>0);
	}
}
