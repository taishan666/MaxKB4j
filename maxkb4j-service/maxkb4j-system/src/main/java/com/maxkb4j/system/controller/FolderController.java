package com.maxkb4j.system.controller;

import com.maxkb4j.common.api.R;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.system.service.FolderService;
import com.maxkb4j.system.vo.FolderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(AppConst.ADMIN_WORKSPACE_API)
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;

    @GetMapping("/{source}/folder")
    public R<List<FolderVO>> folderTree(@PathVariable String source) {
        return R.data(folderService.tree(source));
    }


}
