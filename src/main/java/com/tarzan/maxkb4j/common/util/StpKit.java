package com.tarzan.maxkb4j.common.util;

import cn.dev33.satoken.jwt.StpLogicJwtForStateless;
import cn.dev33.satoken.stp.StpLogic;
import com.tarzan.maxkb4j.module.application.enums.AuthType;

public class StpKit {

    /**
     * User 会话对象，管理 User 表所有账号的登录、权限认证
     */
    public static final StpLogic USER = new StpLogicJwtForStateless(AuthType.USER.name());

    public static final StpLogic API_KEY = new StpLogicJwtForStateless("API_KEY");

    public static final StpLogic PLATFORM = new StpLogicJwtForStateless("PLATFORM");

    public static final StpLogic ACCESS_TOKEN = new StpLogicJwtForStateless("APPLICATION_ACCESS_TOKEN");
}
