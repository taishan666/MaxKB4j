package com.tarzan.maxkb4j.module.folder.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.folder.entity.FolderEntity;
import com.tarzan.maxkb4j.module.folder.mapper.FolderMapper;
import com.tarzan.maxkb4j.module.folder.vo.FolderVO;
import com.tarzan.maxkb4j.module.system.user.domain.entity.UserEntity;
import com.tarzan.maxkb4j.module.system.user.service.UserService;
import com.tarzan.maxkb4j.util.BeanUtil;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class FolderService extends ServiceImpl<FolderMapper, FolderEntity> {

    private final UserService userService;

    @Transactional
    public List<FolderVO> tree(String source) {
        UserEntity user = userService.getById(StpUtil.getLoginIdAsString());
        LambdaQueryWrapper<FolderEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(FolderEntity::getSource, source);
        if (user != null) {
            if (user.getRole().contains("USER")) {
                wrapper.eq(FolderEntity::getUserId, StpUtil.getLoginIdAsString());
            }
        } else {
            wrapper.last(" limit 0");
        }
        List<FolderEntity> list = this.list(wrapper);
        List<FolderVO> result = new ArrayList<>();
        if (CollectionUtils.isEmpty(list)) {
            FolderEntity parent = new FolderEntity();
            parent.setName("根目录");
            parent.setSource(source);
            parent.setUserId(StpUtil.getLoginIdAsString());
            this.save(parent);
            result.add(BeanUtil.copy(parent, FolderVO.class));
        } else {
            List<FolderVO> sourceList = BeanUtil.copyList(list, FolderVO.class);
            Optional<FolderVO> optional = sourceList.stream().filter(e -> Objects.isNull(e.getParentId())).findAny();
            if (optional.isPresent()) {
                FolderVO parent = optional.get();
                result.add(parent);
                buildTree(sourceList, parent);
            }
        }
        return result;
    }

    public void buildTree(List<FolderVO> sourceList, FolderVO parent) {
        if (CollectionUtils.isNotEmpty(sourceList)) {
            List<FolderVO> resultList = sourceList.stream().filter(e -> (Objects.nonNull(e.getParentId()) && e.getParentId().equals(parent.getId()))).collect(Collectors.toList());
            //  resultList.sort(Comparator.comparing(FolderVO::getId));
            parent.setChildren(resultList);
            resultList.forEach(e -> {
                buildTree(sourceList, e);
            });
        }
    }

    @Transactional
    public Boolean deleteFolder(String source, String id) {
        List<String> ids = new ArrayList<>();
        ids.add(id);
        buildLeafIds(ids, List.of(id));
        return this.removeByIds(ids);
    }
    public void buildLeafIds(List<String> ids, List<String> parentIds) {
        //todo 超过1000个数据，会报错
        List<FolderEntity> children = this.list(Wrappers.<FolderEntity>lambdaQuery().select(FolderEntity::getId).in(FolderEntity::getParentId, parentIds));
        if (CollectionUtils.isNotEmpty(children)) {
            List<String> childrenIds = children.stream().map(FolderEntity::getId).toList();
            ids.addAll(childrenIds);
            buildLeafIds(ids, childrenIds);
        }
    }

}
