package com.tarzan.maxkb4j.module.system.user.service;

import cn.dev33.satoken.stp.StpInterface;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tarzan.maxkb4j.module.system.user.entity.UserEntity;
import com.tarzan.maxkb4j.module.system.user.enums.PermissionEnum;
import com.tarzan.maxkb4j.module.system.user.mapper.UserMapper;
import com.tarzan.maxkb4j.module.system.user.vo.PermissionVO;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 自定义权限加载接口实现类
 */
@AllArgsConstructor
@Component    // 保证此类被 SpringBoot 扫描，完成 Sa-Token 的自定义权限验证扩展
public class StpInterfaceImpl implements StpInterface {

    private final UserMapper userMapper;

    private final Cache<String, List<String>> permissionCache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    private final Cache<String, List<String>> roleCache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    /**
     * 返回一个账号所拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        UserEntity user = userMapper.selectById((String) loginId);
        return permissionCache.get(loginId.toString(), id -> {
            List<PermissionVO> permissionVOS=userMapper.getUserPermissionById((String) loginId);
            List<String> permissions=new ArrayList<>();
            for (PermissionVO permission : permissionVOS) {
                String operateItems = permission.getOperate();
                String noBraces = operateItems.replace("{", "").replace("}", "");
                Arrays.stream(noBraces.split(",")).forEach(item->{
                    String operate = permission.getType() +
                            ":" +
                            item +
                            ":" +
                            permission.getId();
                    permissions.add(operate);
                });
            }
            PermissionEnum.getAllPermissions().stream().filter(e-> e.getRoles().contains(user.getRole())).forEach(e->{
                String operate = e.getGroup() + ":" + e.getOperate();
                permissions.add(operate);
            });
           // permissions.add("x-pack");
            return permissions;
        });
    }

    /**
     * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return roleCache.get(loginId.toString(), id -> {
            UserEntity user = userMapper.selectById((String) loginId);
            String role = user.getRole()==null?"USER":user.getRole();
            return List.of(role);
        });
    }

}
