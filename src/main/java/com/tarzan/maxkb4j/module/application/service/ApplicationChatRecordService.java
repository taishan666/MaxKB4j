package com.tarzan.maxkb4j.module.application.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.common.util.BeanUtil;
import com.tarzan.maxkb4j.common.util.PageUtil;
import com.tarzan.maxkb4j.module.application.domian.dto.AddChatImproveDTO;
import com.tarzan.maxkb4j.module.application.domian.dto.ChatImproveDTO;
import com.tarzan.maxkb4j.module.application.domian.dto.ChatInfo;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatEntity;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationChatRecordVO;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationChatMapper;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationChatRecordMapper;
import com.tarzan.maxkb4j.module.chat.cache.ChatCache;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.ParagraphEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.ParagraphVO;
import com.tarzan.maxkb4j.module.knowledge.service.ParagraphService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.SEARCH_KNOWLEDGE;

/**
 * @author tarzan
 * @date 2025-01-10 11:46:06
 */
@AllArgsConstructor
@Service
public class ApplicationChatRecordService extends ServiceImpl<ApplicationChatRecordMapper, ApplicationChatRecordEntity> {

    private final ParagraphService paragraphService;
    private final ApplicationChatMapper chatMapper;



    private ApplicationChatRecordEntity getChatRecordEntity(ChatInfo chatInfo, String chatRecordId) {
        ApplicationChatRecordEntity chatRecord = null;
        if (Objects.nonNull(chatInfo) && !CollectionUtils.isEmpty(chatInfo.getChatRecordList())) {
            chatRecord = chatInfo.getChatRecordList().stream()
                    .filter(e -> e.getId().equals(chatRecordId))
                    .reduce((first, second) -> second) // 保留最后一个匹配的元素
                    .orElse(null);
        }
        if (Objects.isNull(chatRecord)) {
            chatRecord = this.getById(chatRecordId);
        }
        return chatRecord;
    }

    public List<ApplicationChatRecordEntity> getChatRecords(String chatId) {
        ChatInfo chatInfo = ChatCache.get(chatId);
        List<ApplicationChatRecordEntity> chatRecords = new ArrayList<>();
        if (Objects.nonNull(chatInfo)) {
            chatRecords = chatInfo.getChatRecordList();
        }
        if (CollectionUtils.isEmpty(chatRecords)) {
            chatRecords = this.lambdaQuery().eq(ApplicationChatRecordEntity::getChatId, chatId).list();
        }
        return chatRecords;
    }

    public ApplicationChatRecordVO getChatRecordInfo(String chatId, String chatRecordId) {
        ChatInfo chatInfo = ChatCache.get(chatId);
        ApplicationChatRecordEntity chatRecord = getChatRecordEntity(chatInfo, chatRecordId);
        return convert(chatRecord);
    }

    private ApplicationChatRecordVO convert(ApplicationChatRecordEntity chatRecord) {
        if (Objects.isNull(chatRecord)) {
            return null;
        }
        ApplicationChatRecordVO chatRecordVO = BeanUtil.copy(chatRecord, ApplicationChatRecordVO.class);
        chatRecordVO.setParagraphList(new ArrayList<>());
        JSONObject details = chatRecord.getDetails();
        if (!details.isEmpty()) {
            JSONObject searchStep = details.getJSONObject("search_step");
            if (searchStep != null && !searchStep.isEmpty()) {
                JSONArray paragraphList = searchStep.getJSONArray("paragraphList");
                if (!CollectionUtils.isEmpty(paragraphList)) {
                    String json = JSONObject.toJSONString(paragraphList);
                    chatRecordVO.setParagraphList(JSON.parseArray(json, ParagraphVO.class));
                }
            }
            JSONObject problemPadding = details.getJSONObject("problem_padding");
            if (problemPadding != null && !problemPadding.isEmpty()) {
                chatRecordVO.setPaddingProblemText(problemPadding.getString("paddingProblemText"));
            }
            List<JSONObject> executionDetails = new ArrayList<>();
            details.keySet().forEach(key -> executionDetails.add(details.getJSONObject(key)));
            Collections.reverse(executionDetails);
            chatRecordVO.setExecutionDetails(executionDetails);
            for (JSONObject detail : executionDetails) {
                if (SEARCH_KNOWLEDGE.getKey().equals(detail.getString("type"))) {
                    boolean showKnowledge = detail.getBooleanValue("showKnowledge");
                    if (showKnowledge) {
                        Object paragraphListObj = detail.get("paragraphList"); // 假设每个节点都有 id 字段
                        if (paragraphListObj != null) {
                            @SuppressWarnings("unchecked")
                            List<ParagraphVO> list = (List<ParagraphVO>) paragraphListObj;
                            chatRecordVO.getParagraphList().addAll(list);
                        }
                    }
                }
            }
        }
        return chatRecordVO;
    }


    public IPage<ApplicationChatRecordVO> chatRecordPage(String chatId, int current, int size) {
        Page<ApplicationChatRecordEntity> chatRecordpage = new Page<>(current, size);
        LambdaQueryWrapper<ApplicationChatRecordEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ApplicationChatRecordEntity::getChatId, chatId);
        IPage<ApplicationChatRecordEntity> chatRecordIpage = this.page(chatRecordpage, wrapper);
        return PageUtil.copy(chatRecordIpage, this::convert);
    }


    @Transactional
    public boolean addChatLogs(String appId, AddChatImproveDTO dto) {
        List<ApplicationChatRecordEntity> chatRecords = this.lambdaQuery().select(ApplicationChatRecordEntity::getProblemText, ApplicationChatRecordEntity::getAnswerText).in(ApplicationChatRecordEntity::getChatId, dto.getChatIds()).list();
        List<ParagraphEntity> paragraphs=new ArrayList<>();
        for (ApplicationChatRecordEntity e : chatRecords) {
            ParagraphEntity paragraphEntity = paragraphService.createParagraph(dto.getKnowledgeId(), dto.getDocumentId(), e.getProblemText(), e.getAnswerText(), null);
            paragraphs.add(paragraphEntity);
        }
        return paragraphService.saveBatch(paragraphs);
    }

    @Transactional
    public ApplicationChatRecordEntity improveChatLog(String chatId,String chatRecordId,String knowledgeId, String docId, ChatImproveDTO dto) {
        ParagraphEntity paragraphEntity = paragraphService.createParagraph(knowledgeId, docId, dto.getTitle(), dto.getContent(), null);
        paragraphService.saveParagraphAndProblem(paragraphEntity,List.of(dto.getProblemText()));
        ApplicationChatRecordEntity chatRecord = new ApplicationChatRecordEntity();
        chatRecord.setId(chatRecordId);
        chatRecord.setImproveParagraphIdList(List.of(paragraphEntity.getId()));
        this.updateById(chatRecord);
        ApplicationChatEntity chatEntity = chatMapper.selectById(chatId);
        ApplicationChatEntity updateChatEntity=new ApplicationChatEntity();
        updateChatEntity.setId(chatId);
        updateChatEntity.setMarkSum(chatEntity.getMarkSum()+1);
        chatMapper.updateById(updateChatEntity);
        return this.getById(chatRecordId);
    }

    @Transactional
    public boolean removeImproveChatLog(String chatId,String chatRecordId,String paragraphId) {
        ApplicationChatRecordEntity chatRecord = new ApplicationChatRecordEntity();
        chatRecord.setId(chatRecordId);
        chatRecord.setImproveParagraphIdList(List.of());
        this.updateById(chatRecord);
        ApplicationChatEntity chatEntity = chatMapper.selectById(chatId);
        ApplicationChatEntity updateChatEntity=new ApplicationChatEntity();
        updateChatEntity.setId(chatId);
        updateChatEntity.setMarkSum(chatEntity.getMarkSum()-1);
        chatMapper.updateById(updateChatEntity);
        return paragraphService.deleteById(paragraphId);
    }

    public List<ParagraphEntity> improveChatLog(String chatRecordId) {
        ApplicationChatRecordEntity chatRecord =this.getById(chatRecordId);
        return paragraphService.listByIds(chatRecord.getImproveParagraphIdList());
    }

}
