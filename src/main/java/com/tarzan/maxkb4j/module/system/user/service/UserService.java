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
import com.tarzan.maxkb4j.module.system.user.mapper.UserMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

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
    public boolean deleteUserById(UUID userId) {
         boolean f1=teamService.deleteUserById(userId);
         boolean f2=removeById(userId);
        return true;
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
}
