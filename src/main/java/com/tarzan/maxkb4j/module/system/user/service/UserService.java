package com.tarzan.maxkb4j.module.system.user.service;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.jwt.StpLogicJwtForStateless;
import cn.dev33.satoken.secure.SaSecureUtil;
import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpLogic;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tarzan.maxkb4j.common.exception.ApiException;
import com.tarzan.maxkb4j.common.exception.LoginException;
import com.tarzan.maxkb4j.common.props.SystemProperties;
import com.tarzan.maxkb4j.common.util.BeanUtil;
import com.tarzan.maxkb4j.common.util.StpKit;
import com.tarzan.maxkb4j.module.application.enums.ChatUserType;
import com.tarzan.maxkb4j.module.system.setting.service.EmailService;
import com.tarzan.maxkb4j.module.system.user.constants.RoleType;
import com.tarzan.maxkb4j.module.system.user.constants.UserSource;
import com.tarzan.maxkb4j.module.system.user.domain.dto.PasswordDTO;
import com.tarzan.maxkb4j.module.system.user.domain.dto.UserDTO;
import com.tarzan.maxkb4j.module.system.user.domain.dto.UserLoginDTO;
import com.tarzan.maxkb4j.module.system.user.domain.entity.UserEntity;
import com.tarzan.maxkb4j.module.system.user.domain.vo.UserVO;
import com.tarzan.maxkb4j.module.system.user.mapper.UserMapper;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author tarzan
 * @date 2024-12-25 11:27:27
 */
@Service
@RequiredArgsConstructor
public class UserService extends ServiceImpl<UserMapper, UserEntity> {

    private final EmailService emailService;
    private final StpInterface stpInterface;
    private final SystemProperties systemProperties;
    // 创建缓存并配置
    private static final Cache<String, String> AUTH_CODE_CACHE = Caffeine.newBuilder()
            .initialCapacity(5)
            // 超出最大容量时淘汰
            .maximumSize(100000)
            //设置写缓存后n秒钟过期
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build();

    public IPage<UserEntity> selectUserPage(int page, int size, UserDTO dto) {
        Page<UserEntity> userPage = new Page<>(page, size);
        LambdaQueryWrapper<UserEntity> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.isNotBlank(dto.getNickname())) {
            wrapper.like(UserEntity::getNickname, dto.getNickname());
        }
        if (StringUtils.isNotBlank(dto.getUsername())) {
            wrapper.like(UserEntity::getUsername, dto.getUsername());
        }
        if (StringUtils.isNotBlank(dto.getEmail())) {
            wrapper.like(UserEntity::getEmail, dto.getEmail());
        }
        if (Objects.nonNull(dto.getIsActive())) {
            wrapper.eq(UserEntity::getIsActive, dto.getIsActive());
        }
        wrapper.orderByDesc(UserEntity::getCreateTime);
        return this.page(userPage, wrapper);
    }

    @Transactional
    public boolean deleteUserById(String userId) {
        return removeById(userId);
    }


    public String login(UserLoginDTO dto, HttpServletRequest request) {
        HttpSession session = request.getSession();
        String sessionCaptcha = (String) session.getAttribute("captcha");
        if (StringUtils.isBlank(sessionCaptcha)) {
            throw new LoginException("验证码已过期");
        }
        if (Objects.nonNull(dto.getCaptcha()) && !sessionCaptcha.equals(dto.getCaptcha().toLowerCase())) {
            throw new LoginException("验证码错误");
        }
        String password = SaSecureUtil.md5(dto.getPassword());
        UserEntity userEntity = this.lambdaQuery()
                .eq(UserEntity::getUsername, dto.getUsername())
                .eq(UserEntity::getPassword, password).one();
        if (Objects.isNull(userEntity)) {
            throw new LoginException("用户名或密码错误");
        }
        if (!userEntity.getIsActive()) {
            throw new LoginException("该用户已被禁用，请联系管理员！");
        }
        SaLoginModel loginModel = new SaLoginModel();
        loginModel.setExtra("username", userEntity.getUsername());
        loginModel.setExtra("email", userEntity.getEmail());
        loginModel.setExtra("language", userEntity.getLanguage());
        loginModel.setExtra("chatUserId", userEntity.getId());
        loginModel.setExtra("chatUserType", ChatUserType.CHAT_USER.name());
        loginModel.setExtra("roles", userEntity.getRole());
        StpKit.ADMIN.login(userEntity.getId(), loginModel);
        return StpKit.ADMIN.getTokenValue();
    }

    @Transactional
    public boolean createUser(UserEntity user) {
        long usernameNum = this.lambdaQuery().eq(UserEntity::getUsername, user.getUsername()).count();
        if (usernameNum > 0) {
            throw new ApiException("用户名已存在");
        }
        long emailNum = this.lambdaQuery().eq(UserEntity::getEmail, user.getEmail()).count();
        if (emailNum > 0) {
            throw new ApiException("邮箱已存在");
        }
        user.setRole(Set.of(RoleType.USER));
        user.setIsActive(true);
        user.setSource(UserSource.LOCAL);
        user.setLanguage((String) StpKit.ADMIN.getExtra("language"));
        user.setPassword(SaSecureUtil.md5(user.getPassword()));
        return save(user);
    }

    @Transactional
    public void createAdminUser(String username, String password) {
        UserEntity user = new UserEntity();
        user.setNickname("系统管理员");
        user.setUsername(username);
        user.setPassword(SaSecureUtil.md5(password));
        user.setRole(Set.of(RoleType.ADMIN));
        user.setIsActive(true);
        user.setSource(UserSource.LOCAL);
        user.setLanguage("zh-CN");
        user.setPhone("");
        user.setEmail("1334512682@qq.com");
        save(user);
    }

    public UserVO getUserById(String userId) {
        UserEntity userEntity = this.getById(userId);
        if (Objects.isNull(userEntity)) {
            throw new NotLoginException("用户不存在", "", "");
        }
        UserVO user = BeanUtil.copy(userEntity, UserVO.class);
        user.setPermissions(stpInterface.getPermissionList(userId, null));
        if (user.getRole().contains("ADMIN")){
            userEntity.getRole().add("WORKSPACE_MANAGE:/WORKSPACE/default");
        }else {
            user.setRole(Set.of("USER:/WORKSPACE/default"));
        }
        List<Map<String, String>> workspaceList = new ArrayList<>();
        workspaceList.add(Map.of("id", "default", "name", "default"));
        user.setWorkspaceList(workspaceList);
        String defaultPassword = SaSecureUtil.md5(systemProperties.getDefaultPassword());
        user.setIsEditPassword(defaultPassword.equals(user.getPassword()));
        return user;
    }

    public Boolean sendEmailCode() throws MessagingException {
        return sendEmailCode((String) StpKit.ADMIN.getExtra("email"), "【智能知识库问答系统-修改密码】");
    }

    public Boolean sendEmailCode(String email, String subject) throws MessagingException {
        Context context = new Context();
        String code = generateCode();
        context.setVariable("code", code);
        AUTH_CODE_CACHE.put(email, code);
        emailService.sendMessage(email, subject, "email_template", context);
        return true;
    }

    public boolean checkCode(String email, String code) {
        String codeCache=AUTH_CODE_CACHE.getIfPresent(email);
        return code.equals(codeCache);
    }

    private String generateCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }

    public Boolean resetPassword(PasswordDTO dto) {
        if (dto.getPassword().equals(dto.getRePassword())) {
            UserEntity userEntity = new UserEntity();
            userEntity.setId(StpKit.ADMIN.getLoginIdAsString());
            userEntity.setPassword(SaSecureUtil.md5(dto.getPassword()));
            return updateById(userEntity);
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
        String userId = StpKit.ADMIN.getLoginIdAsString();
        user.setId(userId);
        StpLogic stpLogic = new StpLogicJwtForStateless();
        String token = stpLogic.createTokenValue(userId, "default-device", StpKit.ADMIN.getTokenTimeout(), Map.of("language", user.getLanguage()));
        StpKit.ADMIN.setTokenValue(token);
        return updateById(user);
    }


    public Set<String>  getRoleById(String id) {
        UserEntity user= this.lambdaQuery().select(UserEntity::getRole).eq(UserEntity::getId, id).one();
        if (Objects.isNull(user)){
            return Set.of();
        }
        return user.getRole();
    }

    public Map<String, String> getNicknameMap() {
        return this.lambdaQuery().select(UserEntity::getId, UserEntity::getNickname).list().stream().collect(Collectors.toMap(UserEntity::getId, UserEntity::getNickname));
    }


}
