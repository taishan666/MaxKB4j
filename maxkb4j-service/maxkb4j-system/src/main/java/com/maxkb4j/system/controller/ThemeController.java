package com.maxkb4j.system.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaIgnore;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.api.R;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.constant.LoginType;
import com.maxkb4j.common.constant.RoleType;
import com.maxkb4j.common.domain.dto.OssFile;
import com.maxkb4j.oss.service.IOssService;
import com.maxkb4j.system.dto.DisplayInfo;
import com.maxkb4j.system.entity.SystemSettingEntity;
import com.maxkb4j.system.enums.SettingType;
import com.maxkb4j.system.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * @author tarzan
 * @date 2024-12-31 17:33:32
 */
@RestController
@RequestMapping(AppConst.ADMIN_API)
@RequiredArgsConstructor
public class ThemeController {

	private	final SystemSettingService systemSettingService;
	private	final IOssService ossService;

	@SaCheckRole(type=LoginType.ADMIN,value = RoleType.ADMIN)
	@PutMapping(value = "/display/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public R<DisplayInfo> displayUpdate(DisplayInfo fromData) throws IOException {
		Object icon= fromData.getIcon();
		if (icon != null) {
			if (icon instanceof MultipartFile file){
				OssFile ossFile = ossService.uploadFile(file);
				fromData.setIcon(ossFile.getUrl());
			}
		}
		Object loginLogo= fromData.getLoginLogo();
		if (loginLogo != null) {
			if (loginLogo instanceof MultipartFile file){
				OssFile ossFile = ossService.uploadFile(file);
				fromData.setLoginLogo(ossFile.getUrl());
			}
		}
		Object loginImage= fromData.getLoginImage();
		if (loginImage != null) {
			if (loginImage instanceof MultipartFile file){
				OssFile ossFile = ossService.uploadFile(file);
				fromData.setLoginImage(ossFile.getUrl());
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
