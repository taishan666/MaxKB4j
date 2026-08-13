package com.maxkb4j.system.service.impl;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpInterface;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.system.cache.AuthCodeCache;
import com.maxkb4j.common.cache.SystemCache;
import com.maxkb4j.common.constant.RoleType;
import com.maxkb4j.common.context.UserContext;
import com.maxkb4j.common.exception.ApiException;
import com.maxkb4j.common.exception.LoginException;
import com.maxkb4j.common.props.SystemProperties;
import com.maxkb4j.common.util.BeanUtil;
import com.maxkb4j.common.util.I18nUtil;
import com.maxkb4j.common.util.RSAUtil;
import com.maxkb4j.common.util.StpKit;
import com.maxkb4j.common.util.WebUtil;
import com.maxkb4j.system.constant.UserLanguage;
import com.maxkb4j.system.constant.UserSource;
import com.maxkb4j.system.entity.UserEntity;
import com.maxkb4j.system.mapper.UserMapper;
import com.maxkb4j.system.service.EmailService;
import com.maxkb4j.system.dto.PasswordDTO;
import com.maxkb4j.system.dto.UserDTO;
import com.maxkb4j.system.dto.UserLoginDTO;
import com.maxkb4j.system.service.IUserInternalService;
import com.maxkb4j.system.security.LoginAttemptLimiter;
import com.maxkb4j.system.security.PasswordService;
import com.maxkb4j.system.vo.UserNameVO;
import com.maxkb4j.system.vo.UserVO;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;

import java.security.SecureRandom;
import java.util.*;

/**
 * @author tarzan
 * @date 2024-12-25 11:27:27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements IUserInternalService {

    private final EmailService emailService;
    private final StpInterface stpInterface;
    private final SystemProperties systemProperties;
    private final UserContext userContext;
    private final PasswordService passwordService;
    private final LoginAttemptLimiter loginAttemptLimiter;
    private final AuthCodeCache authCodeCache;


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
        if (StringUtils.isNotBlank(dto.getEncryptedData())){
            try {
                String encryptedData = dto.getEncryptedData();
                String text = RSAUtil.rsaLongDecrypt(encryptedData, SystemCache.getPrivateKey());
                dto = JSON.parseObject(text, UserLoginDTO.class);
            } catch (Exception e) {
                throw new LoginException("login.password.decrypt.error");
            }
        }
        if (StringUtils.isBlank(dto.getPassword())){
            throw new LoginException("user.password.empty");
        }
        HttpSession session = request.getSession();
        String sessionCaptcha = (String) session.getAttribute("captcha");
        //清除验证码
        session.removeAttribute("captcha");
        if (StringUtils.isBlank(sessionCaptcha)) {
            throw new LoginException("login.captcha.expired");
        }
        if (StringUtils.isBlank(dto.getCaptcha()) || !sessionCaptcha.equalsIgnoreCase(dto.getCaptcha())) {
            throw new LoginException("login.captcha.error");
        }
        String limiterKey = buildLimiterKey(dto.getUsername());
        loginAttemptLimiter.checkLocked(limiterKey);
        UserEntity userEntity = this.lambdaQuery()
                .select(UserEntity::getId, UserEntity::getIsActive, UserEntity::getLanguage, UserEntity::getPassword)
                .eq(UserEntity::getUsername, dto.getUsername())
                .eq(UserEntity::getSource, UserSource.LOCAL)
                .one();
        // 用户不存在与口令错误统一提示，避免用户名枚举
        if (Objects.isNull(userEntity) || !passwordService.matches(dto.getPassword(), userEntity.getPassword())) {
            loginAttemptLimiter.recordFailure(limiterKey);
            throw new LoginException("login.user.not.exists");
        }
        if (!userEntity.getIsActive()) {
            throw new LoginException("login.user.disabled");
        }
        // 存量 MD5 口令验证通过后自动升级为 BCrypt
        if (passwordService.isLegacyHash(userEntity.getPassword())) {
            UserEntity upgrade = new UserEntity();
            upgrade.setId(userEntity.getId());
            upgrade.setPassword(passwordService.encode(dto.getPassword()));
            this.updateById(upgrade);
        }
        loginAttemptLimiter.recordSuccess(limiterKey);
        // 登录成功后立刻按用户表语言切换当前请求的返回消息
        LocaleContextHolder.setLocale(userEntity.getLanguage() != null && userEntity.getLanguage().toLowerCase().startsWith("en") ? Locale.US : Locale.SIMPLIFIED_CHINESE);
        StpKit.ADMIN.login(userEntity.getId());
        return StpKit.ADMIN.getTokenValue();
    }

    /**
     * 构建登录限流键：用户名 + 来源 IP，避免单一维度误伤。
     */
    private String buildLimiterKey(String username) {
        return StringUtils.defaultString(username) + "|" + WebUtil.getIP();
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
        user.setLanguage(StringUtils.defaultIfBlank(user.getLanguage(), UserLanguage.ZH_CN));
        user.setPassword(passwordService.encode(user.getPassword()));
        return save(user);
    }

    @Transactional
    public void createDefaultAdminUser() {
        String defaultPassword = systemProperties.getDefaultPassword();
        if (StringUtils.isBlank(defaultPassword)) {
            defaultPassword = generateRandomPassword(16);
            systemProperties.setDefaultPassword(defaultPassword);
            log.warn("==================================================================");
            log.warn("未配置 SYSTEM_DEFAULT_PASSWORD，已为默认管理员账号生成随机口令：{}", defaultPassword);
            log.warn("请登录后及时修改口令；或通过环境变量 SYSTEM_DEFAULT_PASSWORD 配置固定默认口令");
            log.warn("==================================================================");
        }
        UserEntity user = new UserEntity();
        user.setNickname(I18nUtil.get("user.admin.nickname"));
        user.setUsername(systemProperties.getDefaultUsername());
        user.setPassword(passwordService.encode(defaultPassword));
        user.setRole(RoleType.ADMIN);
        user.setIsActive(true);
        user.setSource(UserSource.LOCAL);
        user.setLanguage(UserLanguage.ZH_CN);
        user.setPhone(systemProperties.getDefaultPhone());
        user.setEmail(systemProperties.getDefaultEmail());
        save(user);
    }

    public UserVO getUserById(String userId) {
        UserEntity userEntity = this.lambdaQuery()
                .select(UserEntity::getId, UserEntity::getEmail, UserEntity::getPhone,
                        UserEntity::getNickname, UserEntity::getUsername, UserEntity::getPassword,
                        UserEntity::getRole, UserEntity::getIsActive, UserEntity::getSource,
                        UserEntity::getLanguage)
                .eq(UserEntity::getId, userId)
                .one();
        if (Objects.isNull(userEntity)) {
            throw new NotLoginException(I18nUtil.get("login.user.not.found"), "", "");
        }
        UserVO user = BeanUtil.copy(userEntity, UserVO.class);
        user.setPermissions(stpInterface.getPermissionList(userId, null));
        user.setRoleName(Set.of(userEntity.getRole()));
        Set<String> role=new HashSet<>();
        role.add(userEntity.getRole());
        if (RoleType.ADMIN.equals(userEntity.getRole())) {
            role.add(RoleType.WORKSPACE_MANAGE+":/WORKSPACE/default");
        } else {
            role.add(RoleType.USER+":/WORKSPACE/default");
        }
        user.setRole(role);
        List<Map<String, String>> workspaceList = new ArrayList<>();
        workspaceList.add(Map.of("id", "default", "name", "default"));
        user.setWorkspaceList(workspaceList);
        String defaultPassword = systemProperties.getDefaultPassword();
        user.setIsEditPassword(StringUtils.isNotBlank(defaultPassword)
                && passwordService.matches(defaultPassword, userEntity.getPassword()));
        return user;
    }

    public Boolean sendEmailCode(String email, String subject) {
        Context context = new Context();
        String code = generateCode();
        context.setVariable("code", code);
        authCodeCache.put(email, code);
        try {
            emailService.sendMessage(email, subject, "email_template", context);
        } catch (MessagingException e) {
            throw new ApiException(e.getMessage());
        }
        return true;
    }

    public boolean checkCode(String email, String code) {
        String codeCache = authCodeCache.getIfPresent(email);
        return  Objects.equals(codeCache, code);
    }

    private String generateCode() {
        return String.format("%06d", new SecureRandom().nextInt(1000000));
    }

    /**
     * 生成随机口令：去除易混淆字符的大小写字母与数字。
     */
    private String generateRandomPassword(int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public boolean resetPassword(PasswordDTO dto) {
        if (Objects.equals(dto.getPassword(), dto.getRePassword())) {
            UserEntity userEntity = new UserEntity();
            userEntity.setId(userContext.getUserId());
            userEntity.setPassword(passwordService.encode(dto.getPassword()));
            return updateById(userEntity);
        }
        return false;
    }

    public boolean batchDelete(List<String> ids) {
        return this.lambdaUpdate().eq(UserEntity::getRole, RoleType.USER).in(UserEntity::getId, ids).remove();
    }

    public boolean updatePassword(String id, PasswordDTO dto) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setPassword(passwordService.encode(dto.getPassword()));
        return updateById(user);
    }

    public boolean updateLanguage(UserEntity user) {
        String userId = userContext.getUserId();
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
        return this.lambdaQuery().select(UserEntity::getId, UserEntity::getNickname).list().stream()
                .collect(HashMap::new, (m, e) -> m.put(e.getId(), e.getNickname()), HashMap::putAll);
    }

    public List<UserNameVO> listActiveUserNames() {
        List<UserEntity> userList = this.lambdaQuery().eq(UserEntity::getIsActive, true).list();
        return BeanUtil.copyList(userList, UserNameVO.class);
    }

    public List<UserEntity> listActiveMembers() {
        return this.lambdaQuery()
                .eq(UserEntity::getRole, RoleType.USER)
                .eq(UserEntity::getIsActive, true)
                .list();
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
