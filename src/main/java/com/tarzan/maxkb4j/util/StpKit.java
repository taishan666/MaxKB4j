package com.tarzan.maxkb4j.util;

import cn.dev33.satoken.stp.StpLogic;

public class StpKit {

    /**
     * User 会话对象，管理 User 表所有账号的登录、权限认证
     */
    public static final StpLogic USER = new StpLogic("USER");

    public static final StpLogic API_KEY = new StpLogic("API_KEY");

    public static final StpLogic PLATFORM = new StpLogic("PLATFORM");

    public static final StpLogic ACCESS_TOKEN = new StpLogic("APPLICATION_ACCESS_TOKEN");
}
