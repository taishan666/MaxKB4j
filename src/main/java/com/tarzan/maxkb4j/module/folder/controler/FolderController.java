package com.tarzan.maxkb4j.module.folder.controler;

import cn.dev33.satoken.stp.StpUtil;
import com.tarzan.maxkb4j.constant.AppConst;
import com.tarzan.maxkb4j.core.api.R;
import com.tarzan.maxkb4j.module.folder.service.FolderService;
import com.tarzan.maxkb4j.module.folder.vo.FolderVO;
import com.tarzan.maxkb4j.module.system.user.domain.entity.UserEntity;
import com.tarzan.maxkb4j.module.system.user.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(AppConst.ADMIN_PATH + "/workspace/default")
@AllArgsConstructor
public class FolderController {

    private final FolderService folderService;
    private final UserService userService;


    @PostMapping("/{source}/folder")
    public R<Boolean> addFolder(@PathVariable String source, @RequestBody FolderVO folder) {
        UserEntity user = userService.getById(StpUtil.getLoginIdAsString());
        if (user != null) {
            if (user.getRole().contains("ADMIN")) {
                return R.data(folderService.addFolder(source,folder));
            }
        }
        return R.fail("无权限");
    }

    @PutMapping("/{source}/folder/{id}")
    public R<Boolean> updateFolder(@PathVariable String source, @PathVariable String id, @RequestBody FolderVO folder) {
        UserEntity user = userService.getById(StpUtil.getLoginIdAsString());
        if (user != null) {
            if (user.getRole().contains("ADMIN")) {
                return R.data(folderService.updateFolder(source,id,folder));
            }
        }
        return R.fail("无权限");
    }

    @DeleteMapping("/{source}/folder/{id}")
    public R<Boolean> deleteFolder(@PathVariable String source, @PathVariable String id) {
        UserEntity user = userService.getById(StpUtil.getLoginIdAsString());
        if (user != null) {
            if (user.getRole().contains("ADMIN")) {
                return R.data(folderService.deleteFolder(source, id));
            }
        }
        return R.fail("无权限");
    }

    @GetMapping("/{source}/folder")
    public R<List<FolderVO>> folderTree(@PathVariable String source) {
        return R.data(folderService.tree(source));
    }


}
