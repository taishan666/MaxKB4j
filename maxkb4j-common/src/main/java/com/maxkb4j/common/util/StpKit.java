package com.maxkb4j.common.util;

import cn.dev33.satoken.jwt.StpLogicJwtForStateless;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import com.maxkb4j.common.constant.LoginType;

import java.util.HashMap;
import java.util.Map;

public class StpKit {

    /**
     * 签发时间(JWT 载荷 claim,秒级时间戳)。
     * <p>sa-token-jwt 的 eff 为失效时间(毫秒)而非签发时间,无法用于令牌撤销比对,
     * 故在 {@link StpLogicJwtForStateless#createTokenValue} 中自动写入本 claim,
     * 供 {@code UserTokenRevokeCache} 按「签发时间早于撤销时间点即失效」规则校验。
     */
    public static final String ISSUED_AT_CLAIM = "iat";

    /**
     * 默认原生会话对象
     */
    public static final StpLogic DEFAULT = StpUtil.stpLogic;
    /**
     * Admin 后台管理员
     */
    public static final StpLogic ADMIN = new StpLogicJwtForStateless(LoginType.ADMIN) {
        // 重写 StpLogic 类下的 `splicingKeyTokenName` 函数，返回一个与 `StpUtil` 不同的token名称, 防止冲突
        @Override
        public String splicingKeyTokenName() {
            return super.splicingKeyTokenName() + "-" + LoginType.ADMIN;
        }

        // 签发令牌时自动写入 iat 签发时间,供撤销缓存比对
        @Override
        public String createTokenValue(Object loginId, String device, long timeout, Map<String, Object> extraData) {
            return super.createTokenValue(loginId, device, timeout, withIssuedAt(extraData));
        }
    };
    /**
     * User 前台普通用户
     */
    public static final StpLogic USER = new StpLogicJwtForStateless(LoginType.USER) {
        // 重写 StpLogic 类下的 `splicingKeyTokenName` 函数，返回一个与 `StpUtil` 不同的token名称, 防止冲突
        @Override
        public String splicingKeyTokenName() {
            return super.splicingKeyTokenName() + "-" + LoginType.USER;
        }

        // 签发令牌时自动写入 iat 签发时间,供撤销缓存比对
        @Override
        public String createTokenValue(Object loginId, String device, long timeout, Map<String, Object> extraData) {
            return super.createTokenValue(loginId, device, timeout, withIssuedAt(extraData));
        }
    };

    /**
     * 在扩展载荷中补写 iat 签发时间(不改动调用方传入的原始 Map)。
     */
    private static Map<String, Object> withIssuedAt(Map<String, Object> extraData) {
        Map<String, Object> extra = extraData == null ? new HashMap<>() : new HashMap<>(extraData);
        extra.put(ISSUED_AT_CLAIM, System.currentTimeMillis() / 1000);
        return extra;
    }

}
