package com.tarzan.maxkb4j.module.application.entity;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.handler.JOSNBArrayTypeHandler;
import com.tarzan.maxkb4j.handler.JOSNBTypeHandler;
import com.tarzan.maxkb4j.handler.UUIDArrayTypeHandler;
import com.tarzan.maxkb4j.handler.UUIDTypeHandler;
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
	@JsonProperty("vote_status")
	private String voteStatus;
	@JsonProperty("problem_text")
	private String problemText;
	@JsonProperty("answer_text")
	private String answerText;
	@JsonProperty("message_tokens")
	private Integer messageTokens;

	@JsonProperty("answer_tokens")
	private Integer answerTokens;
	@JsonProperty("const")
	@TableField(value = "const")
	private Integer constant;
	@TableField(typeHandler = JOSNBTypeHandler.class)
	@JsonIgnore
	private JSONObject details;

	@JsonProperty("improve_paragraph_id_list")
	@TableField(typeHandler = UUIDArrayTypeHandler.class)
	private UUID[] improveParagraphIdList;

	@JsonProperty("run_time")
	private Double runTime;
	
	private Integer index;

	@TableField(typeHandler = UUIDTypeHandler.class)
	private UUID chatId;

	@JsonProperty("answer_text_list")
	@TableField(typeHandler = JOSNBArrayTypeHandler.class)
	private List<String> answerTextList;
} 
