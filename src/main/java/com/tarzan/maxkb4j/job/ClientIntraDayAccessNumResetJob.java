package com.tarzan.maxkb4j.job;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatUserStatsEntity;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationChatUserStatsMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author tarzan
 */
@Slf4j
@Component
public class ClientIntraDayAccessNumResetJob {

    @Resource
    private ApplicationChatUserStatsMapper accessClientMapper;

    @Scheduled(cron = "0 0 0 * * *")
    public void execute() {
        log.info("开始重置intraDayAccessNum");
        accessClientMapper.update(Wrappers.<ApplicationChatUserStatsEntity>lambdaUpdate().set(ApplicationChatUserStatsEntity::getIntraDayAccessNum,0));
        log.info("结束重置intraDayAccessNum");
    }
}
