package com.tarzan.maxkb4j.module.application.domian.entity;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tarzan.maxkb4j.core.common.entity.BaseEntity;
import com.tarzan.maxkb4j.core.handler.type.JOSNBTypeHandler;
import com.tarzan.maxkb4j.core.handler.type.StringListTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
  * @author tarzan
  * @date 2025-01-10 11:46:06
  */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "application_chat_record",autoResultMap = true)
public class ApplicationChatRecordEntity extends BaseEntity {
	private String voteStatus;
	private String problemText;
	private String answerText;
	private Integer messageTokens;
	private Integer answerTokens;
	private Integer cost;
	@TableField(typeHandler = JOSNBTypeHandler.class)
	@JsonIgnore
	private JSONObject details;
	@TableField(typeHandler = StringListTypeHandler.class)
	private List<String> improveParagraphIdList;
	private Float runTime;
	private Integer index;
	private String chatId;
	@TableField(typeHandler = StringListTypeHandler.class)
	private List<String> answerTextList;

    public JSONObject getNodeDetailsByRuntimeNodeId(String runtimeNodeId) {
		return details.getJSONObject(runtimeNodeId);
    }
}
