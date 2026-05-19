package com.maxkb4j.workflow.handler.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.maxkb4j.common.domain.dto.ChatMessageVO;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.dto.ChildNode;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.builder.NodeBuilder;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbsNodeHandler;
import com.maxkb4j.workflow.logic.LogicFlow;
import com.maxkb4j.workflow.model.LoopParams;
import com.maxkb4j.workflow.model.LoopWorkFlow;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.LoopNode;
import com.maxkb4j.workflow.service.IWorkFlowActuator;
import lombok.RequiredArgsConstructor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.maxkb4j.workflow.enums.NodeType.*;

/**
 * 循环节点处理器
 * 支持数组遍历、指定次数循环和无限循环三种模式
 * 异步执行：通过递归 thenCompose 链式推进迭代，不阻塞线程
 */
@NodeHandlerType(NodeType.LOOP)
@Component
@RequiredArgsConstructor
public class LoopNodeHandler extends AbsNodeHandler {

    // 循环类型常量
    private static final String LOOP_TYPE_ARRAY = "ARRAY";
    private static final String LOOP_TYPE_INFINITE = "LOOP";
    private static final int MAX_INFINITE_LOOP_COUNT = 1000;

    // Detail 键常量
    private static final String DETAIL_LOOP_DATA = "loop_node_data";
    private static final String DETAIL_LOOP_TYPE = "loopType";
    private static final String DETAIL_NUMBER = "number";
    private static final String DETAIL_CURRENT_INDEX = "current_index";
    private static final String DETAIL_INTERRUPT_EXEC = "is_interrupt_exec";

    private final IWorkFlowActuator workFlowActuator;
    private final NodeBuilder nodeBuilder;
    private final TaskExecutor taskExecutor;

    @Override
    public boolean isAsync() {
        return true;
    }

    @Override
    protected NodeResult doExecute(Workflow workflow, AbsNode node) throws Exception {
        throw new UnsupportedOperationException("Loop node uses async execution via doExecuteAsync");
    }

    @Override
    protected CompletableFuture<NodeResult> doExecuteAsync(Workflow workflow, AbsNode node) throws Exception {
        LoopNode.NodeParams nodeParams = parseParams(node, LoopNode.NodeParams.class);

        // 根据循环类型准备迭代列表
        String loopType = nodeParams.getLoopType();
        List<Object> items;
        JSONObject loopBody;
        if (LOOP_TYPE_ARRAY.equals(loopType)) {
            Object value = workflow.getReferenceField(nodeParams.getArray());
            items = value != null ? convertToList(value) : new ArrayList<>();
            loopBody = nodeParams.getLoopBody();
        } else if (LOOP_TYPE_INFINITE.equals(loopType)) {
            items = createIndexList(MAX_INFINITE_LOOP_COUNT);
            loopBody = nodeParams.getLoopBody();
        } else {
            int iterations = nodeParams.getNumber() != null ? nodeParams.getNumber() : 0;
            items = createIndexList(iterations);
            loopBody = nodeParams.getLoopBody();
        }

        LoopExecutionContext ctx = prepareLoopContext(node);

        // 设置子节点的 runtimeNodeId
        ChatParams chatParams = workflow.getChatParams();
        if (chatParams.getChildNode() != null) {
            chatParams.setRuntimeNodeId(chatParams.getChildNode().getRuntimeNodeId());
        }

        return executeIterationsAsync(workflow, node, items, loopBody, ctx, chatParams, nodeParams);
    }

    public boolean isInterrupt(AbsNode node) {
        Object flag = node.getDetail().get(DETAIL_INTERRUPT_EXEC);
        return Boolean.TRUE.equals(flag);
    }

    /**
     * 递归链式执行迭代
     * 每次迭代完成后通过 thenCompose 推进下一次迭代
     * 收到中断信号时提前终止
     */
    private CompletableFuture<NodeResult> executeIterationsAsync(Workflow workflow, AbsNode node,
                                                                  List<Object> items, JSONObject loopBody,
                                                                  LoopExecutionContext ctx, ChatParams chatParams,
                                                                  LoopNode.NodeParams nodeParams) {
        if (ctx.currentIndex >= items.size() || ctx.shouldBreak.get()) {
            // 所有迭代完成或收到中断信号，构建最终结果
            node.getDetail().put(DETAIL_LOOP_DATA, ctx.loopDetails);
            node.getDetail().put(DETAIL_LOOP_TYPE, nodeParams.getLoopType());
            node.getDetail().put(DETAIL_NUMBER, nodeParams.getNumber());
            return CompletableFuture.completedFuture(
                    new NodeResult(workflow.getLoopContext(), true, this::isInterrupt));
        }

        // 执行当前迭代，完成后递归推进下一次
        return executeSingleIterationAsync(workflow, node, items, loopBody, ctx)
                .thenCompose(unused -> {
                    ctx.currentIndex++;
                    chatParams.setRuntimeNodeId(null);
                    return executeIterationsAsync(workflow, node, items, loopBody, ctx, chatParams, nodeParams);
                });
    }

    /**
     * 执行单次循环迭代（异步）
     * 不阻塞线程，通过 thenRun 链式处理迭代完成后的状态更新
     */
    private CompletableFuture<Void> executeSingleIterationAsync(Workflow workflow, AbsNode node, List<Object> items,
                                                                 JSONObject loopBody, LoopExecutionContext ctx) {
        // 清理前一次迭代数据
        removePreviousIterationData(ctx);

        // 构建子工作流
        Sinks.Many<ChatMessageVO> sink = Sinks.many().unicast().onBackpressureBuffer();
        LogicFlow logicFlow = LogicFlow.newInstance(loopBody);
        List<AbsNode> nodes = logicFlow.getNodes().stream()
                .map(nodeBuilder::getNode)
                .filter(Objects::nonNull)
                .toList();

        LoopParams loopParams = new LoopParams(ctx.currentIndex, items.get(ctx.currentIndex));
        LoopWorkFlow loopWorkflow = new LoopWorkFlow(workflow, nodes, logicFlow.getEdges(), loopParams, ctx.currentDetails, sink);

        // 异步执行子工作流
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> workFlowActuator.execute(loopWorkflow), taskExecutor);

        // 订阅输出并发送迭代标记（同步设置，在异步执行前完成）
        AtomicReference<ChildNode> childNodeRef = subscribeToSink(sink, loopParams, ctx, workflow, node);
        emitIterationEnd(workflow, node, childNodeRef);

        // 迭代完成后更新状态，不阻塞
        return future.thenRun(() -> updateIterationState(node, loopWorkflow, ctx));
    }

    /**
     * 循环执行上下文
     */
    private static class LoopExecutionContext {
        int currentIndex;
        final List<JSONObject> loopDetails;
        JSONObject currentDetails;
        final AtomicBoolean shouldBreak;
        final AtomicBoolean isInterrupted;

        LoopExecutionContext(int startIndex, List<JSONObject> existingDetails) {
            this.currentIndex = startIndex;
            this.loopDetails = existingDetails;
            this.currentDetails = new JSONObject();
            this.shouldBreak = new AtomicBoolean(false);
            this.isInterrupted = new AtomicBoolean(false);

            // 恢复之前迭代的详情
            if (loopDetails.size() > startIndex) {
                currentDetails = loopDetails.get(startIndex);
            }
        }
    }

    /**
     * 准备循环执行上下文
     */
    @SuppressWarnings("unchecked")
    private LoopExecutionContext prepareLoopContext(AbsNode node) {
        List<JSONObject> existingDetails = (List<JSONObject>) node.getDetail().get(DETAIL_LOOP_DATA);
        List<JSONObject> loopDetails = existingDetails != null ? existingDetails : new ArrayList<>();
        Object savedIndex = node.getDetail().get(DETAIL_CURRENT_INDEX);
        int startIndex = savedIndex != null ? (int) savedIndex : 0;
        return new LoopExecutionContext(startIndex, loopDetails);
    }

    /**
     * 移除前一次迭代的数据
     */
    private void removePreviousIterationData(LoopExecutionContext ctx) {
        if (ctx.loopDetails.size() > ctx.currentIndex) {
            ctx.loopDetails.removeFirst();
        }
    }

    /**
     * 订阅子工作流输出
     */
    private AtomicReference<ChildNode> subscribeToSink(Sinks.Many<ChatMessageVO> sink, LoopParams loopParams,
                                                        LoopExecutionContext ctx, Workflow workflow, AbsNode node) {
        AtomicReference<ChildNode> childNodeRef = new AtomicReference<>(null);

        sink.asFlux().subscribe(message -> {
            if (isBreakSignal(message)) {
                ctx.shouldBreak.set(true);
            } else {
                handleLoopMessage(message, loopParams, ctx, childNodeRef, workflow, node);
            }
        });

        return childNodeRef;
    }

    /**
     * 判断是否为中断信号
     */
    private boolean isBreakSignal(ChatMessageVO message) {
        return LOOP_BREAK.getKey().equals(message.getNodeType()) && "BREAK".equals(message.getContent());
    }

    /**
     * 处理循环消息
     */
    private void handleLoopMessage(ChatMessageVO message, LoopParams loopParams, LoopExecutionContext ctx,
                                    AtomicReference<ChildNode> childNodeRef, Workflow workflow, AbsNode node) {
        String nodeType = message.getNodeType();

        // 表单和用户选择节点需要中断
        if (FORM.getKey().equals(nodeType) || USER_SELECT.getKey().equals(nodeType)) {
            ctx.shouldBreak.set(true);
            ctx.isInterrupted.set(true);
        }

        // 更新子节点引用
        String runtimeNodeId = message.getRuntimeNodeId() + "_" + loopParams.getIndex();
        childNodeRef.set(new ChildNode(message.getChatRecordId(), runtimeNodeId));

        // 转发消息到主工作流
        if (workflow.output().needsSink()) {
            ChatMessageVO vo = buildLoopMessageVO(message, workflow, node, childNodeRef.get());
            workflow.output().emit(vo);
        }
    }

    /**
     * 构建循环消息VO
     */
    private ChatMessageVO buildLoopMessageVO(ChatMessageVO source, Workflow workflow,
                                              AbsNode node, ChildNode childNode) {
        ChatMessageVO vo = node.toChatMessageVO(
                workflow.getChatParams().getChatId(),
                workflow.getChatParams().getChatRecordId(),
                source.getContent(),
                source.getReasoningContent(),
                childNode,
                false);
        vo.setNodeType(source.getNodeType());
        vo.setViewType(source.getViewType());
        return vo;
    }

    /**
     * 发送迭代结束标记
     */
    private void emitIterationEnd(Workflow workflow, AbsNode node, AtomicReference<ChildNode> childNodeRef) {
        if (workflow.output().needsSink()) {
            ChatMessageVO vo = node.toChatMessageVO(
                    workflow.getChatParams().getChatId(),
                    workflow.getChatParams().getChatRecordId(),
                    "",
                    "",
                    childNodeRef.get(),
                    false);
            workflow.output().emit(vo);
        }
    }

    /**
     * 更新迭代状态
     */
    private void updateIterationState(AbsNode node, LoopWorkFlow loopWorkflow, LoopExecutionContext ctx) {
        node.getDetail().put(DETAIL_INTERRUPT_EXEC, ctx.isInterrupted.get());
        node.getDetail().put(DETAIL_CURRENT_INDEX, ctx.currentIndex);

        // 收集运行时详情
        JSONObject runtimeDetails = loopWorkflow.output().runtimeDetails();
        appendIterationIndex(runtimeDetails, ctx.currentIndex);
        ctx.loopDetails.add(runtimeDetails);
    }

    /**
     * 为运行时详情追加迭代索引
     */
    private void appendIterationIndex(JSONObject details, int index) {
        for (String key : details.keySet()) {
            JSONObject value = details.getJSONObject(key);
            if (value != null) {
                String runtimeNodeId = value.getString("runtimeNodeId");
                if (runtimeNodeId != null) {
                    value.put("runtimeNodeId", runtimeNodeId + "_" + index);
                }
            }
        }
    }

    /**
     * 将值转换为列表
     */
    @SuppressWarnings("unchecked")
    private List<Object> convertToList(Object value) {
        if (value instanceof List) {
            return (List<Object>) value;
        }
        if (value instanceof String) {
            return parseJsonArray((String) value);
        }
        return List.of(value);
    }

    /**
     * 解析 JSON 数组字符串
     */
    private List<Object> parseJsonArray(String jsonStr) {
        String trimmed = jsonStr.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            return new Gson().fromJson(trimmed, new TypeToken<List<Object>>() {}.getType());
        }
        return List.of(jsonStr);
    }

    /**
     * 创建索引列表用于次数循环
     */
    private List<Object> createIndexList(int count) {
        List<Object> indices = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            indices.add(i);
        }
        return indices;
    }
}