package com.tarzan.maxkb4j.module.application.domain.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.util.Date;

@Data
public class ChatRecordDetailVO {

    @ExcelProperty("会话ID")
    private String chatId;
    @ExcelProperty("概述")
    private String overview;
    @ExcelProperty("用户反馈")
    private String voteStatus;
    @ExcelProperty("用户问题")
    private String problemText;
    @ExcelProperty("AI回复")
    private String answerText;
    @ExcelProperty("消费TOKENS")
    private Integer cost;
    @ExcelProperty("运行时间")
    private Float runTime;
    @ExcelProperty("聊天序号")
    private Integer index;
    @ExcelProperty("提问时间")
    private Date createTime;
}
