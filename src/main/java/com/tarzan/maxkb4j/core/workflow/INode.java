package com.tarzan.maxkb4j.core.workflow;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.enums.DialogueType;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.chat.ChatParams;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.input.PromptTemplate;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Data
public abstract class INode {
    private String id;
    private int status = 200;
    private String errMessage = "";
    private String type;
    private String viewType;
    private JSONObject properties;
    private ChatParams chatParams;
    private Map<String, Map<String,Object>> flowVariables;
    private Map<String, Object> promptVariables;
    protected Map<String, Object> context;
    protected JSONObject detail;
    private List<String> upNodeIdList;
    private String runtimeNodeId;
    private String answerText;
    private List<ApplicationChatRecordEntity> historyChatRecords;


    public INode(JSONObject properties) {
        this.context = new HashMap<>(10);
        this.detail = new JSONObject();
        this.upNodeIdList = new ArrayList<>();
        this.properties = properties;
        this.viewType = "many_view";
        this.answerText = "";
    }

    public JSONObject getNodeData() {
        if (Objects.nonNull(properties) && properties.containsKey("nodeData")) {
            return properties.getJSONObject("nodeData");
        }
        return new JSONObject();
    }

    public void setUpNodeIdList(List<String> upNodeIdList) {
        this.upNodeIdList = upNodeIdList;
        this.runtimeNodeId = generateRuntimeNodeId();
    }


    public abstract NodeResult execute() throws Exception;

    protected abstract void saveContext(JSONObject detail);

    public abstract JSONObject getRunDetail();


    private String generateRuntimeNodeId() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            String input = Arrays.toString(upNodeIdList.toArray()) + id;
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }


    public NodeResult run() throws Exception {
        long startTime = System.currentTimeMillis();
        NodeResult result = execute();
        float runTime = (System.currentTimeMillis() - startTime) / 1000F;
        detail.put("runTime", runTime);
        log.info("node:{}, runTime:{} s", type, runTime);
        return result;
    }

    public void setDetail(JSONObject detail) {
        this.detail = detail;
        saveContext(detail);
    }


    public JSONObject getRunDetail(int index) {
        detail.put("nodeId", id);
        detail.put("upNodeIdList", upNodeIdList);
        detail.put("runtimeNodeId", runtimeNodeId);
        detail.put("name", properties.getString("nodeName"));
        detail.put("index", index);
        detail.put("type", type);
        detail.put("status", status);
        detail.put("errMessage", errMessage);
        detail.putAll(getRunDetail());
        return detail;
    }



    public Object getReferenceField(String nodeId, String key) {
        Map<String, Object> nodeVariable = flowVariables.get(nodeId);
        return nodeVariable.get(key);
    }


    public JSONArray resetMessageList(List<ChatMessage> historyMessages) {
        if (CollectionUtils.isEmpty(historyMessages)) {
            return new JSONArray();
        }
        JSONArray newMessageList = new JSONArray();
        for (ChatMessage chatMessage : historyMessages) {
            JSONObject message = new JSONObject();
            if (chatMessage instanceof UserMessage userMessage) {
                message.put("role", "user");
                message.put("content", userMessage.singleText());
                newMessageList.add(message);
            }
            if (chatMessage instanceof AiMessage aiMessage) {
                message.put("role", "ai");
                message.put("content", aiMessage.text());
                newMessageList.add(message);
            }
        }
        if (newMessageList.size() % 2 != 0) {
            newMessageList.remove(newMessageList.size() - 1);
        }
        return newMessageList;
    }

    protected String format(List<ChatMessage> chatMemory) {
        return chatMemory.stream().map(this::format).filter(Objects::nonNull).collect(Collectors.joining("\n"));
    }

    protected String format(ChatMessage message) {
        if (message instanceof UserMessage userMessage) {
            return "User: " + userMessage.singleText();
        } else if (message instanceof AiMessage aiMessage) {
            return aiMessage.hasToolExecutionRequests() ? null : "AI: " + aiMessage.text();
        } else {
            return null;
        }
    }

    public String generatePrompt(String prompt) {
        if (StringUtils.isBlank(prompt)) {
            return "";
        }
        Set<String> extractVariables = extractVariables(prompt);
        if (!promptVariables.isEmpty()) {
            Map<String, Object> variables = new HashMap<>();
            for (String promptVariable : extractVariables) {
                variables.put(promptVariable, promptVariables.getOrDefault(promptVariable, "*"));
            }
            PromptTemplate promptTemplate = PromptTemplate.from(prompt);
            return promptTemplate.apply(variables).text();
        }
        return prompt;
    }



    public Set<String> extractVariables(String template) {
        Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(.+?)\\}\\}");
        Set<String> variables = new HashSet<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        while (matcher.find()) {
            variables.add(matcher.group(1));
        }
        return variables;
    }


    public List<ChatMessage> getHistoryMessages(int dialogueNumber, String dialogueType, String runtimeNodeId) {
        List<ChatMessage> historyMessages;
        if (DialogueType.NODE.name().equals(dialogueType)) {
            historyMessages = getNodeMessages(runtimeNodeId);
        } else {
            historyMessages = getWorkFlowMessages();
        }
        int total = historyMessages.size();
        if (total == 0) {
            return historyMessages;
        }
        int startIndex = Math.max(total - dialogueNumber * 2, 0);
        return historyMessages.subList(startIndex, total);
    }

    private List<ChatMessage> getWorkFlowMessages() {
        String regex = "<form_render>(.*?)</form_render>";
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        List<ChatMessage> messages = new ArrayList<>();
        for (ApplicationChatRecordEntity message : historyChatRecords) {
            String answerText = message.getAnswerText();
            Matcher matcher = pattern.matcher(answerText);
            if (!matcher.find()){
                messages.add(new UserMessage(message.getProblemText()));
                messages.add(new AiMessage(message.getAnswerText()));
            }
        }
        return messages;
    }

    private List<ChatMessage> getNodeMessages(String runtimeNodeId) {
        List<ChatMessage> messages = new ArrayList<>();
        for (ApplicationChatRecordEntity record : historyChatRecords) {
            // 获取节点详情
            JSONObject nodeDetails = record.getNodeDetailsByRuntimeNodeId(runtimeNodeId);
            // 如果节点详情为空，返回空列表
            if (nodeDetails != null) {
                messages.add(new UserMessage(nodeDetails.getString("question")));
                messages.add(new AiMessage(nodeDetails.getString("answer")));
            }
        }
        return messages;
    }


}




