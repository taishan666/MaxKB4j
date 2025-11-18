package com.tarzan.maxkb4j.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatEntity;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationChatMapper;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationChatRecordMapper;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class ChatRecordClearJob {

    @Resource
    private ApplicationMapper applicationMapper;
    @Resource
    private ApplicationChatMapper chatMapper;
    @Resource
    private ApplicationChatRecordMapper chatRecordMapper;

    @Scheduled(cron = "0 0 6 * * *")
    public void execute() {
        log.info("开始应用聊天记录");
        LambdaQueryWrapper<ApplicationEntity> appWrapper=Wrappers.lambdaQuery();
        appWrapper.select(ApplicationEntity::getId,ApplicationEntity::getCleanTime);
        List<ApplicationEntity> applications = applicationMapper.selectList(appWrapper);
        for (ApplicationEntity application : applications) {
            long cleanTime = application.getCleanTime();
            Date cleanTimeAgo = Date.from(Instant.now().minusSeconds(cleanTime * 24 * 60 * 60));
            LambdaQueryWrapper<ApplicationChatEntity> chatWrapper=Wrappers.lambdaQuery();
            chatWrapper.eq(ApplicationChatEntity::getId,application.getId());
            chatWrapper.lt(ApplicationChatEntity::getCreateTime,cleanTimeAgo);
            chatMapper.delete(chatWrapper);
            LambdaQueryWrapper<ApplicationChatRecordEntity> chatRecordWrapper=Wrappers.lambdaQuery();
            chatRecordWrapper.eq(ApplicationChatRecordEntity::getId,application.getId());
            chatRecordWrapper.lt(ApplicationChatRecordEntity::getCreateTime,cleanTimeAgo);
            chatRecordMapper.delete(chatRecordWrapper);
        }
        log.info("结束应用聊天记录");
    }
}
