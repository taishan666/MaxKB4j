package com.maxkb4j.workflow.handler.node.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.dto.ChatMessageVO;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.dto.ChildNode;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.builder.NodeBuilder;
import com.maxkb4j.workflow.engine.graph.ChatLoopWorkflow;
import com.maxkb4j.workflow.engine.graph.ChatWorkflow;
import com.maxkb4j.workflow.engine.graph.KnowledgeLoopWorkflow;
import com.maxkb4j.workflow.engine.graph.KnowledgeWorkflow;
import com.maxkb4j.workflow.handler.node.AbsNodeHandler;
import com.maxkb4j.workflow.logic.LogicFlow;
import com.maxkb4j.workflow.model.IChatWorkflow;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.model.LoopParams;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.LoopNode;
import com.maxkb4j.workflow.service.IWorkFlowActuator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.maxkb4j.workflow.enums.NodeType.*;

/**
 * 循环节点处理器
 * 支持数组遍历、指定次数循环和无限循环三种模式
 */
@NodeHandlerType(LOOP)
@Component
@RequiredArgsConstructor
public class LoopNodeHandler extends AbsNodeHandler {

    // 循环类型常量
    private static final String LOOP_TYPE_ARRAY = "ARRAY";
    private static final String LOOP_TYPE_INFINITE = "LOOP";
    private static final int MAX_INFINITE_LOOP_COUNT = 1000;

    // Detail 键常量
    private static final String DETAIL_LOOP_DATA = "loop_node_data";
    private static final String DETAIL_LOOP_TYPE = "loop_type";
    private static final String DETAIL_NUMBER = "number";
    private static final String DETAIL_CURRENT_INDEX = "current_index";
    private static final String DETAIL_INTERRUPT_EXEC = "is_interrupt_exec";

    private final IWorkFlowActuator workFlowActuator;
    private final NodeBuilder nodeBuilder;

    @Override
    public NodeResult doExecute(IWorkflow workflow, AbsNode node) throws Exception {
        LoopNode.NodeParams nodeParams = parseParams(node, LoopNode.NodeParams.class);
        List<JSONObject> loopDetails = executeLoop(workflow, node, nodeParams);

        node.getDetail().put(DETAIL_LOOP_DATA, loopDetails);
        node.getDetail().put(DETAIL_LOOP_TYPE, nodeParams.getLoopType());
        node.getDetail().put(DETAIL_NUMBER, nodeParams.getNumber());

        return new NodeResult(workflow.getLoopContext(), true, this::getInterruptFlag);
    }

    /**
     * 根据循环类型执行循环逻辑
     */
    private List<JSONObject> executeLoop(IWorkflow workflow, AbsNode node, LoopNode.NodeParams params) {
        String loopType = params.getLoopType();
        if (LOOP_TYPE_ARRAY.equals(loopType)) {
            return executeArrayLoop(workflow, node, params.getArray(), params.getLoopBody());
        } else if (LOOP_TYPE_INFINITE.equals(loopType)) {
            return executeCountLoop(workflow, node, MAX_INFINITE_LOOP_COUNT, params.getLoopBody());
        } else {
            return executeCountLoop(workflow, node, params.getNumber(), params.getLoopBody());
        }
    }

    /**
     * 执行数组遍历循环
     */
    private List<JSONObject> executeArrayLoop(IWorkflow workflow, AbsNode node, List<String> arrayRef, JSONObject loopBody) {
        Object value = workflow.getReferenceField(arrayRef);
        if (value == null) {
            return new ArrayList<>();
        }
        List<Object> items = convertToList(value);
        return executeIterations(workflow, node, items, loopBody);
    }

    /**
     * 执行指定次数循环
     */
    private List<JSONObject> executeCountLoop(IWorkflow workflow, AbsNode node, Integer count, JSONObject loopBody) {
        int iterations = count != null ? count : 0;
        List<Object> items = createIndexList(iterations);
        return executeIterations(workflow, node, items, loopBody);
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
            return JSON.parseArray(trimmed, Object.class);
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

    /**
     * 执行循环迭代
     */
    private List<JSONObject> executeIterations(IWorkflow workflow, AbsNode node, List<Object> items, JSONObject loopBody) {
        LoopExecutionContext ctx = prepareLoopContext(node);
        while (ctx.currentIndex < items.size() && !ctx.shouldBreak.get()) {
            executeSingleIteration(workflow, node, items, loopBody, ctx);
            ctx.currentIndex++;
        }
        return ctx.loopDetails;
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
     * 执行单次循环迭代
     */
    private void executeSingleIteration(IWorkflow workflow, AbsNode node, List<Object> items, JSONObject loopBody, LoopExecutionContext ctx) {
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
        AtomicReference<ChildNode> childNodeRef = subscribeToSink(sink, loopParams, ctx, workflow, node);
        IWorkflow loopWorkflow = null;
        if (workflow instanceof ChatWorkflow chatWorkflow) {
            loopWorkflow = new ChatLoopWorkflow(chatWorkflow, nodes, logicFlow.getEdges(), loopParams, ctx.currentDetails, sink);
            workFlowActuator.execute(loopWorkflow);
            // 发送结束标记
            emitIterationEnd(workflow, node, childNodeRef);
            // 更新状态
            updateIterationState(node, loopWorkflow, ctx);
        }
        if (workflow instanceof KnowledgeWorkflow knowledgeWorkFlow) {
            loopWorkflow = new KnowledgeLoopWorkflow(knowledgeWorkFlow, nodes, logicFlow.getEdges(), loopParams);
            workFlowActuator.execute(loopWorkflow);
            // 更新状态
            updateIterationState(node, loopWorkflow, ctx);
        }
    }

    /**
     * 移除前一次迭代的数据
     */
    private void removePreviousIterationData(LoopExecutionContext ctx) {
        if (ctx.loopDetails.size() > ctx.currentIndex) {
            ctx.loopDetails.remove(ctx.currentIndex);
        }
    }

    /**
     * 订阅子工作流输出
     */
    private AtomicReference<ChildNode> subscribeToSink(Sinks.Many<ChatMessageVO> sink, LoopParams loopParams,
                                                       LoopExecutionContext ctx, IWorkflow workflow, AbsNode node) {
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
                                   AtomicReference<ChildNode> childNodeRef, IWorkflow workflow, AbsNode node) {
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
        emitLoopMessageVO(message, workflow, node, childNodeRef.get());

    }

    /**
     * 构建循环消息VO
     */
    private void emitLoopMessageVO(ChatMessageVO source, IWorkflow workflow,
                                   AbsNode node, ChildNode childNode) {
        if (workflow instanceof IChatWorkflow chatWorkflow) {
            ChatParams chatParams = chatWorkflow.getChatParams();
            ChatMessageVO vo = node.toChatMessageVO(
                    chatParams.getChatId(),
                    chatParams.getChatRecordId(),
                    source.getContent(),
                    source.getReasoningContent(),
                    childNode,
                    false);
            vo.setNodeType(source.getNodeType());
            vo.setViewType(source.getViewType());
            workflow.output().emit(vo);
        }

    }

    /**
     * 发送迭代结束标记
     */
    private void emitIterationEnd(IWorkflow workflow, AbsNode node, AtomicReference<ChildNode> childNodeRef) {
        if (workflow instanceof IChatWorkflow chatWorkflow) {
            ChatParams chatParams = chatWorkflow.getChatParams();
            ChatMessageVO vo = node.toChatMessageVO(
                    chatParams.getChatId(),
                    chatParams.getChatRecordId(),
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
    private void updateIterationState(AbsNode node, IWorkflow loopWorkflow, LoopExecutionContext ctx) {
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
}
