package com.tarzan.maxkb4j.listener;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.system.setting.cache.SystemCache;
import com.tarzan.maxkb4j.module.system.setting.domain.entity.SystemSettingEntity;
import com.tarzan.maxkb4j.module.system.setting.enums.SettingType;
import com.tarzan.maxkb4j.module.system.setting.service.SystemSettingService;
import com.tarzan.maxkb4j.module.system.user.service.UserService;
import com.tarzan.maxkb4j.common.util.RSAUtil;
import io.jsonwebtoken.lang.Collections;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import java.util.Map;

/**
 *
 * @author tarzan
 * @date 2021-10-05
 */
@Slf4j
@Component
@AllArgsConstructor
public class StartedListener implements ApplicationListener<ApplicationStartedEvent> {

    private final SystemSettingService systemSettingService;
    private final UserService userService;

    @Override
    public void onApplicationEvent(@NonNull ApplicationStartedEvent event) {
        long userCount=userService.count();
        if (userCount==0){
            userService.createAdminUser("admin", "maxkb4j.");
        }
        List<SystemSettingEntity> systemSettings=systemSettingService.list();
        if(Collections.isEmpty(systemSettings)){
            try {
                KeyPair keyPair=RSAUtil.generateRSAKeyPair();
                PublicKey publicKey = keyPair.getPublic();
                String publicKeyPem = RSAUtil.publicKeyPem(publicKey);
                PrivateKey privateKey = keyPair.getPrivate();
                String encryptPrivateKeyPem = RSAUtil.encryptPrivateKeyPem(privateKey);
                JSONObject meta=new JSONObject(Map.of("key",publicKeyPem,"value",encryptPrivateKeyPem));
                systemSettingService.saveOrUpdate(meta, SettingType.KEY.getType());
                SystemCache.put(SettingType.KEY.getType(),meta);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        for (SystemSettingEntity systemSetting : systemSettings) {
            SystemCache.put(systemSetting.getType(),systemSetting.getMeta());
        }
        printStartInfo(event);
    }

    /**
     * 打印信息
     */
    private void printStartInfo(ApplicationStartedEvent event) {
        ConfigurableApplicationContext context=event.getApplicationContext();
        Environment env = context.getEnvironment();
        String ip = null;
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.error(e.getMessage());
        }
        String port = env.getProperty("server.port");
        String contextPath = env.getProperty("server.servlet.context-path");
        if (contextPath == null) {
            contextPath = "";
        }
        log.info("\n----------------------------------------------------------\n\tApplication is running! Access URLs:\n\tLocal: \t\thttp://localhost:{}{}\n\tExternal: \thttp://{}:{}{}\n----------------------------------------------------------", port, contextPath, ip, port, contextPath);
    }


}
