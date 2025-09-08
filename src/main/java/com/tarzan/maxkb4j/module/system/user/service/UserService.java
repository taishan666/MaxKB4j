package com.tarzan.maxkb4j.module.system.user.service;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.jwt.StpLogicJwtForStateless;
import cn.dev33.satoken.secure.SaSecureUtil;
import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tarzan.maxkb4j.core.exception.ApiException;
import com.tarzan.maxkb4j.module.application.enums.AuthType;
import com.tarzan.maxkb4j.module.system.setting.service.EmailService;
import com.tarzan.maxkb4j.module.system.user.constants.RoleType;
import com.tarzan.maxkb4j.module.system.user.constants.UserSource;
import com.tarzan.maxkb4j.module.system.user.domain.dto.PasswordDTO;
import com.tarzan.maxkb4j.module.system.user.domain.dto.UserDTO;
import com.tarzan.maxkb4j.module.system.user.domain.dto.UserLoginDTO;
import com.tarzan.maxkb4j.module.system.user.domain.entity.UserEntity;
import com.tarzan.maxkb4j.module.system.user.domain.vo.UserVO;
import com.tarzan.maxkb4j.module.system.user.mapper.UserMapper;
import com.tarzan.maxkb4j.util.BeanUtil;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
public class UserService extends ServiceImpl<UserMapper, UserEntity> {

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

    public List<UserDTO> listByType(String type) {
        List<UserDTO> list = new ArrayList<>(2);
        list.add(new UserDTO("all", "全部"));
        list.add(new UserDTO(StpUtil.getLoginIdAsString(), "我的"));
        return list;
    }

    public String login(UserLoginDTO dto, HttpServletRequest request) {
        HttpSession session = request.getSession();
        String sessionCaptcha = (String) session.getAttribute("captcha");
        if (StringUtils.isBlank(sessionCaptcha)) {
            throw new ApiException("验证码已过期");
        }
        if (Objects.nonNull(dto.getCaptcha()) && !sessionCaptcha.equals(dto.getCaptcha().toLowerCase())) {
            throw new ApiException("验证码错误");
        }
        String password = SaSecureUtil.md5(dto.getPassword());
        UserEntity userEntity = this.lambdaQuery()
                .eq(UserEntity::getUsername, dto.getUsername())
                .eq(UserEntity::getPassword, password).one();
        if (Objects.isNull(userEntity)) {
            throw new ApiException("用户名或密码错误");
        }
        if (!userEntity.getIsActive()) {
            throw new ApiException("该用户已被禁用，请联系管理员！");
        }
        SaLoginModel loginModel = new SaLoginModel();
        loginModel.setExtra("username", userEntity.getUsername());
        loginModel.setExtra("email", userEntity.getEmail());
        loginModel.setExtra("language", userEntity.getLanguage());
        loginModel.setExtra("client_id", userEntity.getId());
        loginModel.setExtra("client_type", AuthType.USER.name());
        StpUtil.login(userEntity.getId(), loginModel);
        //return Map.of("token",StpUtil.getTokenValue());
        return StpUtil.getTokenValue();
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
        user.setLanguage((String) StpUtil.getExtra("language"));
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
        List<String> permissions = new ArrayList<>();
        permissions.add("APPLICATION_OVERVIEW:READ+EMBED:/WORKSPACE/default/APPLICATION/019845dc-1f97-7553-b930-458cbe08c5c4");
        permissions.add("APPLICATION_CHAT_LOG:READ+ANNOTATION:/WORKSPACE/default/APPLICATION/019845dc-1f97-7553-b930-458cbe08c5c4");
        permissions.add("TOOL:READ+EXPORT:/WORKSPACE/default/TOOL/01989be7-49fc-7da1-8df9-e2294a1a4756");
        permissions.add("APPLICATION_ACCESS:READ+EDIT:/WORKSPACE/default/APPLICATION/019845cc-dd3e-7761-a2dc-cb918b6ca924");
        permissions.add("APPLICATION_OVERVIEW:READ+ACCESS:/WORKSPACE/default/APPLICATION/019845dc-1f97-7553-b930-458cbe08c5c4");
        permissions.add("APPLICATION_CHAT_USER:READ+EDIT:/WORKSPACE/default/APPLICATION/01989d3c-8f86-73b3-9896-394d1369d12d");
        permissions.add("APPLICATION_CHAT_LOG:READ+CLEAR_POLICY:/WORKSPACE/default/APPLICATION/019845cc-dd3e-7761-a2dc-cb918b6ca924");
        permissions.add("TOOL:READ+DELETE:/WORKSPACE/default/TOOL/0198a16e-397a-7ce2-86ca-bad99547be86");
        permissions.add("APPLICATION:READ+EDIT:/WORKSPACE/default/APPLICATION/01989d39-faca-7512-b01b-68f62bb8b962");
        permissions.add("APPLICATION_CHAT_USER:READ+EDIT:/WORKSPACE/default/APPLICATION/019845cc-dd3e-7761-a2dc-cb918b6ca924");
        permissions.add("APPLICATION:READ+EXPORT:/WORKSPACE/default/APPLICATION/019845dc-1f97-7553-b930-458cbe08c5c4");
        permissions.add("APPLICATION:READ+EDIT:/WORKSPACE/default/APPLICATION/019845dc-1f97-7553-b930-458cbe08c5c4");
        permissions.add("APPLICATION:READ+IMPORT:/WORKSPACE/default/APPLICATION/01989d3c-8f86-73b3-9896-394d1369d12d");
        permissions.add("APPLICATION:READ+IMPORT:/WORKSPACE/default/APPLICATION/019845dc-1f97-7553-b930-458cbe08c5c4");
        permissions.add("APPLICATION_OVERVIEW:READ+PUBLIC_ACCESS:/WORKSPACE/default/APPLICATION/01989d3c-8f86-73b3-9896-394d1369d12d");
        permissions.add("APPLICATION:READ+EDIT:/WORKSPACE/default/APPLICATION/01989d3c-8f86-73b3-9896-394d1369d12d");
        permissions.add("APPLICATION_CHAT_LOG:READ+EXPORT:/WORKSPACE/default/APPLICATION/01989d3c-8f86-73b3-9896-394d1369d12d");
        permissions.add("APPLICATION:READ+DELETE:/WORKSPACE/default/APPLICATION/019845cc-dd3e-7761-a2dc-cb918b6ca924");
        permissions.add("APPLICATION:READ+DELETE:/WORKSPACE/default/APPLICATION/01989d39-faca-7512-b01b-68f62bb8b962");
        permissions.add("APPLICATION_CHAT_LOG:READ+ANNOTATION:/WORKSPACE/default/APPLICATION/019845cc-dd3e-7761-a2dc-cb918b6ca924");
        permissions.add("TOOL:READ+DEBUG:/WORKSPACE/default/TOOL/0198a16e-397a-7ce2-86ca-bad99547be86");
        permissions.add("APPLICATION_OVERVIEW:READ+DISPLAY:/WORKSPACE/default/APPLICATION/019845dc-1f97-7553-b930-458cbe08c5c4");
        permissions.add("APPLICATION:READ+DELETE:/WORKSPACE/default/APPLICATION/01989d3c-8f86-73b3-9896-394d1369d12d");
        permissions.add("APPLICATION_OVERVIEW:READ:/WORKSPACE/default/APPLICATION/01989d3c-8f86-73b3-9896-394d1369d12d");
        permissions.add("APPLICATION:READ+IMPORT:/WORKSPACE/default/APPLICATION/019845cc-dd3e-7761-a2dc-cb918b6ca924");
        permissions.add("APPLICATION_CHAT_LOG:READ+ADD_KNOWLEDGE:/WORKSPACE/default/APPLICATION/019845cc-dd3e-7761-a2dc-cb918b6ca924");
        permissions.add("APPLICATION_OVERVIEW:READ+DISPLAY:/WORKSPACE/default/APPLICATION/01989d3c-8f86-73b3-9896-394d1369d12d");
        permissions.add("APPLICATION_OVERVIEW:READ+EMBED:/WORKSPACE/default/APPLICATION/01989d3c-8f86-73b3-9896-394d1369d12d");
        permissions.add("APPLICATION_CHAT_LOG:READ+ADD_KNOWLEDGE:/WORKSPACE/default/APPLICATION/01989d3c-8f86-73b3-9896-394d1369d12d");
        permissions.add("APPLICATION_CHAT_LOG:READ:/WORKSPACE/default/APPLICATION/01989d39-faca-7512-b01b-68f62bb8b962");
        permissions.add("APPLICATION_CHAT_USER:READ:/WORKSPACE/default/APPLICATION/01989d39-faca-7512-b01b-68f62bb8b962");
        permissions.add("APPLICATION:READ+EXPORT:/WORKSPACE/default/APPLICATION/019845cc-dd3e-7761-a2dc-cb918b6ca924");
        permissions.add("APPLICATION_OVERVIEW:READ:/WORKSPACE/default/APPLICATION/01989d39-faca-7512-b01b-68f62bb8b962");
        permissions.add("APPLICATION_OVERVIEW:READ+ACCESS:/WORKSPACE/default/APPLICATION/019845cc-dd3e-7761-a2dc-cb918b6ca924");
        permissions.add("APPLICATION_CHAT_LOG:READ+EXPORT:/WORKSPACE/default/APPLICATION/01989d39-faca-7512-b01b-68f62bb8b962");
        permissions.add("APPLICATION_OVERVIEW:READ+API_KEY:/WORKSPACE/default/APPLICATION/01989d3c-8f86-73b3-9896-394d1369d12d");
        permissions.add("APPLICATION_CHAT_LOG:READ:/WORKSPACE/default/APPLICATION/01989d3c-8f86-73b3-9896-394d1369d12d");
        permissions.add("APPLICATION_CHAT_USER:READ+EDIT:/WORKSPACE/default/APPLICATION/01989d39-faca-7512-b01b-68f62bb8b962");
        permissions.add("APPLICATION:READ+IMPORT:/WORKSPACE/default/APPLICATION/01989d39-faca-7512-b01b-68f62bb8b962");
        permissions.add("APPLICATION_OVERVIEW:READ+EMBED:/WORKSPACE/default/APPLICATION/019845cc-dd3e-7761-a2dc-cb918b6ca924");
        permissions.add("APPLICATION_CHAT_USER:READ+EDIT:/WORKSPACE/default/APPLICATION/019845dc-1f97-7553-b930-458cbe08c5c4");
        permissions.add("APPLICATION_CHAT_LOG:READ+CLEAR_POLICY:/WORKSPACE/default/APPLICATION/01989d3c-8f86-73b3-9896-394d1369d12d");
        permissions.add("APPLICATION:READ:/WORKSPACE/default/APPLICATION/019845dc-1f97-7553-b930-458cbe08c5c4");
        permissions.add("TOOL:READ+IMPORT:/WORKSPACE/default/TOOL/01989be7-49fc-7da1-8df9-e2294a1a4756");
        permissions.add("APPLICATION:READ+DELETE:/WORKSPACE/default/APPLICATION/019845dc-1f97-7553-b930-458cbe08c5c4");
        permissions.add("APPLICATION_CHAT_LOG:READ+ADD_KNOWLEDGE:/WORKSPACE/default/APPLICATION/01989d39-faca-7512-b01b-68f62bb8b962");
        permissions.add("APPLICATION:READ:/WORKSPACE/default/APPLICATION/01989d39-faca-7512-b01b-68f62bb8b962");
        permissions.add("APPLICATION_ACCESS:READ:/WORKSPACE/default/APPLICATION/01989d39-faca-7512-b01b-68f62bb8b962");
        permissions.add("APPLICATION_OVERVIEW:READ+ACCESS:/WORKSPACE/default/APPLICATION/01989d3c-8f86-73b3-9896-394d1369d12d");
        permissions.add("APPLICATION_CHAT_LOG:READ+ADD_KNOWLEDGE:/WORKSPACE/default/APPLICATION/019845dc-1f97-7553-b930-458cbe08c5c4");
        permissions.add("APPLICATION_OVERVIEW:READ+EMBED:/WORKSPACE/default/APPLICATION/01989d39-faca-7512-b01b-68f62bb8b962");
        permissions.add("TOOL:READ+EDIT:/WORKSPACE/default/TOOL/0198a16e-397a-7ce2-86ca-bad99547be86");
        permissions.add("APPLICATION_OVERVIEW:READ+PUBLIC_ACCESS:/WORKSPACE/default/APPLICATION/019845cc-dd3e-7761-a2dc-cb918b6ca924");
        permissions.add("APPLICATION:READ+DEBUG:/WORKSPACE/default/APPLICATION/01989d3c-8f86-73b3-9896-394d1369d12d");
        permissions.add("APPLICATION:READ+EXPORT:/WORKSPACE/default/APPLICATION/01989d39-faca-7512-b01b-68f62bb8b962");
        permissions.add("APPLICATION:READ:/WORKSPACE/default/APPLICATION/019845cc-dd3e-7761-a2dc-cb918b6ca924");
        permissions.add("APPLICATION:READ+CREATE:/WORKSPACE/default/APPLICATION/019845dc-1f97-7553-b930-458cbe08c5c4");
        permissions.add("APPLICATION:READ+EXPORT:/WORKSPACE/default/APPLICATION/01989d3c-8f86-73b3-9896-394d1369d12d");
        permissions.add("APPLICATION_OVERVIEW:READ+DISPLAY:/WORKSPACE/default/APPLICATION/01989d39-faca-7512-b01b-68f62bb8b962");
        permissions.add("APPLICATION:READ+DEBUG:/WORKSPACE/default/APPLICATION/019845cc-dd3e-7761-a2dc-cb918b6ca924");
        permissions.add("APPLICATION_ACCESS:READ+EDIT:/WORKSPACE/default/APPLICATION/01989d39-faca-7512-b01b-68f62bb8b962");
        permissions.add("APPLICATION_OVERVIEW:READ+API_KEY:/WORKSPACE/default/APPLICATION/019845dc-1f97-7553-b930-458cbe08c5c4");
        permissions.add("APPLICATION:READ+DEBUG:/WORKSPACE/default/APPLICATION/019845dc-1f97-7553-b930-458cbe08c5c4");
        permissions.add("APPLICATION:READ+DEBUG:/WORKSPACE/default/APPLICATION/01989d39-faca-7512-b01b-68f62bb8b962");
        permissions.add("TOOL:READ+DEBUG:/WORKSPACE/default/TOOL/01989be7-49fc-7da1-8df9-e2294a1a4756");
        permissions.add("APPLICATION_OVERVIEW:READ:/WORKSPACE/default/APPLICATION/019845cc-dd3e-7761-a2dc-cb918b6ca924");
        permissions.add("APPLICATION_ACCESS:READ+EDIT:/WORKSPACE/default/APPLICATION/01989d3c-8f86-73b3-9896-394d1369d12d");
        permissions.add("APPLICATION_ACCESS:READ:/WORKSPACE/default/APPLICATION/01989d3c-8f86-73b3-9896-394d1369d12d");
        permissions.add("APPLICATION_OVERVIEW:READ+API_KEY:/WORKSPACE/default/APPLICATION/01989d39-faca-7512-b01b-68f62bb8b962");
        permissions.add("APPLICATION_CHAT_LOG:READ+ANNOTATION:/WORKSPACE/default/APPLICATION/01989d39-faca-7512-b01b-68f62bb8b962");
        permissions.add("TOOL:READ+CREATE:/WORKSPACE/default/TOOL/0198a16e-397a-7ce2-86ca-bad99547be86");
        permissions.add("APPLICATION_ACCESS:READ:/WORKSPACE/default/APPLICATION/019845dc-1f97-7553-b930-458cbe08c5c4");
        permissions.add("APPLICATION:READ+CREATE:/WORKSPACE/default/APPLICATION/01989d3c-8f86-73b3-9896-394d1369d12d");
        permissions.add("APPLICATION_CHAT_LOG:READ:/WORKSPACE/default/APPLICATION/019845cc-dd3e-7761-a2dc-cb918b6ca924");
        permissions.add("APPLICATION_CHAT_USER:READ:/WORKSPACE/default/APPLICATION/01989d3c-8f86-73b3-9896-394d1369d12d");
        permissions.add("APPLICATION_OVERVIEW:READ+PUBLIC_ACCESS:/WORKSPACE/default/APPLICATION/019845dc-1f97-7553-b930-458cbe08c5c4");
        permissions.add("APPLICATION_CHAT_USER:READ:/WORKSPACE/default/APPLICATION/019845dc-1f97-7553-b930-458cbe08c5c4");
        permissions.add("APPLICATION_OVERVIEW:READ+PUBLIC_ACCESS:/WORKSPACE/default/APPLICATION/01989d39-faca-7512-b01b-68f62bb8b962");
        permissions.add("APPLICATION_ACCESS:READ+EDIT:/WORKSPACE/default/APPLICATION/019845dc-1f97-7553-b930-458cbe08c5c4");
        permissions.add("APPLICATION:READ:/WORKSPACE/default/APPLICATION/01989d3c-8f86-73b3-9896-394d1369d12d");
        permissions.add("APPLICATION_CHAT_LOG:READ+CLEAR_POLICY:/WORKSPACE/default/APPLICATION/01989d39-faca-7512-b01b-68f62bb8b962");
        permissions.add("APPLICATION:READ+CREATE:/WORKSPACE/default/APPLICATION/01989d39-faca-7512-b01b-68f62bb8b962");
        permissions.add("APPLICATION_CHAT_LOG:READ+CLEAR_POLICY:/WORKSPACE/default/APPLICATION/019845dc-1f97-7553-b930-458cbe08c5c4");
        permissions.add("APPLICATION_CHAT_LOG:READ+ANNOTATION:/WORKSPACE/default/APPLICATION/01989d3c-8f86-73b3-9896-394d1369d12d");
        permissions.add("APPLICATION_CHAT_USER:READ:/WORKSPACE/default/APPLICATION/019845cc-dd3e-7761-a2dc-cb918b6ca924");
        permissions.add("APPLICATION_ACCESS:READ:/WORKSPACE/default/APPLICATION/019845cc-dd3e-7761-a2dc-cb918b6ca924");
        permissions.add("TOOL:READ+EXPORT:/WORKSPACE/default/TOOL/0198a16e-397a-7ce2-86ca-bad99547be86");
        permissions.add("APPLICATION_OVERVIEW:READ:/WORKSPACE/default/APPLICATION/019845dc-1f97-7553-b930-458cbe08c5c4");
        permissions.add("TOOL:READ:/WORKSPACE/default/TOOL/0198a16e-397a-7ce2-86ca-bad99547be86");
        permissions.add("APPLICATION_OVERVIEW:READ+ACCESS:/WORKSPACE/default/APPLICATION/01989d39-faca-7512-b01b-68f62bb8b962");
        permissions.add("APPLICATION_CHAT_LOG:READ:/WORKSPACE/default/APPLICATION/019845dc-1f97-7553-b930-458cbe08c5c4");
        permissions.add("TOOL:READ+DELETE:/WORKSPACE/default/TOOL/01989be7-49fc-7da1-8df9-e2294a1a4756");
        permissions.add("APPLICATION:READ+EDIT:/WORKSPACE/default/APPLICATION/019845cc-dd3e-7761-a2dc-cb918b6ca924");
        permissions.add("APPLICATION_CHAT_LOG:READ+EXPORT:/WORKSPACE/default/APPLICATION/019845dc-1f97-7553-b930-458cbe08c5c4");
        permissions.add("APPLICATION:READ+CREATE:/WORKSPACE/default/APPLICATION/019845cc-dd3e-7761-a2dc-cb918b6ca924");
        permissions.add("TOOL:READ+CREATE:/WORKSPACE/default/TOOL/01989be7-49fc-7da1-8df9-e2294a1a4756");
        permissions.add("APPLICATION_OVERVIEW:READ+DISPLAY:/WORKSPACE/default/APPLICATION/019845cc-dd3e-7761-a2dc-cb918b6ca924");
        permissions.add("TOOL:READ+EDIT:/WORKSPACE/default/TOOL/01989be7-49fc-7da1-8df9-e2294a1a4756");
        permissions.add("APPLICATION_CHAT_LOG:READ+EXPORT:/WORKSPACE/default/APPLICATION/019845cc-dd3e-7761-a2dc-cb918b6ca924");
        permissions.add("APPLICATION_OVERVIEW:READ+API_KEY:/WORKSPACE/default/APPLICATION/019845cc-dd3e-7761-a2dc-cb918b6ca924");
        permissions.add("TOOL:READ:/WORKSPACE/default/TOOL/01989be7-49fc-7da1-8df9-e2294a1a4756");
        permissions.add("TOOL:READ+IMPORT:/WORKSPACE/default/TOOL/0198a16e-397a-7ce2-86ca-bad99547be86");
        user.setPermissions(permissions);
        // user.setPermissions(stpInterface.getPermissionList(userId, null));
        user.setRole(Set.of("ADMIN", "WORKSPACE_MANAGE:/WORKSPACE/default"));
        List<Map<String, String>> workspaceList = new ArrayList<>();
        workspaceList.add(Map.of("id", "default", "name", "default"));
        user.setWorkspaceList(workspaceList);
        //todo 临时处理
        user.setIsEditPassword("d880e722c47a34d8e9fce789fc62389d".equals(user.getPassword()) && "ADMIN".equals(user.getRole()));
        return user;
    }

    public Boolean sendEmailCode() throws MessagingException {
        return sendEmailCode((String) StpUtil.getExtra("email"), "【智能知识库问答系统-修改密码】");
    }

    public Boolean sendEmailCode(String email, String subject) throws MessagingException {
        Context context = new Context();
        String code = generateCode();
        context.setVariable("code", code);
        AUTH_CODE_CACHE.put(email, code);
        emailService.sendMessage(email, subject, "email_template", context);
        return true;
    }

    private String generateCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }

    public Boolean resetPassword(PasswordDTO dto) {
        if (dto.getPassword().equals(dto.getRePassword())) {
            UserEntity userEntity = new UserEntity();
            userEntity.setId(StpUtil.getLoginIdAsString());
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
        String userId = StpUtil.getLoginIdAsString();
        user.setId(userId);
        StpLogic stpLogic = new StpLogicJwtForStateless();
        String token = stpLogic.createTokenValue(userId, "default-device", StpUtil.getTokenTimeout(), Map.of("language", user.getLanguage()));
        StpUtil.setTokenValue(token);
        return updateById(user);
    }

    public UserEntity validUserById(String id) {
        //todo
        return this.getById(id);
    }

    public Map<String, String> getNicknameMap() {
        return this.lambdaQuery().select(UserEntity::getId, UserEntity::getNickname).list().stream().collect(Collectors.toMap(UserEntity::getId, UserEntity::getNickname));
    }
}
