package com.tarzan.maxkb4j.module.application.entity;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.common.entity.BaseEntity;
import com.tarzan.maxkb4j.handler.type.JOSNBArrayTypeHandler;
import com.tarzan.maxkb4j.handler.type.JOSNBTypeHandler;
import com.tarzan.maxkb4j.handler.type.StringArrayTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

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
	@JsonProperty("const")
	@TableField(value = "const")
	private Integer constant;
	@TableField(typeHandler = JOSNBTypeHandler.class)
	@JsonIgnore
	private JSONObject details;
	@TableField(typeHandler = StringArrayTypeHandler.class)
	private String[] improveParagraphIdList;
	private Float runTime;
	private Integer index;
	private String chatId;
	@TableField(typeHandler = JOSNBArrayTypeHandler.class)
	private List<String> answerTextList;

    public JSONObject getNodeDetailsByRuntimeNodeId(String runtimeNodeId) {
		return details.getJSONObject(runtimeNodeId);
    }
}
