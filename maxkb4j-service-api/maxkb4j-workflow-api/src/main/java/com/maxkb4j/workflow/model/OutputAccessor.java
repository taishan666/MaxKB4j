package com.maxkb4j.workflow.model;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.dto.Answer;
import com.maxkb4j.common.domain.dto.ChatMessageVO;
import com.maxkb4j.workflow.node.AbsNode;

import java.util.List;

/**
 * 输出访问器
 * 提供输出管理相关方法，封装 WorkflowOutputManager 的直接访问
 *
 * 设计原则：
 * - 提供清晰的语义化方法名
 * - 封装输出发送逻辑
 * - 统一输出管理入口
 */
public class OutputAccessor {

    private final WorkflowOutputManager output;

    OutputAccessor(WorkflowOutputManager output) {
        this.output = output;
    }

    // ==================== 输出方法 ====================

    /**
     * 发送消息到 Sink
     *
     * @param message 聊天消息
     */
    public void emit(ChatMessageVO message) {
        output.emitMessage(message);
    }

    /**
     * 发送消息（便捷方法）
     *
     * @param content    消息内容
     * @param reasoning  推理内容
     * @param node       来源节点
     * @param workflow   工作流实例
     */
    public void emit(String content, String reasoning, AbsNode node, Workflow workflow) {
        if (output.needsSinkOutput() && node != null) {
            ChatMessageVO vo = node.toChatMessageVO(
                    workflow.getConfiguration().getChatParams().getChatId(),
                    workflow.getConfiguration().getChatParams().getChatRecordId(),
                    content,
                    reasoning,
                    null,
                    false
            );
            output.emitMessage(vo);
        }
    }

    // ==================== 读取方法 ====================

    /**
     * 获取答案文本列表
     *
     * @return 答案列表
     */
    public List<Answer> answers() {
        return output.getAnswerTextList();
    }

    /**
     * 获取运行时详情
     *
     * @return 节点运行时详情 JSON
     */
    public JSONObject runtimeDetails() {
        return output.getRuntimeDetails();
    }

    /**
     * 是否需要 Sink 输出
     * 知识库工作流不需要输出，聊天工作流需要输出
     *
     * @return 是否需要输出
     */
    public boolean needsSink() {
        return output.needsSinkOutput();
    }

    // ==================== 检查方法 ====================

    /**
     * 获取答案数量
     *
     * @return 答案数量
     */
    public int answerCount() {
        List<Answer> answers = output.getAnswerTextList();
        return answers != null ? answers.size() : 0;
    }

    /**
     * 是否有答案
     *
     * @return 是否有答案输出
     */
    public boolean hasAnswers() {
        List<Answer> answers = output.getAnswerTextList();
        return answers != null && !answers.isEmpty();
    }
}