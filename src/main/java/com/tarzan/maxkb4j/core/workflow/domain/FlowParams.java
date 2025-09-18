package com.tarzan.maxkb4j.core.workflow.domain;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class FlowParams {

/*    @NotEmpty(message = "历史对答不能为空")
    private List<ApplicationChatRecordEntity> historyChatRecord=new ArrayList<>();*/

    @NotBlank(message = "用户问题不能为空")
    private String question;

    @NotBlank(message = "对话id不能为空")
    private String chatId;

    @NotBlank(message = "对话记录id不能为空")
    private String chatRecordId;

    @NotNull(message = "流式输出不能为空")
    private Boolean stream;

    private JSONObject formData;

    private String clientId;

    private String clientType;

    @NotNull(message = "换个答案不能为空")
    private Boolean reChat;

}