package com.maxkb4j.system.cache;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.cache.SystemCache;
import com.maxkb4j.common.cache.SystemKeySetting;
import com.maxkb4j.common.enums.SettingType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 系统设置缓存 Bean + 静态门面行为测试（第 3 期组件化）。
 */
class SystemSettingCacheServiceTest {

    @AfterEach
    void tearDown() {
        SystemCache.reset();
    }

    @Test
    void construct_registersBeanIntoFacade() {
        SystemSettingCacheService service = new SystemSettingCacheService();
        JSONObject meta = new JSONObject();
        meta.put("k", "v");

        SystemCache.put(SettingType.Email.getType(), meta);

        assertThat(service.get(SettingType.Email.getType())).isSameAs(meta);
        assertThat(SystemCache.get(SettingType.Email.getType())).isSameAs(meta);
    }

    @Test
    void keySetting_helpersReadThroughFacade() {
        new SystemSettingCacheService();
        JSONObject meta = new JSONObject();
        meta.put(SystemKeySetting.FIELD_PUBLIC_KEY, "pub-pem");
        meta.put(SystemKeySetting.FIELD_ENCRYPTED_PRIVATE_KEY, "enc-priv-pem");
        SystemCache.put(SettingType.KEY.getType(), meta);

        assertThat(SystemCache.getPublicKey()).isEqualTo("pub-pem");
        assertThat(SystemCache.getPrivateKey()).isEqualTo("enc-priv-pem");
        assertThat(SystemCache.getKeySetting()).isNotNull();
    }

    @Test
    void facadeDefaultStore_worksWithoutBeanRegistration() {
        SystemCache.reset();
        JSONObject meta = new JSONObject();
        SystemCache.put(SettingType.DISPLAY.getType(), meta);
        assertThat(SystemCache.get(SettingType.DISPLAY.getType())).isSameAs(meta);
    }
}