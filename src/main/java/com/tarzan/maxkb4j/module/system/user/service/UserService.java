package com.tarzan.maxkb4j.module.system.user.service;

import cn.dev33.satoken.secure.SaSecureUtil;
import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tarzan.maxkb4j.exception.ApiException;
import com.tarzan.maxkb4j.module.application.enums.AuthType;
import com.tarzan.maxkb4j.module.system.setting.service.EmailService;
import com.tarzan.maxkb4j.module.system.team.entity.TeamEntity;
import com.tarzan.maxkb4j.module.system.team.service.TeamService;
import com.tarzan.maxkb4j.module.system.user.dto.PasswordDTO;
import com.tarzan.maxkb4j.module.system.user.dto.UserDTO;
import com.tarzan.maxkb4j.module.system.user.dto.UserLoginDTO;
import com.tarzan.maxkb4j.module.system.user.entity.UserEntity;
import com.tarzan.maxkb4j.module.system.user.mapper.UserMapper;
import com.tarzan.maxkb4j.module.system.user.vo.UserVO;
import com.tarzan.maxkb4j.util.BeanUtil;
import jakarta.mail.MessagingException;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author tarzan
 * @date 2024-12-25 11:27:27
 */
@Service
@AllArgsConstructor
public class UserService extends ServiceImpl<UserMapper, UserEntity> {

    private final TeamService teamService;
    private final EmailService emailService;
    private final StpInterface stpInterface;

    // 创建缓存并配置
    private static final Cache<String, String> AUTH_CODE_CACHE = Caffeine.newBuilder()
            .initialCapacity(5)
            // 超出最大容量时淘汰
            .maximumSize(100000)
            //设置写缓存后n秒钟过期
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .expireAfterAccess(3, TimeUnit.MINUTES) // 最近访问后5分钟过期
            .build();

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
        if(Objects.isNull(userEntity)){
            throw new ApiException("用户名或密码错误");
        }
        if(!userEntity.getIsActive()){
            throw new ApiException("该用户已被禁用，请联系管理员！");
        }
        SaLoginModel loginModel = new SaLoginModel();
        loginModel.setExtra("username", userEntity.getUsername());
        loginModel.setExtra("email", userEntity.getEmail());
        loginModel.setExtra("language", userEntity.getLanguage());
        loginModel.setExtra("client_id", userEntity.getId());
        loginModel.setExtra("client_type", AuthType.USER.name());
        StpUtil.login(userEntity.getId(),loginModel);
        return StpUtil.getTokenValue();
    }

    @Transactional
    public boolean createUser(UserEntity user) {
      //  UserEntity admin= this.getById(StpUtil.getLoginIdAsString());
        user.setRole("USER");
        user.setIsActive(true);
        user.setSource("LOCAL");
        user.setLanguage((String) StpUtil.getExtra("language"));
        save(user);
        TeamEntity team = new TeamEntity();
        team.setUserId(user.getId());
        team.setName(user.getUsername()+"的团队");
        return teamService.save(team);
    }

    public UserVO getUserById(String userId) {
        UserEntity userEntity = this.getById(userId);
        if (Objects.isNull(userEntity)){
            return null;
        }
        UserVO user = BeanUtil.copy(userEntity, UserVO.class);
        user.setPermissions(stpInterface.getPermissionList(userId, null));
        user.setIsEditPassword("d880e722c47a34d8e9fce789fc62389d".equals(user.getPassword())&&"ADMIN".equals(user.getRole()));
        return user;
    }

    public Boolean sendEmailCode() throws MessagingException {
       // UserEntity user= this.getById(StpUtil.getLoginIdAsString());
        Context context = new Context();
        String code = generateCode();
        context.setVariable("code", code);
        AUTH_CODE_CACHE.put((String) StpUtil.getExtra("username"), code);
        emailService.sendMessage((String) StpUtil.getExtra("email"), "【智能知识库问答系统-修改密码】", "email_template", context);
        return true;
    }

    private String  generateCode(){
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }

    public Boolean resetPassword(PasswordDTO dto) {
        //UserEntity user= this.getById(StpUtil.getLoginIdAsString());
        String code=AUTH_CODE_CACHE.getIfPresent(StpUtil.getExtra("username"));
        if(dto.getCode().equals(code)){
            if(dto.getPassword().equals(dto.getRePassword())){
                UserEntity userEntity = new UserEntity();
                userEntity.setId(StpUtil.getLoginIdAsString());
                userEntity.setPassword(SaSecureUtil.md5(dto.getPassword()));
                return updateById(userEntity);
            }
        }
        return false;
    }

    public Boolean updatePassword(String id, PasswordDTO dto) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setPassword(SaSecureUtil.md5(dto.getPassword()));
        return updateById(user);
    }

    public Boolean updateLanguage(UserEntity user) {
        user.setId(StpUtil.getLoginIdAsString());
        return updateById(user);
    }

    public UserEntity validUserById(String id) {
        //todo
        return this.getById(id);
    }
}
