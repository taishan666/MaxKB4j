package com.maxkb4j.common.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.dto.ChatMessageVO;
import com.maxkb4j.common.domain.dto.ChatRecordDTO;
import com.maxkb4j.common.domain.dto.ChildNode;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回归测试：历史消息转换流程（成对裁剪、表单/工具标签清理、最近 N 轮截取）。
 */
class MessageConverterTest {

    @Test
    void formatHistoryMessages_emptyOrNull() {
        assertThat(MessageConverter.formatHistoryMessages(null).isEmpty()).isTrue();
        assertThat(MessageConverter.formatHistoryMessages(List.of()).isEmpty()).isTrue();
    }

    @Test
    void formatHistoryMessages_singleTextContentSerializedAsString() {
        JSONArray result = MessageConverter.formatHistoryMessages(List.of(
                UserMessage.from("你好"),
                AiMessage.from("你好呀")
        ));
        assertThat(result).hasSize(2);
        assertThat(result.getJSONObject(0).getString("role")).isEqualTo("user");
        assertThat(result.getJSONObject(0).getString("content")).isEqualTo("你好");
        assertThat(result.getJSONObject(1).getString("role")).isEqualTo("ai");
        assertThat(result.getJSONObject(1).getString("content")).isEqualTo("你好呀");
    }

    @Test
    void formatHistoryMessages_multimodalContentSerializedAsList() {
        JSONArray paired = MessageConverter.formatHistoryMessages(List.of(
                UserMessage.from(TextContent.from("看图"), ImageContent.from("AAAA", "image/png")),
                AiMessage.from("收到")
        ));
        assertThat(paired).hasSize(2);
        Object content = paired.getJSONObject(0).get("content");
        assertThat(content).isInstanceOf(List.class);
        @SuppressWarnings("unchecked")
        List<JSONObject> contents = (List<JSONObject>) content;
        assertThat(contents).hasSize(2);
        assertThat(contents.get(0).getString("type")).isEqualTo("text");
        assertThat(contents.get(0).getString("text")).isEqualTo("看图");
        assertThat(contents.get(1).getString("type")).isEqualTo("image_url");
        assertThat(contents.get(1).getJSONObject("image_url").getString("url"))
                .isEqualTo("data:image/png;base64,AAAA");
    }

    @Test
    void formatHistoryMessages_dropsTrailingUnpairedMessage() {
        JSONArray result = MessageConverter.formatHistoryMessages(List.of(
                UserMessage.from("q1"),
                AiMessage.from("a1"),
                UserMessage.from("q2")
        ));
        assertThat(result).hasSize(2);
        assertThat(result.getJSONObject(1).getString("role")).isEqualTo("ai");
    }

    @Test
    void toHistoryMessages_nullReturnsEmpty() {
        assertThat(MessageConverter.toHistoryMessages(null, 5)).isEmpty();
    }

    @Test
    void toHistoryMessages_skipsFormRenderAndStripsToolCallsRender() {
        ChatRecordDTO r1 = record("q1", "a1");
        ChatRecordDTO r2 = record("q2", "<form_render>ui</form_render>");
        ChatRecordDTO r3 = record("q3", "<tool_calls_render>tool</tool_calls_render>reply");
        ChatRecordDTO r4 = record("q4", null);

        List<ChatMessage> messages = MessageConverter.toHistoryMessages(List.of(r1, r2, r3, r4), 3);

        // r2 被跳过，r4 空答案转为 ""，最终 3 对 = 6 条
        assertThat(messages).hasSize(6);
        assertThat(((UserMessage) messages.get(0)).singleText()).isEqualTo("q1");
        assertThat(((AiMessage) messages.get(1)).text()).isEqualTo("a1");
        assertThat(((UserMessage) messages.get(2)).singleText()).isEqualTo("q3");
        assertThat(((AiMessage) messages.get(3)).text()).isEqualTo("reply");
        assertThat(((UserMessage) messages.get(4)).singleText()).isEqualTo("q4");
        assertThat(((AiMessage) messages.get(5)).text()).isEqualTo("");
    }

    @Test
    void toHistoryMessages_keepsOnlyLastRounds() {
        List<ChatRecordDTO> records = List.of(
                record("q1", "a1"),
                record("q2", "a2"),
                record("q3", "a3")
        );
        List<ChatMessage> messages = MessageConverter.toHistoryMessages(records, 1);
        assertThat(messages).hasSize(2);
        assertThat(((UserMessage) messages.get(0)).singleText()).isEqualTo("q3");
        assertThat(((AiMessage) messages.get(1)).text()).isEqualTo("a3");
    }

    @Test
    void lastRounds_handlesNullEmptyAndNonPositiveRounds() {
        assertThat(MessageConverter.lastRounds(null, 1)).isEmpty();
        assertThat(MessageConverter.lastRounds(List.of(), 1)).isEmpty();
        List<ChatMessage> msgs = List.of(
                UserMessage.from("u1"), AiMessage.from("a1"),
                UserMessage.from("u2"), AiMessage.from("a2"));
        assertThat(MessageConverter.lastRounds(msgs, 0)).isEmpty();
    }

    @Test
    void lastRounds_returnsLastRoundsCopyAndClampsOverflow() {
        List<ChatMessage> msgs = List.of(
                UserMessage.from("u1"), AiMessage.from("a1"),
                UserMessage.from("u2"), AiMessage.from("a2"));
        List<ChatMessage> one = MessageConverter.lastRounds(msgs, 1);
        assertThat(one).hasSize(2);
        assertThat(((UserMessage) one.get(0)).singleText()).isEqualTo("u2");

        List<ChatMessage> all = MessageConverter.lastRounds(msgs, 5);
        assertThat(all).hasSize(4);
        // 返回独立副本，修改不影响入参
        all.clear();
        assertThat(msgs).hasSize(4);
    }

    @Test
    void toChatMessageVO_populatesFieldsWithChildNode() {
        ChildNode child = new ChildNode("rec-child", "rt-child");
        ChatMessageVO vo = MessageConverter.toChatMessageVO(
                "chat-1", "rec-1", "node-1", "nodeName-1", "content", "reasoning",
                List.of("up-1"), "rt-1", "rt-real", "ai-chat", "many_view", child, true, false);
        assertThat(vo.getChatId()).isEqualTo("chat-1");
        assertThat(vo.getChatRecordId()).isEqualTo("rec-1");
        assertThat(vo.getNodeId()).isEqualTo("node-1");
        assertThat(vo.getNodeName()).isEqualTo("nodeName-1");
        assertThat(vo.getContent()).isEqualTo("content");
        assertThat(vo.getReasoningContent()).isEqualTo("reasoning");
        assertThat(vo.getUpNodeIdList()).containsExactly("up-1");
        assertThat(vo.getRuntimeNodeId()).isEqualTo("rt-1");
        assertThat(vo.getRealNodeId()).isEqualTo("rt-real");
        assertThat(vo.getNodeType()).isEqualTo("ai-chat");
        assertThat(vo.getViewType()).isEqualTo("many_view");
        assertThat(vo.getChildNode()).isSameAs(child);
        assertThat(vo.getNodeIsEnd()).isTrue();
        assertThat(vo.getIsEnd()).isFalse();
    }

    private ChatRecordDTO record(String problem, String answer) {
        ChatRecordDTO dto = new ChatRecordDTO();
        dto.setProblemText(problem);
        dto.setAnswerText(answer);
        return dto;
    }
}