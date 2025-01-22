package com.tarzan.maxkb4j.job;

import com.tarzan.maxkb4j.module.application.entity.ApplicationPublicAccessClientEntity;
import com.tarzan.maxkb4j.module.application.service.ApplicationPublicAccessClientService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author tarzan
 */
@Slf4j
@Component
@AllArgsConstructor
public class ClientIntraDayAccessNumResetJob {

    private final ApplicationPublicAccessClientService accessClientService;

    @Scheduled(cron = "0 0 0 * * *")
    public void execute() {
        log.info("开始重置intraDayAccessNum");
        accessClientService.lambdaUpdate().set(ApplicationPublicAccessClientEntity::getIntraDayAccessNum,0);
        log.info("结束重置intraDayAccessNum");
    }
}
