package com.tarzan.maxkb4j.module.application.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.application.dto.ChatInfo;
import com.tarzan.maxkb4j.module.application.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationChatRecordMapper;
import com.tarzan.maxkb4j.module.application.vo.ApplicationChatRecordVO;
import com.tarzan.maxkb4j.module.dataset.vo.ParagraphVO;
import com.tarzan.maxkb4j.util.BeanUtil;
import com.tarzan.maxkb4j.util.PageUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author tarzan
 * @date 2025-01-10 11:46:06
 */
@Service
public class ApplicationChatRecordService extends ServiceImpl<ApplicationChatRecordMapper, ApplicationChatRecordEntity>{

    public ApplicationChatRecordVO getChatRecordInfo(ChatInfo chatInfo,String chatRecordId) {
        ApplicationChatRecordEntity  chatRecord=null;
        if(Objects.nonNull(chatInfo)&&!CollectionUtils.isEmpty(chatInfo.getChatRecordList())) {
            chatRecord = chatInfo.getChatRecordList().stream()
                    .filter(e -> e.getId().equals(chatRecordId))
                    .reduce((first, second) -> second) // 保留最后一个匹配的元素
                    .orElse(null);
        }
        if(Objects.isNull(chatRecord)){
            chatRecord=this.getById(chatRecordId);
        }
        return convert(chatRecord);
    }

    private ApplicationChatRecordVO convert(ApplicationChatRecordEntity  chatRecord){
        if(Objects.isNull(chatRecord)) {
            return null;
        }
        ApplicationChatRecordVO chatRecordVO = BeanUtil.copy(chatRecord, ApplicationChatRecordVO.class);
        JSONObject details=chatRecord.getDetails();
        if(!details.isEmpty()){
            JSONObject searchStep=details.getJSONObject("search_step");
            if(searchStep!=null&&!searchStep.isEmpty()){
                JSONArray paragraphList=searchStep.getJSONArray("paragraph_list");
                if(!CollectionUtils.isEmpty(paragraphList)){
                    String json=JSONObject.toJSONString(paragraphList);
                    chatRecordVO.setParagraphList(JSON.parseArray(json, ParagraphVO.class));
                }
            }
            JSONObject problemPadding=details.getJSONObject("problem_padding");
            if(problemPadding!=null&&!problemPadding.isEmpty()){
                chatRecordVO.setPaddingProblemText(problemPadding.getString("padding_problem_text"));
            }
            List<JSONObject> executionDetails= new ArrayList<>();
            details.keySet().forEach(key->{
                executionDetails.add(details.getJSONObject(key));
            });
            Collections.reverse(executionDetails);
            chatRecordVO.setExecutionDetails(executionDetails);
        }
        return chatRecordVO;
    }

    public IPage<ApplicationChatRecordVO> chatRecordPage(String chatId, int page, int size) {
        Page<ApplicationChatRecordEntity> chatRecordpage = new Page<>(page, size);
        LambdaQueryWrapper<ApplicationChatRecordEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ApplicationChatRecordEntity::getChatId,chatId);
        IPage<ApplicationChatRecordEntity> chatRecordIpage=this.page(chatRecordpage, wrapper);
        return PageUtil.copy(chatRecordIpage, this::convert);
    }
}
