package com.maxkb4j.application.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.application.dto.ApplicationDTO;
import com.maxkb4j.application.dto.ApplicationQuery;
import com.maxkb4j.application.entity.ApplicationEntity;
import com.maxkb4j.application.vo.ApplicationListVO;
import com.maxkb4j.application.vo.ApplicationVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * 应用服务「对内」接口：供 Controller 使用的完整服务契约。
 * 与 {@link IApplicationService}（对外跨模块契约，位于 maxkb4j-application-api）区分。
 */
public interface IApplicationInternalService extends IApplicationService, IService<ApplicationEntity> {

    IPage<ApplicationVO> selectAppPage(int page, int size, ApplicationQuery query);

    boolean deleteByAppId(String appId);

    ApplicationEntity createApp(ApplicationDTO application);

    boolean appImport(InputStream inputStream);

    boolean appImport(MultipartFile file);

    ApplicationVO getDetail(String id);

    Boolean updateAppById(String appId, ApplicationVO appVO);

    ApplicationEntity publish(String id, JSONObject params);

    List<ApplicationListVO> listApps(String folderId);

    boolean deleteBatch(List<String> idList);
}