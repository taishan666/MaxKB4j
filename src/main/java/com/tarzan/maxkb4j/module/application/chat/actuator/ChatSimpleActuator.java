package com.tarzan.maxkb4j.module.application.chat.actuator;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.tarzan.maxkb4j.module.application.cache.ChatCache;
import com.tarzan.maxkb4j.module.application.chat.base.ChatBaseActuator;
import com.tarzan.maxkb4j.module.application.domian.dto.ChatInfo;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationChatRecordVO;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.application.enums.AppType;
import com.tarzan.maxkb4j.module.application.handler.PostResponseHandler;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationChatMapper;
import com.tarzan.maxkb4j.module.application.ragpipeline.PipelineManage;
import com.tarzan.maxkb4j.module.application.ragpipeline.step.chatstep.impl.BaseChatStep;
import com.tarzan.maxkb4j.module.application.ragpipeline.step.resetproblemstep.impl.BaseResetProblemStep;
import com.tarzan.maxkb4j.module.application.ragpipeline.step.searchdatasetstep.impl.SearchDatasetStep;
import com.tarzan.maxkb4j.module.application.service.ApplicationChatRecordService;
import com.tarzan.maxkb4j.module.application.service.ApplicationKnowledgeMappingService;
import com.tarzan.maxkb4j.module.application.service.ApplicationService;
import com.tarzan.maxkb4j.module.application.service.ApplicationVersionService;
import com.tarzan.maxkb4j.module.chat.ChatParams;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.ParagraphVO;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@AllArgsConstructor
@Component
public class ChatSimpleActuator extends ChatBaseActuator {


    private final SearchDatasetStep searchDatasetStep;
    private final BaseChatStep baseChatStep;
    private final BaseResetProblemStep baseResetProblemStep;
    private final ApplicationChatRecordService chatRecordService;
    private final ApplicationKnowledgeMappingService datasetMappingService;
    private final ApplicationChatMapper chatMapper;
    private final ApplicationService applicationService;
    private final ApplicationVersionService applicationVersionService;
    private final PostResponseHandler postResponseHandler;

    @Override
    public String chatOpenTest(ApplicationVO application) {
        ChatInfo chatInfo = new ChatInfo();
        chatInfo.setChatId(IdWorker.get32UUID());
        chatInfo.setAppId(application.getId());
        chatInfo.setAppType(AppType.SIMPLE.name());
        chatInfo.setDebug(true);
        ChatCache.put(chatInfo.getChatId(), chatInfo);
        return chatInfo.getChatId();
    }

    @Override
    public String chatOpen(ApplicationVO application, String chatId) {
        ChatInfo chatInfo = new ChatInfo();
        chatInfo.setChatId(chatId);
        chatInfo.setAppId(application.getId());
        chatInfo.setAppType(AppType.SIMPLE.name());
        ChatCache.put(chatInfo.getChatId(), chatInfo);
        return chatId;
    }

    @Override
    public String chatMessage(ChatParams chatParams,boolean debug) {
        long startTime = System.currentTimeMillis();
        ChatInfo chatInfo = ChatCache.get(chatParams.getChatId());
        boolean stream = chatParams.getStream() == null || chatParams.getStream();
        String problemText = chatParams.getMessage();
        boolean reChat = chatParams.getReChat();
        List<String> excludeParagraphIds = new ArrayList<>();
        if (reChat) {
            //todo 获取聊天记录
            String chatRecordId = chatParams.getChatRecordId();
            if (Objects.nonNull(chatRecordId)) {
                ApplicationChatRecordVO chatRecord = chatRecordService.getChatRecordInfo(chatInfo, chatRecordId);
                List<ParagraphVO> paragraphs = chatRecord.getParagraphList();
                if (!CollectionUtils.isEmpty(paragraphs)) {
                    excludeParagraphIds = paragraphs.stream().map(ParagraphVO::getId).toList();
                }
            }
        }
        ApplicationVO application;
        if (debug){
             application = applicationService.getDetail(chatInfo.getAppId());
        }else {
             application = applicationVersionService.getDetail(chatInfo.getAppId());
        }

        PipelineManage.Builder pipelineManageBuilder = new PipelineManage.Builder();
        Boolean problemOptimization = application.getProblemOptimization();
        if (!CollectionUtils.isEmpty(application.getKnowledgeIdList())) {
            if (Objects.nonNull(problemOptimization) && problemOptimization) {
                pipelineManageBuilder.addStep(baseResetProblemStep);
            }
            pipelineManageBuilder.addStep(searchDatasetStep);
        }
        pipelineManageBuilder.addStep(baseChatStep);
        PipelineManage pipelineManage = pipelineManageBuilder.build();
        Map<String, Object> params = chatInfo.toPipelineManageParams(application, chatParams.getChatRecordId(),problemText, excludeParagraphIds,"", "", stream);
        String answer =  pipelineManage.run(params,chatParams.getSink());
        JSONObject details=pipelineManage.getDetails();
        postResponseHandler.handler(chatParams.getChatId(), chatParams.getChatRecordId(), problemText, answer,details, startTime, "", "",debug);
        return answer;
    }

}
