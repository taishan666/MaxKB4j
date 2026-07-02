package com.maxkb4j.system.service.impl;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.secure.SaSecureUtil;
import cn.dev33.satoken.stp.StpInterface;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.common.cache.AuthCodeCache;
import com.maxkb4j.common.cache.SystemCache;
import com.maxkb4j.common.constant.RoleType;
import com.maxkb4j.common.exception.ApiException;
import com.maxkb4j.common.exception.LoginException;
import com.maxkb4j.common.props.SystemProperties;
import com.maxkb4j.common.util.BeanUtil;
import com.maxkb4j.common.util.I18nUtil;
import com.maxkb4j.common.util.RSAUtil;
import com.maxkb4j.common.util.StpKit;
import com.maxkb4j.system.constant.UserSource;
import com.maxkb4j.system.service.EmailService;
import com.maxkb4j.user.dto.PasswordDTO;
import com.maxkb4j.user.dto.UserDTO;
import com.maxkb4j.user.dto.UserLoginDTO;
import com.maxkb4j.user.entity.UserEntity;
import com.maxkb4j.user.mapper.UserMapper;
import com.maxkb4j.user.service.IUserService;
import com.maxkb4j.user.vo.UserVO;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author tarzan
 * @date 2024-12-25 11:27:27
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements IUserService {

    private final EmailService emailService;
    private final StpInterface stpInterface;
    private final SystemProperties systemProperties;


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
        String encryptedData = dto.getEncryptedData();
        try {
            String text = RSAUtil.rsaLongDecrypt(encryptedData, SystemCache.getPrivateKey());
            dto = JSON.to(UserLoginDTO.class,text);
        } catch (Exception e) {
            throw new LoginException("密码解密错误");
        }
        HttpSession session = request.getSession();
        String sessionCaptcha = (String) session.getAttribute("captcha");
        //清除验证码
        session.removeAttribute("captcha");
        if (StringUtils.isBlank(sessionCaptcha)) {
            throw new LoginException("login.captcha.expired");
        }
        if (Objects.nonNull(dto.getCaptcha()) && !sessionCaptcha.equals(dto.getCaptcha().toLowerCase())) {
            throw new LoginException("login.captcha.error");
        }
        String password = SaSecureUtil.md5(dto.getPassword());
        UserEntity userEntity = this.lambdaQuery()
                .select(UserEntity::getId, UserEntity::getIsActive,  UserEntity::getLanguage)
                .eq(UserEntity::getUsername, dto.getUsername())
                .eq(UserEntity::getPassword, password)
                .eq(UserEntity::getSource, UserSource.LOCAL)
                .one();
        if (Objects.isNull(userEntity)) {
            throw new LoginException("login.user.not.exists");
        }
        if (!userEntity.getIsActive()) {
            throw new LoginException("login.user.disabled");
        }
        // 登录成功后立刻按用户表语言切换当前请求的返回消息
        LocaleContextHolder.setLocale(userEntity.getLanguage() != null && userEntity.getLanguage().toLowerCase().startsWith("en") ? Locale.US : Locale.SIMPLIFIED_CHINESE);
        StpKit.ADMIN.login(userEntity.getId());
        return StpKit.ADMIN.getTokenValue();
    }

    @Transactional
    public boolean createUser(UserEntity user) {
        long usernameNum = this.lambdaQuery().eq(UserEntity::getUsername, user.getUsername()).count();
        if (usernameNum > 0) {
            throw new ApiException("user.username.exists");
        }
        long emailNum = this.lambdaQuery().eq(UserEntity::getEmail, user.getEmail()).count();
        if (emailNum > 0) {
            throw new ApiException("user.email.exists");
        }
        user.setRole(RoleType.USER);
        user.setIsActive(true);
        user.setSource(UserSource.LOCAL);
        user.setLanguage(StringUtils.defaultIfBlank(user.getLanguage(), "zh-CN"));
        user.setPassword(SaSecureUtil.md5(user.getPassword()));
        return save(user);
    }

    @Transactional
    public void createAdminUser(String username, String password) {
        UserEntity user = new UserEntity();
        user.setNickname(I18nUtil.get("user.admin.nickname"));
        user.setUsername(username);
        user.setPassword(SaSecureUtil.md5(password));
        user.setRole(RoleType.ADMIN);
        user.setIsActive(true);
        user.setSource(UserSource.LOCAL);
        user.setLanguage("zh-CN");
        user.setPhone("13843838438");
        user.setEmail("1334512682@qq.com");
        save(user);
    }

    public UserVO getUserById(String userId) {
        UserEntity userEntity = this.getById(userId);
        if (Objects.isNull(userEntity)) {
            throw new NotLoginException(I18nUtil.get("login.user.not.found"), "", "");
        }
        UserVO user = BeanUtil.copy(userEntity, UserVO.class);
        Set<String> role=new HashSet<>();
        role.add(userEntity.getRole());
        user.setPermissions(stpInterface.getPermissionList(userId, null));
        if (RoleType.ADMIN.equals(userEntity.getRole())) {
            role.add("WORKSPACE_MANAGE:/WORKSPACE/default");
        } else {
            role.add("USER:/WORKSPACE/default");
        }
        user.setRole(role);
        List<Map<String, String>> workspaceList = new ArrayList<>();
        workspaceList.add(Map.of("id", "default", "name", "default"));
        user.setWorkspaceList(workspaceList);
        String defaultPassword = SaSecureUtil.md5(systemProperties.getDefaultPassword());
        user.setIsEditPassword(defaultPassword.equals(userEntity.getPassword()));
        return user;
    }

    public Boolean sendEmailCode() throws MessagingException {
        String userId = StpKit.ADMIN.getLoginIdAsString();
        return sendEmailCode(getEmail(userId), I18nUtil.get("email.subject.modify.password"));
    }

    public Boolean sendEmailCode(String email, String subject) throws MessagingException {
        Context context = new Context();
        String code = generateCode();
        context.setVariable("code", code);
        AuthCodeCache.put(email, code);
        emailService.sendMessage(email, subject, "email_template", context);
        return true;
    }

    public boolean checkCode(String email, String code) {
        String codeCache = AuthCodeCache.getIfPresent(email);
        return  Objects.equals(codeCache, code);
    }

    private String generateCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }

    public Boolean resetPassword(PasswordDTO dto) {
        if (Objects.equals(dto.getPassword(), dto.getRePassword())) {
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
        return updateById(user);
    }


    public Set<String> getRoleById(String id) {
        UserEntity user = this.lambdaQuery().select(UserEntity::getRole).eq(UserEntity::getId, id).one();
        if (Objects.isNull(user)) {
            return Set.of();
        }
        return Set.of(user.getRole());
    }

    public Map<String, String> getNicknameMap() {
        return this.lambdaQuery().select(UserEntity::getId, UserEntity::getNickname).list().stream().collect(Collectors.toMap(UserEntity::getId, UserEntity::getNickname));
    }

    @Override
    public String getUsername(String userId) {
        List<UserEntity> list = this.lambdaQuery().select(UserEntity::getUsername).eq(UserEntity::getId, userId).list();
        return list.isEmpty() ? "" : list.getFirst().getUsername();
    }

    public String getNickname(String userId) {
        List<UserEntity> list = this.lambdaQuery().select(UserEntity::getNickname).eq(UserEntity::getId, userId).list();
        return list.isEmpty() ? "" : list.getFirst().getNickname();
    }

    @Override
    public String getEmail(String userId) {
        List<UserEntity> list = this.lambdaQuery().select(UserEntity::getEmail).eq(UserEntity::getId, userId).list();
        return list.isEmpty() ? "" : list.getFirst().getEmail();
    }

    @Override
    public String getLanguage(String userId) {
        List<UserEntity> list = this.lambdaQuery().select(UserEntity::getLanguage).eq(UserEntity::getId, userId).list();
        return list.isEmpty() ? "" : list.getFirst().getLanguage();
    }


}
