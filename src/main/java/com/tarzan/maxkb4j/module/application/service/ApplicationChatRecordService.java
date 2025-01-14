package com.tarzan.maxkb4j.module.application.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.application.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationChatRecordMapper;
import com.tarzan.maxkb4j.module.application.vo.ApplicationChatRecordVO;
import com.tarzan.maxkb4j.module.dataset.vo.ParagraphVO;
import com.tarzan.maxkb4j.util.BeanUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author tarzan
 * @date 2025-01-10 11:46:06
 */
@Service
public class ApplicationChatRecordService extends ServiceImpl<ApplicationChatRecordMapper, ApplicationChatRecordEntity>{

    public ApplicationChatRecordVO getChatRecordInfo(UUID chatId,UUID chatRecordId) {
        ApplicationChatRecordEntity chatRecord = this.getById(chatRecordId);
        ApplicationChatRecordVO chatRecordVO = BeanUtil.copy(chatRecord, ApplicationChatRecordVO.class);
        JSONObject details=chatRecord.getDetails();
        List<ParagraphVO> paragraphList= new ArrayList<>();
        if(!details.isEmpty()){
            JSONObject searchStep=details.getJSONObject("search_step");
            if(!searchStep.isEmpty()){
                JSONArray array=searchStep.getJSONArray("paragraph_list");
                if(!CollectionUtils.isEmpty(array)){
                    paragraphList=array.toJavaList(ParagraphVO.class);
                }
            }
        }
        chatRecordVO.setParagraphList(paragraphList);
        return chatRecordVO;
    }
}
