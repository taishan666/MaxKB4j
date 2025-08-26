package com.tarzan.maxkb4j.module.application.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.constant.AppConst;
import com.tarzan.maxkb4j.core.api.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(AppConst.ADMIN_PATH +"/workspace")
public class ApplicationFolderController {


    @GetMapping("/{workspaceId}/{type}/folder")
    public R<List<JSONObject>> applicationFolder(@PathVariable String workspaceId, @PathVariable String type) {
        JSONObject result = new JSONObject();
        result.put("id", "default");
        result.put("name", "根目录");
        result.put("parentId", null);
        result.put("userId", StpUtil.getLoginId());
        result.put("workspaceId", "default");
        result.put("children", List.of());
        return R.data(List.of(result));
    }
}
