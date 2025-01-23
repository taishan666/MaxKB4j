package com.tarzan.maxkb4j.listener;

import com.tarzan.maxkb4j.module.system.setting.cache.SystemCache;
import com.tarzan.maxkb4j.module.system.setting.entity.SystemSettingEntity;
import com.tarzan.maxkb4j.module.system.setting.service.SystemSettingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 *
 * @author tarzan
 * @date 2021-10-05
 */
@Slf4j
@Component
public class StartedListener implements ApplicationListener<ApplicationStartedEvent> {

    @Autowired
    private SystemSettingService systemSettingService;

    @Override
    public void onApplicationEvent(@NonNull ApplicationStartedEvent event) {
        List<SystemSettingEntity> systemSettings=systemSettingService.list();
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
        log.info("\n----------------------------------------------------------\n\t" +
                "Application is running! Access URLs:\n\t" +
                "Local: \t\thttp://localhost:" + port + contextPath + "\n\t" +
                "External: \thttp://" + ip + ':' + port + contextPath + '\n' +
                "----------------------------------------------------------");
    }


}
