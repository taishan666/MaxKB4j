package com.maxkb4j.application.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.maxkb4j.application.entity.ApplicationChatEntity;
import com.maxkb4j.application.entity.ApplicationChatRecordEntity;
import com.maxkb4j.application.entity.ApplicationEntity;
import com.maxkb4j.application.service.IApplicationChatInternalService;
import com.maxkb4j.application.service.IApplicationChatRecordInternalService;
import com.maxkb4j.application.service.IApplicationInternalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * @author tarzan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationChatRecordCleanJob {
    private final IApplicationInternalService applicationService;
    private final IApplicationChatInternalService chatService;
    private final IApplicationChatRecordInternalService chatRecordService;


    @Scheduled(cron = "0 0 22 * * *")
    public void execute() {
        log.info("开始清理应用聊天记录");
        LambdaQueryWrapper<ApplicationEntity> appWrapper= Wrappers.lambdaQuery();
        appWrapper.select(ApplicationEntity::getId,ApplicationEntity::getCleanTime);
        List<ApplicationEntity> applications = applicationService.list(appWrapper);
        for (ApplicationEntity application : applications) {
            long cleanTime = application.getCleanTime();
            Date cleanTimeAgo = Date.from(Instant.now().minusSeconds(cleanTime * 24 * 60 * 60));
            LambdaQueryWrapper<ApplicationChatEntity> chatWrapper=Wrappers.lambdaQuery();
            chatWrapper.eq(ApplicationChatEntity::getId,application.getId());
            chatWrapper.lt(ApplicationChatEntity::getCreateTime,cleanTimeAgo);
            chatService.remove(chatWrapper);
            LambdaQueryWrapper<ApplicationChatRecordEntity> chatRecordWrapper=Wrappers.lambdaQuery();
            chatRecordWrapper.eq(ApplicationChatRecordEntity::getId,application.getId());
            chatRecordWrapper.lt(ApplicationChatRecordEntity::getCreateTime,cleanTimeAgo);
            chatRecordService.remove(chatRecordWrapper);
        }
        log.info("结束清理应用聊天记录");
    }
}
