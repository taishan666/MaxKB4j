package com.tarzan.maxkb4j.core.workflow;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.model.ChatRecordSimple;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.domian.vo.ChatMessageVO;
import com.tarzan.maxkb4j.module.chat.ChatParams;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.input.PromptTemplate;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Sinks;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Data
public abstract class INode {
    public String id;
    public int status = 200;
    public String errMessage = "";
    public String type;
    public String viewType;
    public JSONObject properties;
    private ChatParams chatParams;
    public JSONObject globalVariable;
    public List<INode> upNodes;
    public JSONObject context;
    public List<String> upNodeIdList;
    public String runtimeNodeId;
    protected Sinks.Many<ChatMessageVO> sink;
    private List<ApplicationChatRecordEntity> historyChatRecords;


    public INode(JSONObject properties) {
        this.context = new JSONObject();
        this.upNodeIdList=new ArrayList<>();
        this.properties = properties;
        this.runtimeNodeId= generateRuntimeNodeId();
        this.viewType = "many_view";
    }

    public JSONObject getNodeData() {
        if (Objects.nonNull(properties) && properties.containsKey("nodeData")) {
            return properties.getJSONObject("nodeData");
        }
        return new JSONObject();
    }

    public void setUpNodeIdList(List<String> upNodeIdList) {
        this.upNodeIdList = upNodeIdList;
        this.runtimeNodeId= generateRuntimeNodeId();
    }


    public abstract NodeResult execute() throws Exception;

    public abstract JSONObject getDetail();

    private String generateRuntimeNodeId() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            assert upNodeIdList != null;
            String input = Arrays.toString(upNodeIdList.stream().sorted().toArray()) + id;
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


/*    public void getWriteErrorContext(Exception e) {
        this.status = 500;
        this.errMessage = e.getMessage();
        long startTime = (long) this.context.get("start_time");
        this.context.put("runTime", (System.currentTimeMillis() - startTime)/1000F);
    }*/

    public NodeResult run() throws Exception {
        long startTime = System.currentTimeMillis();
        this.context.put("start_time", startTime);
        NodeResult result = execute();
        float runTime = (System.currentTimeMillis() - startTime)/1000F;
        this.context.put("runTime", runTime);
        log.info("node:{}, runTime:{} s",type,runTime);
        return result;
    }



    public JSONObject getDetail(int index){
        JSONObject detail=new JSONObject();
        detail.put("name",properties.getString("nodeName"));
        detail.put("index",index);
        detail.put("type",type);
        detail.put("runTime",context.get("runTime"));
        detail.put("status",status);
        detail.put("err_message",errMessage);
        detail.putAll(getDetail());
        return detail;
    }


    public Object getReferenceField(String nodeId, String key) {
        if ("global".equals(nodeId)) {
            return globalVariable.get(key);
        } else {
            INode node = upNodes.stream().filter(e -> e.getId().equals(nodeId)).findAny().orElse(null);
            return node == null ? null : node.getReferenceField(key);
        }
    }

    protected Object getReferenceField(String key) {
        return context.get(key);
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
        if (newMessageList.size()%2!=0){
            newMessageList.remove(newMessageList.size()-1);
        }
        return newMessageList;
    }

    public UserMessage generatePromptQuestion(String prompt) {
        return UserMessage.from(this.generatePrompt(prompt));
    }

    public String generatePrompt(String prompt) {
        if (StringUtils.isBlank(prompt)) {
            return "";
        }
        Set<String> promptVariables = extractVariables(prompt);
        if (!promptVariables.isEmpty()) {
            Map<String, Object> allVariables = this.allVariables();
            Map<String, Object> variables = new HashMap<>();
            for (String promptVariable : promptVariables) {
                variables.put(promptVariable, allVariables.getOrDefault(promptVariable, "*"));
            }
            PromptTemplate promptTemplate = PromptTemplate.from(prompt);
            return promptTemplate.apply(variables).text();
        }
        return prompt;
    }

    public Map<String, Object> allVariables() {
        JSONObject workflowContext = new JSONObject();
        workflowContext.put("global", globalVariable);
        for (INode node : upNodes) {
            String nodeName=node.getProperties().getString("nodeName");
            workflowContext.put(nodeName, node.getContext());
        }
        return jsonToMap(workflowContext);
    }

    private void iteratorJson(String parentKey, JSONObject json, Map<String, Object> resultMap) {
        Set<String> keys = json.keySet();
        for (String key : keys) {
            Object value = json.get(key);
            // 生成新的key，如果parentKey不为空，则使用parentKey.key格式
            String newKey = (!parentKey.isEmpty()) ? (parentKey + "." + key) : key;
            if (value instanceof JSONObject) {
                // 如果是JSONObject，递归处理
                iteratorJson(newKey, (JSONObject) value, resultMap);
            } else {
                // 否则，直接放入结果map中
                resultMap.put(newKey, Objects.requireNonNullElse(value, ""));
            }
        }
    }

    public Map<String, Object> jsonToMap(JSONObject jsonObject) {
        Map<String, Object> resultMap = new HashMap<>();
        iteratorJson("", jsonObject, resultMap);
        return resultMap;
    }

    private static Set<String> extractVariables(String template) {
        Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(.+?)\\}\\}");
        Set<String> variables = new HashSet<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        while (matcher.find()) {
            variables.add(matcher.group(1));
        }
        return variables;
    }

    public List<ChatMessage> generateMessageList(String system, UserMessage question, List<ChatMessage> historyMessages) {
        List<ChatMessage> messageList = new ArrayList<>();
        if (StringUtils.isNotBlank(system)) {
            messageList.add(SystemMessage.from(system));
        }
        messageList.addAll(historyMessages);
        messageList.add(question);
        return messageList;
    }

    public List<ChatMessage> getHistoryMessages(int dialogueNumber, String dialogueType, String runtimeNodeId) {
        List<ChatMessage> historyMessages;
        if("NODE".equals(dialogueType)){
            historyMessages=getNodeMessages(runtimeNodeId);
        }else {
            historyMessages=getWorkFlowMessages();
        }
        int total=historyMessages.size();
        if (total==0){
            return historyMessages;
        }
        int startIndex = Math.max(total - dialogueNumber*2, 0);
        return historyMessages.subList(startIndex, total);
    }

    public List<ChatMessage> getWorkFlowMessages() {
        List<ChatMessage> messages = new ArrayList<>();
        for (ApplicationChatRecordEntity message : historyChatRecords) {
            messages.add(new UserMessage(message.getProblemText()));
            messages.add(new AiMessage(message.getAnswerText()));
        }
        return messages;
    }

    public List<ChatMessage> getNodeMessages(String runtimeNodeId) {
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

    //todo 获取历史聊天记录
    public String getHistoryContext() {
        // 获取历史聊天记录
        List<ChatRecordSimple> historyContext = new ArrayList<>();
        for (ApplicationChatRecordEntity chatRecord : historyChatRecords) {
            ChatRecordSimple record = new ChatRecordSimple();
            record.setQuestion(chatRecord.getProblemText());
            record.setAnswer(chatRecord.getAnswerText());
            historyContext.add(record);
        }
        return JSON.toJSONString(historyContext);
    }

}




