package com.tarzan.maxkb4j.module.application.vo;

import lombok.Data;

import java.util.Date;

@Data
public class ChatRecordDetailVO {

    private String chatId;
    private String overview;
    private String voteStatus;
    private String problemText;
    private String answerText;
    private Integer cost;
    private Float runTime;
    private Integer index;
    private Date createTime;
}
