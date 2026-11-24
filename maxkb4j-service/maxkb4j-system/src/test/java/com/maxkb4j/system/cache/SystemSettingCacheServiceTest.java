package com.maxkb4j.system.cache;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.cache.SystemKeySetting;
import com.maxkb4j.common.cache.SystemSettingStore;
import com.maxkb4j.common.enums.SettingType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 系统设置缓存 Bean 行为测试。
 */
class SystemSettingCacheServiceTest {

    private SystemSettingStore store;

    @BeforeEach
    void setUp() {
        store = new SystemSettingCacheService();
    }

    @Test
    void putGet_roundTrip() {
        JSONObject meta = new JSONObject();
        meta.put("k", "v");

        store.put(SettingType.Email.getType(), meta);

        assertThat(store.get(SettingType.Email.getType())).isSameAs(meta);
    }

    @Test
    void keySetting_helpersReadThroughStore() {
        JSONObject meta = new JSONObject();
        meta.put(SystemKeySetting.FIELD_PUBLIC_KEY, "pub-pem");
        meta.put(SystemKeySetting.FIELD_ENCRYPTED_PRIVATE_KEY, "enc-priv-pem");
        store.put(SettingType.KEY.getType(), meta);

        assertThat(store.getPublicKey()).isEqualTo("pub-pem");
        assertThat(store.getPrivateKey()).isEqualTo("enc-priv-pem");
        assertThat(store.getKeySetting()).isNotNull();
    }

    @Test
    void keySetting_absent_returnsNull() {
        assertThat(store.getPublicKey()).isNull();
        assertThat(store.getPrivateKey()).isNull();
        assertThat(store.getKeySetting()).isNull();
    }
}
