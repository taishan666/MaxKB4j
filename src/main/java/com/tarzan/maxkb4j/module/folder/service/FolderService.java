package com.tarzan.maxkb4j.module.folder.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.common.util.BatchUtil;
import com.tarzan.maxkb4j.common.util.BeanUtil;
import com.tarzan.maxkb4j.module.folder.entity.FolderEntity;
import com.tarzan.maxkb4j.module.folder.mapper.FolderMapper;
import com.tarzan.maxkb4j.module.folder.vo.FolderVO;
import com.tarzan.maxkb4j.module.system.user.domain.entity.UserEntity;
import com.tarzan.maxkb4j.module.system.user.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class FolderService extends ServiceImpl<FolderMapper, FolderEntity> {


    private final UserService userService;

    @Transactional
    public List<FolderVO> tree(String source) {
        String userId = StpUtil.getLoginIdAsString();
     /*   UserEntity loginUser = userService.getById(userId);
        if (Objects.nonNull(loginUser) && loginUser.getRole().contains("ADMIN")) {
            return getAdminFolder(userId, source);
        }*/
        return getUserFolder(userId, source);
    }

    private List<FolderVO> getUserFolder(String userId, String source) {
        FolderVO userRoot = new FolderVO(userId, "根目录", null, null, userId,List.of());
        LambdaQueryWrapper<FolderEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(FolderEntity::getSource, source);
        wrapper.eq(FolderEntity::getUserId, userId);
        List<FolderEntity> list = this.list(wrapper);
        if (CollectionUtils.isNotEmpty(list)) {
            List<FolderVO> sourceList = BeanUtil.copyList(list, FolderVO.class);
            userRoot=sourceList.stream().filter(e -> e.getId().equals(userId)).findAny().orElse(userRoot);
            buildTree(sourceList, userRoot);
        }
        return List.of(userRoot);
    }


    private List<FolderVO> getAdminFolder(String userId, String source) {
        List<UserEntity> users = userService.lambdaQuery().select(UserEntity::getId, UserEntity::getNickname, UserEntity::getRole).ne(UserEntity::getId, userId).list();
        List<FolderEntity> list = this.lambdaQuery().eq(FolderEntity::getSource, source).list();
        List<FolderVO> sourceList = BeanUtil.copyList(list, FolderVO.class);
        List<FolderVO> result = new ArrayList<>();
        FolderVO adminRoot = new FolderVO(userId, "根目录", null, null, userId,List.of());
        buildTree(sourceList, adminRoot);
        result.add(adminRoot);
        for (UserEntity user : users) {
            FolderVO userRoot = new FolderVO(user.getId(), user.getNickname(), null, null,userId, List.of());
            buildTree(sourceList, userRoot);
            result.add(userRoot);
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
        return super.removeByIds(ids);
    }

    public void buildLeafIds(List<String> ids, List<String> parentIds) {
        List<FolderEntity> children= BatchUtil.protectBach(ids, idList-> {
                    return this.list(Wrappers.<FolderEntity>lambdaQuery().select(FolderEntity::getId).in(FolderEntity::getParentId, parentIds));
                } );
        if (CollectionUtils.isNotEmpty(children)) {
            List<String> childrenIds = children.stream().map(FolderEntity::getId).toList();
            ids.addAll(childrenIds);
            buildLeafIds(ids, childrenIds);
        }
    }

    public Boolean addFolder(String source, FolderVO folder) {
        FolderEntity folderEntity = BeanUtil.copy(folder, FolderEntity.class);
        folderEntity.setUserId(StpUtil.getLoginIdAsString());
        folderEntity.setSource(source);
        return super.save(folderEntity);
    }

    public Boolean updateFolder(String source, String id, FolderVO folder) {
        FolderEntity folderEntity = BeanUtil.copy(folder, FolderEntity.class);
        folderEntity.setId(id);
        folderEntity.setSource(source);
        return super.saveOrUpdate(folderEntity);
    }
}
