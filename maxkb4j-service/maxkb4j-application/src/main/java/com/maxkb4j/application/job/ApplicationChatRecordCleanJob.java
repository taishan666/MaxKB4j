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
            // 单个应用清理失败不应中断其余应用的清理
            try {
                Integer cleanTimeDays = application.getCleanTime();
                // cleanTime 为空或 <=0 表示不自动清理
                if (cleanTimeDays == null || cleanTimeDays <= 0) {
                    continue;
                }
                Date cleanTimeAgo = Date.from(Instant.now().minusSeconds(cleanTimeDays * 24L * 60 * 60));
                // 先查出该应用下过期的会话，再级联清理其聊天记录
                LambdaQueryWrapper<ApplicationChatEntity> chatWrapper = Wrappers.lambdaQuery();
                chatWrapper.select(ApplicationChatEntity::getId);
                chatWrapper.eq(ApplicationChatEntity::getApplicationId, application.getId());
                chatWrapper.lt(ApplicationChatEntity::getCreateTime, cleanTimeAgo);
                List<String> expiredChatIds = chatService.list(chatWrapper)
                        .stream().map(ApplicationChatEntity::getId).toList();
                if (expiredChatIds.isEmpty()) {
                    continue;
                }
                LambdaQueryWrapper<ApplicationChatRecordEntity> chatRecordWrapper = Wrappers.lambdaQuery();
                chatRecordWrapper.in(ApplicationChatRecordEntity::getChatId, expiredChatIds);
                chatRecordService.remove(chatRecordWrapper);
                LambdaQueryWrapper<ApplicationChatEntity> removeChatWrapper = Wrappers.lambdaQuery();
                removeChatWrapper.in(ApplicationChatEntity::getId, expiredChatIds);
                chatService.remove(removeChatWrapper);
            } catch (Exception e) {
                log.error("清理应用[{}]聊天记录失败", application.getId(), e);
            }
        }
        log.info("结束清理应用聊天记录");
    }
}
