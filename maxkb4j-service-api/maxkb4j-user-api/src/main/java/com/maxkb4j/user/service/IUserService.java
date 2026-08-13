package com.maxkb4j.user.service;

import com.maxkb4j.user.dto.UserDTO;

import java.util.Map;
import java.util.Set;

public interface IUserService {
    Set<String> getRoleById(String id);

    Map<String, String> getNicknameMap();

    String getUsername(String userId);

    String getNickname(String userId);

    String getLanguage(String userId);

    UserDTO getByUsernameOrEmail(String username, String email);

    void saveDTO(UserDTO user);
}
