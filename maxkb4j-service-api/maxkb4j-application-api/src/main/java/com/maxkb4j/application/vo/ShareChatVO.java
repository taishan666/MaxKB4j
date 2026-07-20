package com.maxkb4j.application.vo;

import lombok.Data;

import java.util.List;

@Data
public class ShareChatVO {

    private String summary;
    private List<ApplicationChatRecordVO> chatRecordList;
}
