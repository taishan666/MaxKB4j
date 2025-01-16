package com.tarzan.maxkb4j.module.chatpipeline.step.generatehumanmessagestep.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.chatpipeline.PipelineManage;
import com.tarzan.maxkb4j.module.chatpipeline.step.generatehumanmessagestep.IGenerateHumanMessageStep;
import com.tarzan.maxkb4j.module.dataset.vo.ParagraphVO;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

public class GenerateHumanMessageStep extends IGenerateHumanMessageStep {

    @Override
    protected List<ChatMessage> execute(PipelineManage manage) {
        List<ChatMessage> messages = new ArrayList<>();
        JSONObject context = manage.context;
        List<ParagraphVO> paragraphList = (List<ParagraphVO>) context.get("paragraph_list");
        List<ApplicationChatRecordEntity> chatRecordList = (List<ApplicationChatRecordEntity>) context.get("chatRecordList");
        ApplicationEntity application = (ApplicationEntity) context.get("application");
        JSONObject modelSetting = application.getModelSetting();
        String system = modelSetting.getString("system");
        String prompt = modelSetting.getString("prompt");
        String problemText = context.getString("problem_text");
        String paddingProblemText=context.getString("padding_problem_text");
        String execProblemText = StringUtils.isNotBlank(paddingProblemText)?paddingProblemText:problemText;
        JSONObject datasetSetting = application.getDatasetSetting();
        int maxParagraphCharNumber = datasetSetting.getInteger("max_paragraph_char_number");
        JSONObject noReferencesSetting = datasetSetting.getJSONObject("no_references_setting");
        int dialogueNumber = application.getDialogueNumber();

        if (StringUtils.isNotBlank(system)) {
            messages.add(SystemMessage.from(system));
        }
        if (!CollectionUtils.isEmpty(chatRecordList)) {
            int startIndex = chatRecordList.size() - dialogueNumber;
            if (startIndex > 0) {
                chatRecordList = chatRecordList.subList(startIndex, chatRecordList.size());
            }
            for (ApplicationChatRecordEntity chatRecord : chatRecordList) {
                messages.add(UserMessage.from(chatRecord.getProblemText()));
                messages.add(AiMessage.from(chatRecord.getAnswerText()));
            }
        }
        messages.add(toHumanMessage(prompt, execProblemText, paragraphList, maxParagraphCharNumber, noReferencesSetting));
        return messages;
    }

    public ChatMessage toHumanMessage(String prompt, String problem, List<ParagraphVO> paragraphList, int maxParagraphCharNumber, JSONObject noReferencesSetting) {
        if (CollectionUtils.isEmpty(paragraphList)) {
            if ("ai_questioning".equals(noReferencesSetting.getString("status"))) {
                String value = noReferencesSetting.getString("value");
                return UserMessage.from(value.replace("{question}", problem));
            } else {
                return UserMessage.from(prompt.replace("{data}", "").replace("{question}", problem));
            }
        } else {
            StringBuilder data = new StringBuilder("\n");
            StringBuilder temp = new StringBuilder();
            for (ParagraphVO p : paragraphList) {
                String content = p.getTitle() + ":" + p.getContent();
                temp.append(content);
                if (temp.length() > maxParagraphCharNumber) {
                    String rowData = content.substring(0, temp.length() - maxParagraphCharNumber);
                    data.append("<data>").append(rowData).append("</data>");
                    break;
                } else {
                    data.append("<data>").append(content).append("</data>");
                }
            }
            return UserMessage.from(prompt.replace("{data}", data).replace("{question}", problem));
        }

    }


    @Override
    public JSONObject getDetails() {
        return null;
    }
}