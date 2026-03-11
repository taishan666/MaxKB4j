package com.maxkb4j.system.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.application.entity.ApplicationEntity;
import com.maxkb4j.application.util.ResourceUtil;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.domain.api.R;
import com.maxkb4j.common.util.IoUtil;
import com.maxkb4j.common.util.JarUtil;
import com.maxkb4j.system.dto.AppTemplate;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author tarzan
 * @date 2024-12-31 17:33:32
 */
@RestController
@RequestMapping(AppConst.ADMIN_API)
public class StoreController {

    @GetMapping("/workspace/store/application_template")
    public R<JSONObject> applicationTemplate(String name) throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        InputStream inputStream = resolver.getResource("templates/maxkb.json").getInputStream();
        String text = IoUtil.readToString(inputStream);
        JSONObject result = JSON.parseObject(text);
        List<AppTemplate> appTemplates = new ArrayList<>();
        Resource[] resources = resolver.getResources("classpath:templates/app/*/*.mk");
        for (Resource resource : resources) {
            if (Objects.requireNonNull(resource.getFilename()).endsWith(".mk")) {
                String filename = resource.getFilename();
                AppTemplate appTemplate = new AppTemplate();
                ApplicationEntity app= ResourceUtil.parseApp(resource);
                if (app!=null){
                    String parentDirName = JarUtil.getParentDirName(resource);
                    String icon=StringUtils.isNotBlank(app.getIcon())?app.getIcon():"./app/"+parentDirName+"/logo.png";
                    appTemplate.setIcon(icon);
                    appTemplate.setName(app.getName());
                    appTemplate.setDescription(app.getDesc());
                    appTemplate.setType(app.getType());
                    Resource readmeResource= resolver.getResource("templates/app/"+parentDirName+"/readme.md");
                    if (readmeResource.exists()){
                        appTemplate.setReadMe(IoUtil.readToString(readmeResource.getInputStream()));
                    }
                    appTemplate.setDownloadUrl(parentDirName+"/"+filename);
                    appTemplate.setLabel("application_template");
                    appTemplates.add(appTemplate);
                }

            }
        }
        if(StringUtils.isNotBlank(name)) {
            appTemplates = appTemplates.stream().filter(app -> app.getName().contains(name)).collect(Collectors.toList());
        }
        result.put("apps", appTemplates);
        return R.data(result);
    }


}
