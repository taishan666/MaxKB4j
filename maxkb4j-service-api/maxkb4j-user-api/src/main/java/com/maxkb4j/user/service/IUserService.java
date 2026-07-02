package com.maxkb4j.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.user.dto.PasswordDTO;
import com.maxkb4j.user.dto.UserDTO;
import com.maxkb4j.user.dto.UserLoginDTO;
import com.maxkb4j.user.entity.UserEntity;
import com.maxkb4j.user.vo.UserVO;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;
import java.util.Set;

public interface IUserService extends IService<UserEntity> {
    Set<String> getRoleById(String id);

    Map<String, String> getNicknameMap();

    String getUsername(String userId);

    String getNickname(String userId);

    String getEmail(String userId);

    String getLanguage(String userId);

    UserVO getUserById(String userId);

    String login(UserLoginDTO dto, HttpServletRequest request);

    Boolean sendEmailCode(String email, String subject) throws MessagingException;

    boolean checkCode(String email, String code);

    void createAdminUser(String defaultUsername, String defaultPassword);

    boolean updateLanguage(UserEntity user);

    IPage<UserEntity> selectUserPage(int page, int size, UserDTO dto);

    boolean createUser(UserEntity user);

    boolean deleteUserById(String id);

    boolean updatePassword(String id, PasswordDTO dto);

    boolean resetPassword(PasswordDTO dto);
}
