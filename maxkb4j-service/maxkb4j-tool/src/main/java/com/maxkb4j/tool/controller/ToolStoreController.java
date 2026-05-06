package com.maxkb4j.tool.controller;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.api.R;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.util.IoUtil;
import com.maxkb4j.common.util.JarUtil;
import com.maxkb4j.tool.consts.ToolConstants;
import com.maxkb4j.tool.entity.ToolEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
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
public class ToolStoreController {

    @GetMapping("/workspace/internal/tool")
    public R<List<ToolEntity>>  store(String name) throws IOException {
        List<ToolEntity> list = new ArrayList<>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:templates/tool/*/*" + ToolConstants.FileType.TOOL_EXTENSION);
        for (Resource resource : resources) {
            String filename = resource.getFilename();
            if (Objects.requireNonNull(filename).endsWith(ToolConstants.FileType.TOOL_EXTENSION)) {
                // ✅ 安全获取父目录名：从 resource 的 URL 路径中解析
                String parentDirName = JarUtil.getParentDirName(resource);
                String text = IoUtil.readToString(resource.getInputStream());
                ToolEntity tool = JSONObject.parseObject(text, ToolEntity.class);
                if (tool != null) {
                    tool.setLabel(parentDirName);
                    if (StringUtils.isBlank(tool.getVersion())) {
                        tool.setVersion(ToolConstants.Defaults.DEFAULT_VERSION);
                    }
                    list.add(tool);
                }
            }
        }
        if (StringUtils.isNotBlank(name)) {
            list = list.stream().filter(tool -> tool.getName().contains(name)).collect(Collectors.toList());
        }
        return R.data(list);
    }


}
