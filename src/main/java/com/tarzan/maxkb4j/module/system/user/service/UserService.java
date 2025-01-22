package com.tarzan.maxkb4j.module.system.user.service;

import cn.dev33.satoken.secure.SaSecureUtil;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.system.team.service.TeamService;
import com.tarzan.maxkb4j.module.system.user.dto.UserDTO;
import com.tarzan.maxkb4j.module.system.user.dto.UserLoginDTO;
import com.tarzan.maxkb4j.module.system.user.entity.UserEntity;
import com.tarzan.maxkb4j.module.system.user.enums.PermissionEnum;
import com.tarzan.maxkb4j.module.system.user.mapper.UserMapper;
import com.tarzan.maxkb4j.module.system.user.vo.PermissionVO;
import com.tarzan.maxkb4j.module.system.user.vo.UserVO;
import com.tarzan.maxkb4j.util.BeanUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * @author tarzan
 * @date 2024-12-25 11:27:27
 */
@Service
public class UserService extends ServiceImpl<UserMapper, UserEntity> {

    @Autowired
    private TeamService teamService;


    public IPage<UserEntity> selectUserPage(int page, int size, String emailOrUsername) {
        Page<UserEntity> userPage = new Page<>(page, size);
        LambdaQueryWrapper<UserEntity> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.isNotBlank(emailOrUsername)) {
            wrapper.like(UserEntity::getEmail, emailOrUsername)
                    .or().like(UserEntity::getUsername, emailOrUsername);
        }
        wrapper.orderByDesc(UserEntity::getCreateTime);
        return this.page(userPage, wrapper);
    }

    @Transactional
    public boolean deleteUserById(String userId) {
        boolean f1 = teamService.deleteUserById(userId);
        boolean f2 = removeById(userId);
        return f1&&f2;
    }

    public List<UserDTO> listByType(String type) {
        List<UserDTO> list = new ArrayList<>(2);
        list.add(new UserDTO("all", "全部"));
        list.add(new UserDTO(StpUtil.getLoginIdAsString(), "我的"));
        return list;
    }

    public String login(UserLoginDTO dto) {
        String password = SaSecureUtil.md5(dto.getPassword());
        UserEntity userEntity = this.lambdaQuery()
                .eq(UserEntity::getUsername, dto.getUsername())
                .eq(UserEntity::getPassword, password).one();
        if (Objects.nonNull(userEntity)) {
            StpUtil.login(userEntity.getId().toString());
            SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
            System.out.println(tokenInfo);
            return tokenInfo.getTokenValue();
        }
        return null;
    }

    public boolean createUser(UserEntity user) {
        user.setRole("USER");
        user.setIsActive(true);
        user.setSource("LOCAL");
        return save(user);
    }

    public UserVO getUserById(String userId) {
        UserEntity userEntity = this.getById(userId);
        if (Objects.isNull(userEntity)){
            return null;
        }
        UserVO user = BeanUtil.copy(userEntity, UserVO.class);
        List<PermissionVO> permissionVOS=baseMapper.getUserPermissionById(userId);
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
        PermissionEnum.getAllPermissions().forEach(e->{
            String operate = e.getGroup() + ":" + e.getOperate();
            permissions.add(operate);
        });
        user.setPermissions(permissions);
        user.setIsEditPassword("d880e722c47a34d8e9fce789fc62389d".equals(user.getPassword())&&"ADMIN".equals(user.getRole()));
        return user;
    }
}
