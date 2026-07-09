package com.maxkb4j.application.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.application.entity.ApplicationEntity;
import com.maxkb4j.application.vo.ApplicationVO;

public interface IApplicationService extends IService<ApplicationEntity> {

    ApplicationVO appProfile(String appId);

    ApplicationVO getAppDetail(String appId, boolean debug);
}
