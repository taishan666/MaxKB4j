package com.maxkb4j.application.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.application.entity.ApplicationVersionEntity;
import com.maxkb4j.application.vo.ApplicationVO;

import java.util.List;

/**
 * 应用版本服务「对内」接口：供 Controller 使用的完整服务契约。
 */
public interface IApplicationVersionService extends IService<ApplicationVersionEntity> {

    ApplicationVO getAppLatestOne(String appId);

    List<ApplicationVersionEntity> listByApplicationId(String appId);
}