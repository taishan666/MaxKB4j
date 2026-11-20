package com.maxkb4j.common.util;

import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.jwt.StpLogicJwtForStateless;
import cn.dev33.satoken.stp.StpLogic;
import com.maxkb4j.common.constant.LoginType;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class StpKitTest {

    @Test
    void adminTokenRemainsValidAfterLogicIsRecreated() {
        assertThat(StpKit.ADMIN).isInstanceOf(StpLogicJwtForStateless.class);

        SaTokenConfig originalConfig = StpKit.ADMIN.getConfig();
        SaTokenConfig config = new SaTokenConfig();
        config.setJwtSecretKey("test-secret-key-for-admin-token-restart");

        try {
            StpKit.ADMIN.setConfig(config);
            String token = StpKit.ADMIN.createTokenValue(
                    "admin-1", "PC", 86400, Collections.emptyMap());

            StpLogic restartedAdmin = new StpLogicJwtForStateless(LoginType.ADMIN);
            restartedAdmin.setConfig(config);

            assertThat(restartedAdmin.getLoginIdByToken(token)).isEqualTo("admin-1");
        } finally {
            StpKit.ADMIN.setConfig(originalConfig);
        }
    }
}
