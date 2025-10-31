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
import com.tarzan.maxkb4j.module.chat.cache.ChatCache;
import com.tarzan.maxkb4j.module.application.domian.dto.AddChatImproveDTO;
import com.tarzan.maxkb4j.module.application.domian.dto.ChatImproveDTO;
import com.tarzan.maxkb4j.module.application.domian.dto.ChatInfo;
import com.tarzan.maxkb4j.module.application.domian.dto.ChatQueryDTO;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationChatRecordVO;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationChatUserStatsVO;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationStatisticsVO;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationChatRecordMapper;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.ParagraphEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.ParagraphVO;
import com.tarzan.maxkb4j.module.knowledge.service.ParagraphService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.SEARCH_KNOWLEDGE;

/**
 * @author tarzan
 * @date 2025-01-10 11:46:06
 */
@AllArgsConstructor
@Service
public class ApplicationChatRecordService extends ServiceImpl<ApplicationChatRecordMapper, ApplicationChatRecordEntity> {

    private final ApplicationChatUserStatsService chatUserStatsService;
    private final ParagraphService paragraphService;


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

    // 定义日期格式
    static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public IPage<ApplicationChatRecordVO> chatRecordPage(String chatId, int current, int size) {
        Page<ApplicationChatRecordEntity> chatRecordpage = new Page<>(current, size);
        LambdaQueryWrapper<ApplicationChatRecordEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ApplicationChatRecordEntity::getChatId, chatId);
        IPage<ApplicationChatRecordEntity> chatRecordIpage = this.page(chatRecordpage, wrapper);
        return PageUtil.copy(chatRecordIpage, this::convert);
    }

    public List<ApplicationStatisticsVO> applicationStats(String appId, ChatQueryDTO query) {
        List<ApplicationStatisticsVO> result = new ArrayList<>();
        List<ApplicationStatisticsVO> list = baseMapper.chatRecordCountTrend(appId, query);
        List<ApplicationChatUserStatsVO> accessClientList = chatUserStatsService.getCustomerCountTrend(appId, query);
        if (Objects.isNull(query.getStartTime()) || Objects.isNull(query.getEndTime())) {
            return result;
        }
        // 将字符串解析为LocalDate对象
        LocalDate startDate = LocalDate.parse(query.getStartTime(), formatter);
        LocalDate endDate = LocalDate.parse(query.getEndTime(), formatter);
        // 遍历从开始日期到结束日期之间的所有日期
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            String day = date.format(formatter);
            ApplicationStatisticsVO vo = getApplicationStatisticsVO(list, day);
            vo.setCustomerAddedCount(getCustomerAddedCount(accessClientList, day));
            result.add(vo);
        }
        return result;
    }

    public ApplicationStatisticsVO getApplicationStatisticsVO(List<ApplicationStatisticsVO> list, String day) {
        if (!CollectionUtils.isEmpty(list)) {
            Optional<ApplicationStatisticsVO> optional = list.stream().filter(e -> e.getDay().equals(day)).findFirst();
            if (optional.isPresent()) {
                return optional.get();
            }
        }
        ApplicationStatisticsVO vo = new ApplicationStatisticsVO();
        vo.setDay(day);
        vo.setStarNum(0);
        vo.setTokensNum(0);
        vo.setCustomerNum(0);
        vo.setChatRecordCount(0);
        vo.setTrampleNum(0);
        return vo;
    }

    public int getCustomerAddedCount(List<ApplicationChatUserStatsVO> list, String day) {
        if (!CollectionUtils.isEmpty(list)) {
            Optional<ApplicationChatUserStatsVO> optional = list.stream().filter(e -> e.getDay().equals(day)).findFirst();
            if (optional.isPresent()) {
                return optional.get().getCustomerAddedCount();
            }
        }
        return 0;
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
    public ApplicationChatRecordEntity improveChatLog(String chatRecordId,String knowledgeId, String docId, ChatImproveDTO dto) {
        ParagraphEntity paragraphEntity = paragraphService.createParagraph(knowledgeId, docId, dto.getProblemText(), dto.getContent(), null);
        paragraphService.save(paragraphEntity);
        ApplicationChatRecordEntity chatRecord = new ApplicationChatRecordEntity();
        chatRecord.setId(chatRecordId);
        chatRecord.setImproveParagraphIdList(List.of(paragraphEntity.getId()));
        this.updateById(chatRecord);
        return this.getById(chatRecordId);
    }

    @Transactional
    public boolean removeImproveChatLog(String chatRecordId,String paragraphId) {
        ApplicationChatRecordEntity chatRecord = new ApplicationChatRecordEntity();
        chatRecord.setId(chatRecordId);
        chatRecord.setImproveParagraphIdList(List.of());
        this.updateById(chatRecord);
        return paragraphService.removeById(paragraphId);
    }

    public List<ParagraphEntity> improveChatLog(String chatRecordId) {
        ApplicationChatRecordEntity chatRecord =this.getById(chatRecordId);
        return paragraphService.listByIds(chatRecord.getImproveParagraphIdList());
    }
}
