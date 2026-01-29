package com.tarzan.maxkb4j.module.application.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.application.domain.dto.PlatformStatusDTO;
import com.tarzan.maxkb4j.module.application.domain.entity.ApplicationAccessEntity;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationAccessMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ApplicationAccessService extends ServiceImpl<ApplicationAccessMapper, ApplicationAccessEntity> {

    // 平台类型常量
    public static final Set<String> SUPPORTED_PLATFORMS = Set.of("wecom", "dingtalk", "wechat", "lark", "slack");

    // 构建默认平台状态 JSON
    private JSONObject buildDefaultStatus() {
        JSONObject status = new JSONObject();
        for (String platform : SUPPORTED_PLATFORMS) {
            status.put(platform, false);
        }
        return status;
    }

    // 构建默认平台配置 JSON
    private JSONObject buildDefaultConfig() {
        JSONObject config = new JSONObject();
        config.put("wechat", new JSONObject()
                .fluentPut("app_id", "")
                .fluentPut("app_secret", "")
                .fluentPut("token", "")
                .fluentPut("encoding_aes_key", "")
                .fluentPut("is_certification", false)
                .fluentPut("callback_url", "")
        );
        config.put("dingtalk", new JSONObject()
                .fluentPut("client_id", "")
                .fluentPut("client_secret", "")
                .fluentPut("callback_url", "")
        );
        config.put("wecom", new JSONObject()
                .fluentPut("app_id", "")
                .fluentPut("agent_id", "")
                .fluentPut("secret", "")
                .fluentPut("token", "")
                .fluentPut("encoding_aes_key", "")
                .fluentPut("callback_url", "")
        );
        config.put("lark", new JSONObject()
                .fluentPut("app_id", "")
                .fluentPut("app_secret", "")
                .fluentPut("verification_token", "")
                .fluentPut("callback_url", "")
        );
        config.put("slack", new JSONObject()
                .fluentPut("signing_secret", "")
                .fluentPut("bot_user_token", "")
                .fluentPut("callback_url", "")
        );
        return config;
    }

    // ==================== 状态相关 ====================

    public JSONObject getPlatformStatus(String id) {
        ApplicationAccessEntity entity = this.getById(id);
        if (entity != null && entity.getStatus() != null) {
            return entity.getStatus();
        }
        return buildDefaultStatus();
    }

    public boolean updatePlatformStatus(String id, PlatformStatusDTO params) {
        if (!SUPPORTED_PLATFORMS.contains(params.getType())) {
            throw new IllegalArgumentException("Unsupported platform: " + params.getType());
        }
        ApplicationAccessEntity entity = this.getById(id);
        if (entity == null) {
            entity = new ApplicationAccessEntity();
            entity.setId(id); // 如果 id 是主键且非自增，需显式设置
        }
        JSONObject status = Optional.ofNullable(entity.getStatus())
                .orElseGet(this::buildDefaultStatus);
        // 更新指定平台的状态：[true, params.getStatus()]
        status.put(params.getType(), Boolean.TRUE.equals(params.getStatus()));
        entity.setStatus(status);
        return this.saveOrUpdate(entity);
    }

    // ==================== 配置相关 ====================

    public JSONObject getPlatformConfig(String id, String key) {
        if (!SUPPORTED_PLATFORMS.contains(key)) {
            throw new IllegalArgumentException("Unsupported platform config key: " + key);
        }
        ApplicationAccessEntity entity = this.getById(id);
        JSONObject config = Optional.ofNullable(entity)
                .map(ApplicationAccessEntity::getConfig)
                .orElseGet(this::buildDefaultConfig);

        return config.getJSONObject(key);
    }

    public boolean updatePlatformConfig(String id, String key, JSONObject platformConfig) {
        if (!SUPPORTED_PLATFORMS.contains(key)) {
            throw new IllegalArgumentException("Unsupported platform config key: " + key);
        }
        ApplicationAccessEntity entity = this.getById(id);
        if (entity == null) {
            entity = new ApplicationAccessEntity();
            entity.setId(id); // 同上，根据实际主键策略决定
        }
        JSONObject config = Optional.ofNullable(entity.getConfig())
                .orElseGet(this::buildDefaultConfig);
        config.put(key, platformConfig);
        entity.setConfig(config);
        return this.saveOrUpdate(entity);
    }

    public boolean platformCallback(String id, String key, JSONObject params) {
        ApplicationAccessEntity entity = this.getById(id);
        if (entity != null) {
            if (entity.getStatus() != null) {
                return entity.getStatus().getBooleanValue(key);
            }
            if (entity.getConfig() != null) {
                JSONObject config =entity.getConfig().getJSONObject(key);
                //todo
            }
        }
        return false;
    }
}