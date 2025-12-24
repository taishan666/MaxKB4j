package com.tarzan.maxkb4j.module.folder.controller;

import com.tarzan.maxkb4j.common.api.R;
import com.tarzan.maxkb4j.common.constant.AppConst;
import com.tarzan.maxkb4j.module.folder.service.FolderService;
import com.tarzan.maxkb4j.module.folder.vo.FolderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(AppConst.ADMIN_API + "/workspace/default")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;


/*    @PostMapping("/{source}/folder")
    public R<Boolean> addFolder(@PathVariable String source, @RequestBody FolderVO folder) {
        return R.data(folderService.addFolder(source,folder));
    }

    @PutMapping("/{source}/folder/{id}")
    public R<Boolean> updateFolder(@PathVariable String source, @PathVariable String id, @RequestBody FolderVO folder) {
        return R.data(folderService.updateFolder(source,id,folder));
    }

    @DeleteMapping("/{source}/folder/{id}")
    public R<Boolean> deleteFolder(@PathVariable String source, @PathVariable String id) {
        return R.data(folderService.deleteFolder(id));
    }*/

    @GetMapping("/{source}/folder")
    public R<List<FolderVO>> folderTree(@PathVariable String source) {
        return R.data(folderService.tree(source));
    }


}
