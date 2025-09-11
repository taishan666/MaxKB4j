package com.tarzan.maxkb4j.module.folder.controler;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.tarzan.maxkb4j.constant.AppConst;
import com.tarzan.maxkb4j.core.api.R;
import com.tarzan.maxkb4j.module.folder.entity.FolderEntity;
import com.tarzan.maxkb4j.module.folder.service.FolderService;
import com.tarzan.maxkb4j.module.folder.vo.FolderVO;
import com.tarzan.maxkb4j.util.BeanUtil;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(AppConst.ADMIN_PATH + "/workspace/default")
@AllArgsConstructor
public class FolderController {

    private final FolderService folderService;

    @PostMapping("/{type}/folder")
    public R<Boolean> addFolder(@PathVariable String type, @RequestBody FolderVO folder) {
        FolderEntity folderEntity = BeanUtil.copy(folder, FolderEntity.class);
        folderEntity.setUserId(StpUtil.getLoginIdAsString());
        folderEntity.setSource(type);
        return R.data(folderService.save(folderEntity));
    }

    @PutMapping("/{type}/folder/{id}")
    public R<Boolean> addFolder(@PathVariable String type, @PathVariable String id, @RequestBody FolderVO folder) {
        FolderEntity folderEntity = BeanUtil.copy(folder, FolderEntity.class);
        folderEntity.setId(id);
        folderEntity.setSource(type);
        return R.data(folderService.updateById(folderEntity));
    }

    @DeleteMapping("/{type}/folder/{id}")
    public R<Boolean> deleteFolder(@PathVariable String type, @PathVariable String id) {
        if ("default".equals(id)){
            return R.fail("默认文件夹不能删除");
        }
        return R.data(folderService.removeById(id));
    }

    @GetMapping("/{type}/folder")
    public R<List<FolderVO>> folderTree(@PathVariable String type) {
        List<FolderVO> result = new ArrayList<>();
        result.add(new FolderVO("default", "根目录", null, "1233456", List.of()));
        return R.data(result);
    }

    /**
     * 图层目录-组装树
     *
     * @param sourceList&parent
     * @Author: tarzan Liu
     * @Date: 2019/12/6 11:14
     */
    public void buildTree(List<FolderVO> sourceList, FolderVO parent) {
        if (CollectionUtils.isNotEmpty(sourceList)) {
            List<FolderVO> resultList = sourceList.stream().filter(e -> e.getParentId().equals(parent.getId())).collect(Collectors.toList());
          //  resultList.sort(Comparator.comparing(FolderVO::getSort));
            parent.setChildren(resultList);
            resultList.forEach(e -> {
                buildTree(sourceList, e);
            });
        }
    }

}
