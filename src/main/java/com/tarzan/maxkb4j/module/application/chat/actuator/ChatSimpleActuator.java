package com.tarzan.maxkb4j.module.application.chat.actuator;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.tarzan.maxkb4j.core.exception.ApiException;
import com.tarzan.maxkb4j.module.application.cache.ChatCache;
import com.tarzan.maxkb4j.module.application.chat.base.ChatBaseActuator;
import com.tarzan.maxkb4j.module.application.domian.dto.ChatInfo;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatEntity;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationKnowledgeMappingEntity;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationChatRecordVO;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.application.handler.PostResponseHandler;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationChatMapper;
import com.tarzan.maxkb4j.module.application.ragpipeline.PipelineManage;
import com.tarzan.maxkb4j.module.application.ragpipeline.step.chatstep.impl.BaseChatStep;
import com.tarzan.maxkb4j.module.application.ragpipeline.step.resetproblemstep.impl.BaseResetProblemStep;
import com.tarzan.maxkb4j.module.application.ragpipeline.step.searchdatasetstep.impl.SearchDatasetStep;
import com.tarzan.maxkb4j.module.application.service.ApplicationChatRecordService;
import com.tarzan.maxkb4j.module.application.service.ApplicationKnowledgeMappingService;
import com.tarzan.maxkb4j.module.application.service.ApplicationService;
import com.tarzan.maxkb4j.module.chat.ChatParams;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.ParagraphVO;
import com.tarzan.maxkb4j.module.model.info.entity.ModelEntity;
import com.tarzan.maxkb4j.module.model.info.service.ModelService;
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


    private final ModelService modelService;
    private final SearchDatasetStep searchDatasetStep;
    private final BaseChatStep baseChatStep;
    private final BaseResetProblemStep baseResetProblemStep;
    private final ApplicationChatRecordService chatRecordService;
    private final ApplicationKnowledgeMappingService datasetMappingService;
    private final ApplicationChatMapper chatMapper;
    private final ApplicationService applicationService;
    private final PostResponseHandler postResponseHandler;

    @Override
    public String chatOpenTest(ApplicationVO application) {
        ChatInfo chatInfo = new ChatInfo();
        chatInfo.setChatId(IdWorker.get32UUID());
        //application.setId(null); // 清空id,为了区分是否是测试对话
        chatInfo.setApplication(application);
        ChatCache.put(chatInfo.getChatId(), chatInfo);
        return chatInfo.getChatId();
    }

    @Override
    public String chatOpen(ApplicationVO application, String chatId) {
        ChatInfo chatInfo = new ChatInfo();
        chatInfo.setChatId(chatId);
        List<ApplicationKnowledgeMappingEntity> list = datasetMappingService.lambdaQuery().eq(ApplicationKnowledgeMappingEntity::getApplicationId, application.getId()).list();
        application.setKnowledgeIdList(list.stream().map(ApplicationKnowledgeMappingEntity::getKnowledgeId).toList());
        chatInfo.setApplication(application);
        ChatCache.put(chatInfo.getChatId(), chatInfo);
        return chatId;
    }

    @Override
    public String chatMessage(ChatParams chatParams) {
        long startTime = System.currentTimeMillis();
        chatParams.setChatRecordId(chatParams.getChatRecordId() == null ? IdWorker.get32UUID() : chatParams.getChatRecordId());
        ChatInfo chatInfo = getChatInfo(chatParams.getChatId());
        chatCheck(chatInfo,chatParams);
        String modelId = chatInfo.getApplication().getModelId();
        ModelEntity model = modelService.getById(modelId);
        if (Objects.isNull(model) || !"SUCCESS".equals(model.getStatus())) {
            throw new ApiException("当前模型不可用");
        }
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
        ApplicationVO application = chatInfo.getApplication();
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
        Map<String, Object> params = chatInfo.toPipelineManageParams(chatParams.getChatRecordId(),problemText, excludeParagraphIds, "", "", stream);
        String answer =  pipelineManage.run(params,chatParams.getSink());
        JSONObject details=pipelineManage.getDetails();
        postResponseHandler.handler(chatParams.getChatId(), chatParams.getChatRecordId(), problemText, answer, null,details, startTime, "", "",chatParams.isDebug());
        return answer;
    }

    @Override
    public ChatInfo reChatOpen(String chatId) {
        ApplicationChatEntity chatEntity = chatMapper.selectById(chatId);
        if (chatEntity == null){
            return null;
        }
        ChatInfo chatInfo = new ChatInfo();
        chatInfo.setChatId(chatId);
        ApplicationVO application = applicationService.getDetail(chatEntity.getApplicationId());
        List<ApplicationKnowledgeMappingEntity> list = datasetMappingService.lambdaQuery().eq(ApplicationKnowledgeMappingEntity::getApplicationId, application.getId()).list();
        application.setKnowledgeIdList(list.stream().map(ApplicationKnowledgeMappingEntity::getKnowledgeId).toList());
        chatInfo.setApplication(application);
        ChatCache.put(chatInfo.getChatId(), chatInfo);
        return chatInfo;
    }
}
