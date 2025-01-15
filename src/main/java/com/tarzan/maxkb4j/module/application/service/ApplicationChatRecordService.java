package com.tarzan.maxkb4j.module.application.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.application.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationChatRecordMapper;
import com.tarzan.maxkb4j.module.application.vo.ApplicationChatRecordVO;
import com.tarzan.maxkb4j.module.chatpipeline.ChatCache;
import com.tarzan.maxkb4j.module.chatpipeline.ChatInfo;
import com.tarzan.maxkb4j.module.dataset.vo.ParagraphVO;
import com.tarzan.maxkb4j.util.BeanUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * @author tarzan
 * @date 2025-01-10 11:46:06
 */
@Service
public class ApplicationChatRecordService extends ServiceImpl<ApplicationChatRecordMapper, ApplicationChatRecordEntity>{

    public ApplicationChatRecordVO getChatRecordInfo(UUID chatId,UUID chatRecordId) {
        ChatInfo chatInfo= ChatCache.get(chatId);
        ApplicationChatRecordEntity  chatRecord=chatInfo.getChatRecordList().stream().filter(e->e.getId().equals(chatRecordId)).findFirst().orElse(null);
        if(Objects.isNull(chatRecord)) {
            chatRecord = this.getById(chatRecordId);
        }
        ApplicationChatRecordVO chatRecordVO = BeanUtil.copy(chatRecord, ApplicationChatRecordVO.class);
        JSONObject details=chatRecord.getDetails();
        if(!details.isEmpty()){
            JSONObject searchStep=details.getJSONObject("search_step");
            if(!searchStep.isEmpty()){
                JSONArray paragraphList=searchStep.getJSONArray("paragraph_list");
                if(!CollectionUtils.isEmpty(paragraphList)){
                    String json=JSONObject.toJSONString(paragraphList);
                    chatRecordVO.setParagraphList(JSON.parseArray(json, ParagraphVO.class));
                }
            }
            JSONObject problemPadding=details.getJSONObject("problem_padding");
            if(!problemPadding.isEmpty()){
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
}
