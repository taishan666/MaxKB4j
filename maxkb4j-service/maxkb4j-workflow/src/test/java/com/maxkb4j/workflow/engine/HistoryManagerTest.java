package com.maxkb4j.workflow.engine;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.dto.ChatRecordDTO;
import com.maxkb4j.workflow.enums.DialogueType;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回归测试：历史消息管理（节点级 / 全局级消息装配、多模态问题解析、缺失节点空安全）。
 */
class HistoryManagerTest {

    @Test
    void constructor_nullHistory_initializesEmptyList() {
        HistoryManager manager = new HistoryManager(null);
        assertThat(manager.historyChatRecords()).isNotNull().isEmpty();
        assertThat(manager.getHistoryMessages(5, DialogueType.NODE.name(), "rt-1")).isEmpty();
    }

    @Test
    void getHistoryMessages_nodeType_buildsMessagesFromStringQuestion() {
        ChatRecordDTO record = recordWithNode("rt-1", "你好", "你好呀");
        HistoryManager manager = new HistoryManager(List.of(record));

        List<ChatMessage> messages = manager.getHistoryMessages(1, DialogueType.NODE.name(), "rt-1");

        assertThat(messages).hasSize(2);
        assertThat(((UserMessage) messages.get(0)).singleText()).isEqualTo("你好");
        assertThat(((AiMessage) messages.get(1)).text()).isEqualTo("你好呀");
    }

    @Test
    void getHistoryMessages_nodeType_buildsMultimodalFromListQuestion() {
        JSONArray question = new JSONArray();
        question.add(textNode("看图"));
        question.add(imageNode("http://example.com/a.png"));
        ChatRecordDTO record = recordWithNodeQuestion("rt-1", question, "收到");
        HistoryManager manager = new HistoryManager(List.of(record));

        List<ChatMessage> messages = manager.getHistoryMessages(1, DialogueType.NODE.name(), "rt-1");

        assertThat(messages).hasSize(2);
        List<Content> contents = ((UserMessage) messages.get(0)).contents();
        assertThat(contents).hasSize(2);
        assertThat(((TextContent) contents.get(0)).text()).isEqualTo("看图");
        assertThat(contents.get(1)).isInstanceOf(ImageContent.class);
        assertThat(((ImageContent) contents.get(1)).image().url().toString())
                .isEqualTo("http://example.com/a.png");
        assertThat(((AiMessage) messages.get(1)).text()).isEqualTo("收到");
    }

    @Test
    void getHistoryMessages_nodeType_skipsImageWithoutUrl() {
        JSONArray question = new JSONArray();
        question.add(textNode("hi"));
        JSONObject imageWithoutUrl = new JSONObject();
        imageWithoutUrl.put("type", "image_url");
        question.add(imageWithoutUrl);
        ChatRecordDTO record = recordWithNodeQuestion("rt-1", question, "ok");
        HistoryManager manager = new HistoryManager(List.of(record));

        List<ChatMessage> messages = manager.getHistoryMessages(1, DialogueType.NODE.name(), "rt-1");

        List<Content> contents = ((UserMessage) messages.get(0)).contents();
        // 缺少 url 的图片项被跳过，仅保留文本
        assertThat(contents).hasSize(1);
        assertThat(((TextContent) contents.get(0)).text()).isEqualTo("hi");
    }

    @Test
    void getHistoryMessages_nodeType_missingNodeReturnsEmpty() {
        ChatRecordDTO record = recordWithNode("rt-1", "q", "a");
        HistoryManager manager = new HistoryManager(List.of(record));

        assertThat(manager.getHistoryMessages(5, DialogueType.NODE.name(), "nope")).isEmpty();
    }

    @Test
    void getHistoryMessages_nodeType_absentQuestionStillRecordsAnswer() {
        JSONObject node = new JSONObject();
        node.put("answer", "只答");
        ChatRecordDTO record = wrapNode("rt-1", node);
        HistoryManager manager = new HistoryManager(List.of(record));

        List<ChatMessage> messages = manager.getHistoryMessages(5, DialogueType.NODE.name(), "rt-1");

        // question 缺失时不产生 UserMessage，但 answer 仍被记录
        assertThat(messages).hasSize(1);
        assertThat(((AiMessage) messages.get(0)).text()).isEqualTo("只答");
    }

    @Test
    void getHistoryMessages_workflowType_delegatesToGlobalHistory() {
        HistoryManager manager = new HistoryManager(List.of(
                record("q1", "a1"),
                record("q2", "a2")));

        List<ChatMessage> messages = manager.getHistoryMessages(
                1, DialogueType.WORK_FLOW.name(), "rt-1");

        assertThat(messages).hasSize(2);
        assertThat(((UserMessage) messages.get(0)).singleText()).isEqualTo("q2");
        assertThat(((AiMessage) messages.get(1)).text()).isEqualTo("a2");
    }

    private ChatRecordDTO recordWithNode(String runtimeNodeId, String question, String answer) {
        JSONObject node = new JSONObject();
        node.put("question", question);
        node.put("answer", answer);
        return wrapNode(runtimeNodeId, node);
    }

    private ChatRecordDTO recordWithNodeQuestion(String runtimeNodeId, JSONArray question, String answer) {
        JSONObject node = new JSONObject();
        node.put("question", question);
        node.put("answer", answer);
        return wrapNode(runtimeNodeId, node);
    }

    private ChatRecordDTO wrapNode(String runtimeNodeId, JSONObject node) {
        JSONObject details = new JSONObject();
        details.put(runtimeNodeId, node);
        ChatRecordDTO record = new ChatRecordDTO();
        record.setDetails(details);
        return record;
    }

    private ChatRecordDTO record(String problem, String answer) {
        ChatRecordDTO record = new ChatRecordDTO();
        record.setProblemText(problem);
        record.setAnswerText(answer);
        return record;
    }

    private JSONObject textNode(String text) {
        JSONObject node = new JSONObject();
        node.put("type", "text");
        node.put("text", text);
        return node;
    }

    private JSONObject imageNode(String url) {
        JSONObject node = new JSONObject();
        node.put("type", "image_url");
        node.put("url", url);
        return node;
    }
}