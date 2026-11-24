package com.maxkb4j.application.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.application.dto.AddChatImproveDTO;
import com.maxkb4j.application.dto.ApplicationChatRecordDTO;
import com.maxkb4j.application.dto.ChatImproveDTO;
import com.maxkb4j.application.entity.ApplicationChatEntity;
import com.maxkb4j.application.entity.ApplicationChatRecordEntity;
import com.maxkb4j.application.mapper.ApplicationChatMapper;
import com.maxkb4j.application.mapper.ApplicationChatRecordMapper;
import com.maxkb4j.application.service.IApplicationChatRecordInternalService;
import com.maxkb4j.application.vo.ApplicationChatRecordVO;
import com.maxkb4j.application.vo.ParagraphRecordVO;
import com.maxkb4j.common.cache.ChatInfoStore;
import com.maxkb4j.common.constant.ResourceType;
import com.maxkb4j.common.domain.dto.ChatInfo;
import com.maxkb4j.common.domain.dto.ChatRecordDTO;
import com.maxkb4j.common.util.BeanUtil;
import com.maxkb4j.knowledge.dto.ParagraphDTO;
import com.maxkb4j.knowledge.service.IDocumentService;
import com.maxkb4j.knowledge.service.IParagraphService;
import com.maxkb4j.system.service.IResourceMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.maxkb4j.workflow.enums.NodeType.SEARCH_KNOWLEDGE;


/**
 * @author tarzan
 * @date 2025-01-10 11:46:06
 */
@RequiredArgsConstructor
@Service
public class ApplicationChatRecordServiceImpl extends ServiceImpl<ApplicationChatRecordMapper, ApplicationChatRecordEntity> implements IApplicationChatRecordInternalService {

    private final IParagraphService paragraphService;
    private final ApplicationChatMapper chatMapper;
    private final IDocumentService documentService;
    private final IResourceMappingService resourceMappingService;
    private final ChatInfoStore chatInfoStore;

    private boolean chatBelongsToApp(String appId, String chatId) {
        return chatMapper.selectCount(Wrappers.<ApplicationChatEntity>lambdaQuery()
                .eq(ApplicationChatEntity::getId, chatId)
                .eq(ApplicationChatEntity::getApplicationId, appId)) > 0;
    }

    private ApplicationChatRecordEntity getOwnedChatRecord(String appId, String chatId, String chatRecordId) {
        if (chatBelongsToApp(appId, chatId)) {
            return baseMapper.selectOne(Wrappers.<ApplicationChatRecordEntity>lambdaQuery()
                    .eq(ApplicationChatRecordEntity::getId, chatRecordId)
                    .eq(ApplicationChatRecordEntity::getChatId, chatId));
        }
        return null;
    }

    private boolean knowledgeDocumentBelongsToApp(String appId, String knowledgeId, String docId) {
        return resourceMappingService.existsMapping(
                ResourceType.APPLICATION, appId, ResourceType.KNOWLEDGE, knowledgeId)
                && documentService.existsByKnowledgeIdAndId(knowledgeId, docId);
    }

    private boolean paragraphBelongsToRecord(ApplicationChatRecordEntity chatRecord, String knowledgeId,
                                             String docId, String paragraphId) {
        if (CollectionUtils.isEmpty(chatRecord.getImproveParagraphIdList())
                || !chatRecord.getImproveParagraphIdList().contains(paragraphId)) {
            return false;
        }
        return paragraphService.listDtoByIds(List.of(paragraphId)).stream()
                .anyMatch(paragraph -> Objects.equals(knowledgeId, paragraph.getKnowledgeId())
                        && Objects.equals(docId, paragraph.getDocumentId()));
    }

    @Override
    public List<ChatRecordDTO> getChatRecords(String chatId) {
        // 缓存冷启动时由 DB 回填历史上下文：取“最近 100 条”再反转为时间正序（旧→新），
        // 与缓存内 addChatRecord 的追加顺序保持一致；无 ORDER BY 时取行与顺序均不确定
        List<ApplicationChatRecordEntity> chatRecordList = this.lambdaQuery()
                .eq(ApplicationChatRecordEntity::getChatId, chatId)
                .orderByDesc(ApplicationChatRecordEntity::getCreateTime)
                .last("limit 100")
                .list();
        if (CollectionUtils.isEmpty(chatRecordList)) {
            return Collections.emptyList();
        }
        Collections.reverse(chatRecordList);
        return BeanUtil.copyList(chatRecordList, ChatRecordDTO.class);
    }

    @Override
    public ChatRecordDTO getChatRecord(String appId, String chatId, String chatRecordId) {
        ChatInfo chatInfo = chatInfoStore.get(chatId);
        if (Objects.nonNull(chatInfo)) {
            // 缓存命中：校验会话归属，防止跨应用越权读取
            if (!Objects.equals(appId, chatInfo.getAppId())) {
                return null;
            }
            if (!CollectionUtils.isEmpty(chatInfo.getChatRecordList())) {
                ChatRecordDTO cached = chatInfo.getChatRecordList().stream()
                        .filter(e -> Objects.equals(e.getId(), chatRecordId))
                        .reduce((first, second) -> second) // 保留最后一个匹配的元素
                        .orElse(null);
                if (Objects.nonNull(cached)) {
                    return cached;
                }
            }
            // 缓存仅保留最近 100 条，未命中时按 chatId + recordId 回退查库（归属已在上方校验）
            return getChatRecordFromDb(chatId, chatRecordId);
        }
        // 缓存冷启动（会话过期/淘汰）：回退查库，但必须校验会话归属该应用
        if (!chatBelongsToApp(appId, chatId)) {
            return null;
        }
        return getChatRecordFromDb(chatId, chatRecordId);
    }

    private ChatRecordDTO getChatRecordFromDb(String chatId, String chatRecordId) {
        ApplicationChatRecordEntity chatRecordEntity = baseMapper.selectOne(
                Wrappers.<ApplicationChatRecordEntity>lambdaQuery()
                        .eq(ApplicationChatRecordEntity::getId, chatRecordId)
                        .eq(ApplicationChatRecordEntity::getChatId, chatId));
        return chatRecordEntity == null ? null : BeanUtil.copy(chatRecordEntity, ChatRecordDTO.class);
    }

    @Override
    public ApplicationChatRecordVO getChatRecordInfo(String appId, String chatId, String chatRecordId) {
        return convert(getChatRecord(appId,chatId, chatRecordId));
    }

    private ApplicationChatRecordVO convert(ApplicationChatRecordEntity chatRecordEntity) {
        ChatRecordDTO chatRecord = BeanUtil.copy(chatRecordEntity, ChatRecordDTO.class);
        return convert(chatRecord);
    }

    private ApplicationChatRecordVO convert(ChatRecordDTO chatRecord) {
        if (Objects.isNull(chatRecord)) {
            return null;
        }
        ApplicationChatRecordVO chatRecordVO = BeanUtil.copy(chatRecord, ApplicationChatRecordVO.class);
        chatRecordVO.setParagraphList(new ArrayList<>());
        JSONObject details = chatRecord.getDetails();
        if (details != null && !details.isEmpty()) {
            JSONObject searchStep = details.getJSONObject("search_step");
            if (searchStep != null && !searchStep.isEmpty()) {
                JSONArray paragraphList = searchStep.getJSONArray("paragraphList");
                if (!CollectionUtils.isEmpty(paragraphList)) {
                    String json = JSONObject.toJSONString(paragraphList);
                    chatRecordVO.setParagraphList(JSON.parseArray(json, ParagraphRecordVO.class));
                }
            }
            JSONObject problemPadding = details.getJSONObject("problem_padding");
            if (problemPadding != null && !problemPadding.isEmpty()) {
                chatRecordVO.setPaddingProblemText(problemPadding.getString("paddingProblemText"));
            }
            List<JSONObject> executionDetails = new ArrayList<>();
            for (String key : details.keySet()) {
                JSONObject detail = details.getJSONObject(key);
                if (detail == null) {
                    continue;
                }
                if (SEARCH_KNOWLEDGE.getKey().equals(detail.getString("type"))) {
                    boolean showKnowledge = detail.getBooleanValue("showKnowledge");
                    if (showKnowledge) {
                        Object paragraphListObj = detail.get("paragraphList");
                        if (paragraphListObj != null) {
                            // elements 是 JSONObject，直接强转会在下游按 VO 类型访问时抛 ClassCastException，需经序列化后按类型反解
                            String json = JSONObject.toJSONString(paragraphListObj);
                            chatRecordVO.getParagraphList().addAll(JSON.parseArray(json, ParagraphRecordVO.class));
                        }
                    }
                }
                executionDetails.add(detail);
            }
            Collections.reverse(executionDetails);
            chatRecordVO.setExecutionDetails(executionDetails);
        }
        return chatRecordVO;
    }


    @Override
    public IPage<ApplicationChatRecordVO> chatRecordPage(String appId, String chatId, int current, int size) {
        if (chatBelongsToApp(appId, chatId)) {
            return chatRecordPage(chatId, current, size);
        }
        return new Page<>(current, size);
    }

    public IPage<ApplicationChatRecordVO> chatRecordPage(String chatId, int current, int size) {
        Page<ApplicationChatRecordEntity> chatRecordpage = new Page<>(current, size);
        LambdaQueryWrapper<ApplicationChatRecordEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ApplicationChatRecordEntity::getChatId, chatId);
        wrapper.orderByDesc(ApplicationChatRecordEntity::getCreateTime);
        IPage<ApplicationChatRecordEntity> chatRecordIpage = this.page(chatRecordpage, wrapper);
        return BeanUtil.copyPage(chatRecordIpage, this::convert);
    }

    @Override
    public List<ApplicationChatRecordVO> listVOByIds(String chatId, List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return List.of();
        }
        List<ApplicationChatRecordEntity> chatRecordList = baseMapper.selectList(
                Wrappers.<ApplicationChatRecordEntity>lambdaQuery()
                        .select(ApplicationChatRecordEntity::getId,
                                ApplicationChatRecordEntity::getProblemText,
                                ApplicationChatRecordEntity::getAnswerText,
                                ApplicationChatRecordEntity::getAnswerTextList,
                                ApplicationChatRecordEntity::getDetails,
                                ApplicationChatRecordEntity::getCreateTime)
                        .eq(ApplicationChatRecordEntity::getChatId, chatId)
                        .in(ApplicationChatRecordEntity::getId, ids));
        return BeanUtil.copyList(chatRecordList, this::convert);
    }

    @Override
    public void updateDtoById(ApplicationChatRecordDTO applicationChatDTO) {
        this.lambdaUpdate()
                .eq(ApplicationChatRecordEntity::getId, applicationChatDTO.getId())
                .eq(applicationChatDTO.getChatId() != null, ApplicationChatRecordEntity::getChatId, applicationChatDTO.getChatId())
                .set(applicationChatDTO.getVoteStatus() != null, ApplicationChatRecordEntity::getVoteStatus, applicationChatDTO.getVoteStatus())
                .set(applicationChatDTO.getVoteReason() != null, ApplicationChatRecordEntity::getVoteReason, applicationChatDTO.getVoteReason())
                .set(applicationChatDTO.getVoteOtherContent() != null, ApplicationChatRecordEntity::getVoteOtherContent, applicationChatDTO.getVoteOtherContent())
                .update();
    }

    @Override
    public List<ApplicationChatRecordDTO> listVoteStatusByChatId(String chatId) {
        List<ApplicationChatRecordEntity> chatRecordEntities = this.lambdaQuery().select(ApplicationChatRecordEntity::getVoteStatus).eq(ApplicationChatRecordEntity::getChatId, chatId).list();
        return BeanUtil.copyList(chatRecordEntities, ApplicationChatRecordDTO.class);

    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addChatLogs(String appId, AddChatImproveDTO dto) {
        if (dto == null || CollectionUtils.isEmpty(dto.getChatIds())) {
            return false;
        }
        if (knowledgeDocumentBelongsToApp(appId, dto.getKnowledgeId(), dto.getDocumentId())) {
            List<String> chatIds = dto.getChatIds().stream().distinct().toList();
            long ownedChatCount = chatMapper.selectCount(Wrappers.<ApplicationChatEntity>lambdaQuery()
                    .eq(ApplicationChatEntity::getApplicationId, appId)
                    .in(ApplicationChatEntity::getId, chatIds));
            if (ownedChatCount != chatIds.size()) {
                return false;
            }
            List<ApplicationChatRecordEntity> chatRecords = baseMapper.selectList(
                    Wrappers.<ApplicationChatRecordEntity>lambdaQuery()
                            .select(ApplicationChatRecordEntity::getProblemText, ApplicationChatRecordEntity::getAnswerText)
                            .in(ApplicationChatRecordEntity::getChatId, chatIds));
            List<ParagraphDTO> paragraphs = new ArrayList<>();
            for (ApplicationChatRecordEntity e : chatRecords) {
                ParagraphDTO paragraphDTO = new ParagraphDTO(IdWorker.get32UUID(), dto.getKnowledgeId(), dto.getDocumentId(), e.getProblemText(), e.getAnswerText(), null);
                paragraphs.add(paragraphDTO);
            }
            return !paragraphs.isEmpty() && paragraphService.saveDtoBatch(paragraphs);
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApplicationChatRecordEntity improveChatLog(String appId, String chatId, String chatRecordId, String knowledgeId, String docId, ChatImproveDTO dto) {
        ApplicationChatRecordEntity ownedRecord = getOwnedChatRecord(appId, chatId, chatRecordId);
        if (ownedRecord == null) {
            return null;
        }
        if(knowledgeDocumentBelongsToApp(appId, knowledgeId, docId)){
            ParagraphDTO paragraphDTO = new ParagraphDTO(IdWorker.get32UUID(), knowledgeId, docId, dto.getTitle(), dto.getContent(), null);
            if (!paragraphService.saveParagraphAndProblem(paragraphDTO, List.of(dto.getProblemText()))) {
                return null;
            }
            ApplicationChatRecordEntity chatRecord = new ApplicationChatRecordEntity();
            chatRecord.setImproveParagraphIdList(List.of(paragraphDTO.getId()));
            baseMapper.update(chatRecord, Wrappers.<ApplicationChatRecordEntity>lambdaUpdate()
                    .eq(ApplicationChatRecordEntity::getId, chatRecordId)
                    .eq(ApplicationChatRecordEntity::getChatId, chatId));
            // 使用 setSql 实现原子自增，避免并发丢更新
            chatMapper.update(null, new LambdaUpdateWrapper<ApplicationChatEntity>()
                    .eq(ApplicationChatEntity::getId, chatId)
                    .eq(ApplicationChatEntity::getApplicationId, appId)
                    .setSql("mark_sum = COALESCE(mark_sum, 0) + 1"));
            return baseMapper.selectOne(Wrappers.<ApplicationChatRecordEntity>lambdaQuery()
                    .eq(ApplicationChatRecordEntity::getId, chatRecordId)
                    .eq(ApplicationChatRecordEntity::getChatId, chatId));
        }
        return  null;

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeImproveChatLog(String appId, String chatId, String chatRecordId, String knowledgeId, String docId, String paragraphId) {
        ApplicationChatRecordEntity ownedRecord = getOwnedChatRecord(appId, chatId, chatRecordId);
        if (ownedRecord == null || !knowledgeDocumentBelongsToApp(appId, knowledgeId, docId)) {
            return false;
        }
        if (paragraphBelongsToRecord(ownedRecord, knowledgeId, docId, paragraphId)){
            if (!paragraphService.deleteById(knowledgeId, paragraphId)) {
                return false;
            }
            ApplicationChatRecordEntity chatRecord = new ApplicationChatRecordEntity();
            chatRecord.setImproveParagraphIdList(List.of());
            baseMapper.update(chatRecord, Wrappers.<ApplicationChatRecordEntity>lambdaUpdate()
                    .eq(ApplicationChatRecordEntity::getId, chatRecordId)
                    .eq(ApplicationChatRecordEntity::getChatId, chatId));
            // 使用 setSql 实现原子递减，避免并发丢更新
            chatMapper.update(null, new LambdaUpdateWrapper<ApplicationChatEntity>()
                    .eq(ApplicationChatEntity::getId, chatId)
                    .eq(ApplicationChatEntity::getApplicationId, appId)
                    .setSql("mark_sum = GREATEST(COALESCE(mark_sum, 0) - 1, 0)"));
            return true;
        }
        return false;
    }

    @Override
    public List<ParagraphDTO> improveChatLog(String appId, String chatId, String chatRecordId) {
        ApplicationChatRecordEntity chatRecord = getOwnedChatRecord(appId, chatId, chatRecordId);
        if (chatRecord == null || CollectionUtils.isEmpty(chatRecord.getImproveParagraphIdList())) {
            return List.of();
        }
        return paragraphService.listDtoByIds(chatRecord.getImproveParagraphIdList());
    }

    public List<ApplicationChatRecordEntity> listByAppIdAndChatUserId(String applicationId, String chatUserId, int pageSize, int offset) {
        return baseMapper.listByAppIdAndChatUserId(applicationId, chatUserId, pageSize, offset);
    }

    public long countByAppIdAndChatUserId(String applicationId, String chatUserId) {
        return baseMapper.countByAppIdAndChatUserId(applicationId, chatUserId);
    }
}
