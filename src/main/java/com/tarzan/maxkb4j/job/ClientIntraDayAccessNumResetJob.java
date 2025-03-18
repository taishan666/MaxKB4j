package com.tarzan.maxkb4j.job;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tarzan.maxkb4j.module.application.entity.ApplicationPublicAccessClientEntity;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationPublicAccessClientMapper;
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
    private  ApplicationPublicAccessClientMapper accessClientMapper;

    @Scheduled(cron = "0 0 0 * * *")
    public void execute() {
        log.info("开始重置intraDayAccessNum");
        accessClientMapper.update(Wrappers.<ApplicationPublicAccessClientEntity>lambdaUpdate().set(ApplicationPublicAccessClientEntity::getIntraDayAccessNum,0));
        log.info("结束重置intraDayAccessNum");
    }
}
