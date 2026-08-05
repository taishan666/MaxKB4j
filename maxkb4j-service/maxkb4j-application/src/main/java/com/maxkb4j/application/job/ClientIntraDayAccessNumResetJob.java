package com.maxkb4j.application.job;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.maxkb4j.application.entity.ApplicationChatUserStatsEntity;
import com.maxkb4j.application.service.ApplicationChatUserStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author tarzan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClientIntraDayAccessNumResetJob {
    private final ApplicationChatUserStatsService chatUserStatsService;

    @Scheduled(cron = "0 0 0 * * *")
    public void execute() {
        log.info("开始重置intraDayAccessNum");
        chatUserStatsService.update(Wrappers.<ApplicationChatUserStatsEntity>lambdaUpdate().set(ApplicationChatUserStatsEntity::getIntraDayAccessNum,0));
        log.info("结束重置intraDayAccessNum");
    }
}
