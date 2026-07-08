package com.maxkb4j.chat.service;

import com.maxkb4j.common.util.StpKit;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Chat 令牌签发适配器。
 * <p>将 sa-token 的 token 签发逻辑收敛至此,使业务层(如 {@link ChatApiService})不直接依赖 sa-token,
 * 保持"sa-token 仅出现在安全边界"的分层约定。
 *
 * @author tarzan
 */
@Component
public class ChatTokenService {

    /**
     * 为聊天用户签发 token(JWT),复用当前会话的设备标识。
     * <p>设备标识来自身份解析拦截器已桥接的 USER 会话;未登录(首次匿名)时为 {@code null}。
     *
     * @param chatUserId 聊天用户ID
     * @param extraData  写入 token 的扩展声明(applicationId / chatUserType / accessToken 等)
     * @return 签发的 token 字符串
     */
    public String issueAnonymousToken(String chatUserId, Map<String, Object> extraData) {
        return StpKit.USER.createTokenValue(chatUserId, StpKit.USER.getLoginDevice(), -1L, extraData);
    }
}
