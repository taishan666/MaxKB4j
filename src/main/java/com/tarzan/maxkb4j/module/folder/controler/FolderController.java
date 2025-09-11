package com.tarzan.maxkb4j.module.folder.controler;

import cn.dev33.satoken.stp.StpUtil;
import com.tarzan.maxkb4j.constant.AppConst;
import com.tarzan.maxkb4j.core.api.R;
import com.tarzan.maxkb4j.module.folder.entity.FolderEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(AppConst.ADMIN_PATH +"/workspace/default")
public class FolderController {

    @GetMapping("/{type}/folder")
    public R<List<FolderEntity>> folderTree(@PathVariable String type) {
        List<FolderEntity> result = new ArrayList<>();
        result.add(new FolderEntity("default", "根目录", null, StpUtil.getLoginIdAsString(), "default", new ArrayList<>()));
        return R.data(result);
    }
}
