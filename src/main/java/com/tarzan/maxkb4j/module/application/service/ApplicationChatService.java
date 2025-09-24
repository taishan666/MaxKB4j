package com.tarzan.maxkb4j.module.application.service;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.core.exception.ApiException;
import com.tarzan.maxkb4j.core.workflow.domain.ChatFile;
import com.tarzan.maxkb4j.module.application.cache.ChatCache;
import com.tarzan.maxkb4j.module.application.chat.provider.ChatActuatorBuilder;
import com.tarzan.maxkb4j.module.application.chat.provider.IChatActuator;
import com.tarzan.maxkb4j.module.application.domian.dto.ChatInfo;
import com.tarzan.maxkb4j.module.application.domian.dto.ChatQueryDTO;
import com.tarzan.maxkb4j.module.application.domian.entity.*;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.application.domian.vo.ChatRecordDetailVO;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationChatMapper;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationMapper;
import com.tarzan.maxkb4j.module.chat.ChatParams;
import com.tarzan.maxkb4j.module.oss.service.MongoFileService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author tarzan
 * @date 2024-12-26 09:50:23
 */
@Service
@AllArgsConstructor
public class ApplicationChatService extends ServiceImpl<ApplicationChatMapper, ApplicationChatEntity> {

    private final ApplicationMapper applicationMapper;
    private final ApplicationVersionService applicationVersionService;
    private final ApplicationChatRecordService chatRecordService;
    private final ApplicationService applicationService;
    private final MongoFileService fileService;
    private final ApplicationChatUserStatsService userStatsService;
    private final ApplicationAccessTokenService accessTokenService;


    public IPage<ApplicationChatEntity> chatLogs(String appId, int page, int size, ChatQueryDTO query) {
        Page<ApplicationChatEntity> chatPage = new Page<>(page, size);
        return baseMapper.chatLogs(chatPage, appId, query);
    }


    public String chatOpenTest(String appId) {
        ApplicationVO application = applicationService.getDetail(appId);
        IChatActuator chatActuator = ChatActuatorBuilder.getActuator(application.getType());
        return chatActuator.chatOpenTest(application);
    }


    public String chatOpen(String appId) {
        return chatOpen(appId, null);
    }

    public String chatOpen(String appId, String chatId) {
        ApplicationVO application = applicationVersionService.getDetail(appId);
        if (application==null){
            throw  new ApiException("应用未发布，请发布后使用。");
        }
        if (StringUtils.isBlank(chatId)) {
            chatId = IdWorker.get32UUID();
        }
        IChatActuator chatActuator = ChatActuatorBuilder.getActuator(application.getType());
        return chatActuator.chatOpen(application, chatId);
    }

    // 当前会话不存在时，重新打开会话（测试模式下不生效）
    public ChatInfo reChatOpen(String chatId) {
        ApplicationChatEntity chatEntity = this.getById(chatId);
        if (chatEntity == null) {
            return null;
        }
        ApplicationEntity application = applicationMapper.selectById(chatEntity.getApplicationId());
        ChatInfo chatInfo = new ChatInfo();
        chatInfo.setChatId(chatId);
        chatInfo.setAppId(application.getId());
        chatInfo.setAppType(application.getType());
        chatInfo.setDebug(true);
        ChatCache.put(chatInfo.getChatId(), chatInfo);
        return chatInfo;
    }

    private ChatInfo getChatInfo(String chatId) {
        ChatInfo chatInfo = ChatCache.get(chatId);
        if (chatInfo == null) {
            return reChatOpen(chatId);
        }
        return chatInfo;
    }

    //todo 优化
    public String chatMessage(ChatParams chatParams, boolean debug) {
        ChatInfo chatInfo = getChatInfo(chatParams.getChatId());
        if (chatInfo == null) {
            chatParams.getSink().tryEmitError(new ApiException("会话不存在"));
            return "";
        }
        String appId = chatInfo.getAppId();
        visitCountCheck(appId, chatParams.getUserId(), debug);
        String appType = chatInfo.getAppType();
        IChatActuator chatActuator = ChatActuatorBuilder.getActuator(appType);
        String answer = chatActuator.chatMessage(chatParams, debug);
        chatParams.getSink().tryEmitComplete();
        return answer;
    }

    public void visitCountCheck(String appId, String chatUserId, boolean debug) {
        if (!debug && Objects.nonNull(appId)) {
            ApplicationChatUserStatsEntity accessClient = userStatsService.getById(chatUserId);
            if (Objects.isNull(accessClient)) {
                accessClient = new ApplicationChatUserStatsEntity();
                accessClient.setId(chatUserId);
                accessClient.setApplicationId(appId);
                accessClient.setAccessNum(0);
                accessClient.setIntraDayAccessNum(0);
                userStatsService.save(accessClient);
            }
            ApplicationAccessTokenEntity appAccessToken = accessTokenService.lambdaQuery().eq(ApplicationAccessTokenEntity::getApplicationId, appId).one();
            if (Objects.nonNull(appAccessToken)){
                if (appAccessToken.getAccessNum() < accessClient.getIntraDayAccessNum()) {
                    throw new ApiException("访问次数超过今日访问量");
                }
            }

        }
    }


    public List<ChatFile> uploadFile(String id, String chatId, MultipartFile[] files) {
        List<ChatFile> fileList = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                fileList.add(fileService.uploadFile(file));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return fileList;
    }

    public void chatExport(List<String> ids, HttpServletResponse response) throws IOException {
        List<ChatRecordDetailVO> rows = baseMapper.chatRecordDetail(ids);
        EasyExcel.write(response.getOutputStream(), ChatRecordDetailVO.class).sheet("sheet").doWrite(rows);
    }

    @Transactional
    public Boolean deleteById(String chatId) {
        chatRecordService.lambdaUpdate().eq(ApplicationChatRecordEntity::getChatId, chatId).remove();
        return this.removeById(chatId);
    }
}
