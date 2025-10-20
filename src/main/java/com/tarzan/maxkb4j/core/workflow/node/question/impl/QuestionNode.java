package com.tarzan.maxkb4j.core.workflow.node.question.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.common.util.SpringUtil;
import com.tarzan.maxkb4j.core.assistant.Assistant;
import com.tarzan.maxkb4j.core.langchain4j.AppChatMemory;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.enums.DialogueType;
import com.tarzan.maxkb4j.core.workflow.node.question.input.QuestionParams;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.module.model.info.service.ModelFactory;
import com.tarzan.maxkb4j.module.model.provider.service.impl.BaseChatModel;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;

import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.QUESTION;

public class QuestionNode extends INode {


    public QuestionNode(JSONObject properties) {
        super(properties);
        super.setType(QUESTION.getKey());
    }



    @Override
    public void saveContext(JSONObject detail) {
        context.put("answer", detail.get("answer"));
    }



}
