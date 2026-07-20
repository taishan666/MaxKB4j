package com.maxkb4j.application.service;

import com.maxkb4j.application.dto.ApplicationSimple;
import com.maxkb4j.application.vo.ApplicationVO;

import java.util.List;
import java.util.Map;

public interface IApplicationService {

    ApplicationVO appProfile(String appId);

    ApplicationVO getAppDetail(String appId, boolean debug);
    ApplicationSimple getAppSimpleById(String appId);
    List<ApplicationSimple> listAppSimpleByIds(List<String> applicationIds);
    ApplicationVO getDtoById(String id);
    List<ApplicationVO> listDtoByIds(List<String> ids);
    List<Map<String, Object>> listMapsByIds(List<String> ids);
}
