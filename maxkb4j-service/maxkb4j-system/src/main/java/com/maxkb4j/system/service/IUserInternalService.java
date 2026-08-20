package com.maxkb4j.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.system.dto.UserLoginDTO;
import com.maxkb4j.system.entity.UserEntity;
import com.maxkb4j.system.vo.UserNameVO;
import com.maxkb4j.system.vo.UserProfileVO;
import com.maxkb4j.system.vo.UserVO;
import com.maxkb4j.user.dto.PasswordDTO;
import com.maxkb4j.user.dto.UserQuery;
import com.maxkb4j.user.service.IUserService;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 用户服务「对内」接口：供 Controller / 本模块内部使用的完整服务契约。
 *
 * <p>与 {@link IUserService}（对外跨模块契约，位于 maxkb4j-user-api）区分。
 * 本接口位于 service 模块，可引用 {@link UserEntity} 等 service 模块类型，
 * 并继承 {@link IService} 获得 MyBatis-Plus 通用方法（getById/updateById 等），
 * 使 Controller 依赖抽象而非具体实现 {@code UserServiceImpl}。</p>
 */
public interface IUserInternalService extends IUserService, IService<UserEntity> {

    IPage<UserVO> selectUserPage(int page, int size, UserQuery dto);

    boolean deleteUserById(String userId);

    String login(UserLoginDTO dto, HttpServletRequest request);

    boolean createUser(UserEntity user);

    UserProfileVO getUserProfileById(String userId);

    Boolean sendEmailCode(String email, String subject);

    boolean checkCode(String email, String code);

    boolean resetPassword(PasswordDTO dto);

    boolean batchDelete(List<String> ids);

    boolean updatePassword(String id, PasswordDTO dto);

    boolean updateLanguage(UserEntity user);

    List<UserNameVO> listActiveUserNames();

    List<UserEntity> listActiveMembers();

    String getEmail(String userId);
}