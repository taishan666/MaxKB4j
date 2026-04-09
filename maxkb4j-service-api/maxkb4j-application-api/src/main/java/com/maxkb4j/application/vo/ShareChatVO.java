package com.maxkb4j.application.vo;

import com.maxkb4j.application.entity.ApplicationChatRecordEntity;
import lombok.Data;

import java.util.List;

@Data
public class ShareChatVO {

    private String summary;
    private List<ApplicationChatRecordEntity> chatRecordList;
}
