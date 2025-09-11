package com.tarzan.maxkb4j.module.folder.controler;

import cn.dev33.satoken.stp.StpUtil;
import com.tarzan.maxkb4j.constant.AppConst;
import com.tarzan.maxkb4j.core.api.R;
import com.tarzan.maxkb4j.module.folder.entity.FolderEntity;
import com.tarzan.maxkb4j.module.folder.service.FolderService;
import com.tarzan.maxkb4j.module.folder.vo.FolderVO;
import com.tarzan.maxkb4j.util.BeanUtil;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(AppConst.ADMIN_PATH + "/workspace/default")
@AllArgsConstructor
public class FolderController {

    private final FolderService folderService;


    @PostMapping("/{source}/folder")
    public R<Boolean> addFolder(@PathVariable String source, @RequestBody FolderVO folder) {
        FolderEntity folderEntity = BeanUtil.copy(folder, FolderEntity.class);
        folderEntity.setUserId(StpUtil.getLoginIdAsString());
        folderEntity.setSource(source);
        return R.data(folderService.save(folderEntity));
    }

    @PutMapping("/{source}/folder/{id}")
    public R<Boolean> updateFolder(@PathVariable String source, @PathVariable String id, @RequestBody FolderVO folder) {
        FolderEntity folderEntity = BeanUtil.copy(folder, FolderEntity.class);
        folderEntity.setId(id);
        folderEntity.setSource(source);
        return R.data(folderService.updateById(folderEntity));
    }

    @DeleteMapping("/{source}/folder/{id}")
    public R<Boolean> deleteFolder(@PathVariable String source, @PathVariable String id) {
        return R.data(folderService.deleteFolder(source, id));
    }

    @GetMapping("/{source}/folder")
    public R<List<FolderVO>> folderTree(@PathVariable String source) {
        return R.data(folderService.tree(source));
    }


}
