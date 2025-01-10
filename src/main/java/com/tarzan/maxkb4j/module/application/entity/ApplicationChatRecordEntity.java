package com.tarzan.maxkb4j.module.application.entity;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.module.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.UUID;

/**
  * @author tarzan
  * @date 2025-01-10 11:46:06
  */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("application_chat_record")
public class ApplicationChatRecordEntity extends BaseEntity {
	
	private String voteStatus;
	
	private String problemText;
	
	private String answerText;
	
	private Integer messageTokens;
	
	private Integer answerTokens;
	
	//private Integer const;
	
	private JSONObject details;
	
	private Long improveParagraphIdList;
	
	private Double runTime;
	
	private Integer index;
	
	private UUID chatId;
	
	private List<String> answerTextList;
} 
