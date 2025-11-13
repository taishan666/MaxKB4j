package com.tarzan.maxkb4j.common.util;

import cn.dev33.satoken.jwt.StpLogicJwtForStateless;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;

public class StpKit {

    /**
     * 默认原生会话对象
     */
    public static final StpLogic DEFAULT = StpUtil.stpLogic;
    /**
     * Admin 后台管理员
     */
    public static final StpLogic ADMIN = new StpLogicJwtForStateless("admin"){
        // 重写 StpLogic 类下的 `splicingKeyTokenName` 函数，返回一个与 `StpUtil` 不同的token名称, 防止冲突
        @Override
        public String splicingKeyTokenName() {
            return super.splicingKeyTokenName() + "-admin";
        }
    };
    /**
     * User 前台普通用户
     */
    public static final StpLogic USER = new StpLogicJwtForStateless("user"){
        // 重写 StpLogic 类下的 `splicingKeyTokenName` 函数，返回一个与 `StpUtil` 不同的token名称, 防止冲突
        @Override
        public String splicingKeyTokenName() {
            return super.splicingKeyTokenName() + "-user";
        }
    };


}
