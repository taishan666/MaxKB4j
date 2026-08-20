package com.maxkb4j.workflow.handler.node.loop;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.builder.NodeBuilder;
import com.maxkb4j.workflow.logic.LogicFlow;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.model.LoopParams;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.LoopNode;
import com.maxkb4j.workflow.service.IWorkFlowActuator;
import com.maxkb4j.workflow.service.WorkflowFactory;
import com.maxkb4j.workflow.service.WorkflowSpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 循环迭代执行器
 *
 * <p>从循环节点处理器中拆出的迭代控制职责：
 * <ul>
 *   <li>循环类型分发（数组遍历 / 指定次数 / 无限循环）</li>
 *   <li>迭代游标推进、中断短路、迭代详情收集与索引标注</li>
 *   <li>子工作流构建统一走 {@link WorkflowFactory}（Loop 规格），
 *       执行统一走 {@link IWorkFlowActuator}，自身不感知 Chat/Knowledge 变体细节</li>
 * </ul>
 * chat 系与 knowledge 系的差异被收敛到两处：
 * 消息输出差异由 {@link LoopMessageForwarder} 吸收（订阅返回 empty 即无输出流），
 * 工作流变体差异由工厂吸收。</p>
 */
@Component
@RequiredArgsConstructor
public class LoopIterationRunner {

    // 循环类型常量
    private static final String LOOP_TYPE_ARRAY = "ARRAY";
    private static final String LOOP_TYPE_INFINITE = "LOOP";
    private static final int MAX_INFINITE_LOOP_COUNT = 1000;

    // Detail 键常量
    private static final String DETAIL_LOOP_DATA = "loop_node_data";
    private static final String DETAIL_CURRENT_INDEX = "current_index";
    private static final String DETAIL_INTERRUPT_EXEC = "is_interrupt_exec";

    private final IWorkFlowActuator workFlowActuator;
    private final NodeBuilder nodeBuilder;
    private final WorkflowFactory workflowFactory;
    private final LoopMessageForwarder messageForwarder;

    /**
     * 执行循环并返回历次迭代详情
     *
     * @param workflow 主工作流
     * @param node     循环节点
     * @param params   循环节点参数
     * @return 各次迭代的运行时详情列表
     */
    public List<JSONObject> run(IWorkflow workflow, AbsNode node, LoopNode.NodeParams params) {
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
        if (value == null || "".equals(value)) {
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
        while (ctx.currentIndex < items.size() && !ctx.isInterrupted.get()) {
            executeSingleIteration(workflow, node, items, loopBody, ctx);
            ctx.currentIndex++;
        }
        return ctx.loopDetails;
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
     * 执行单次循环迭代：经工厂构建子工作流后交由执行器运行，
     * 输出订阅与迭代标记仅在 chat 系（存在订阅句柄）时发生。
     */
    private void executeSingleIteration(IWorkflow workflow, AbsNode node, List<Object> items,
                                        JSONObject loopBody, LoopExecutionContext ctx) {
        // 构建循环体子图
        LogicFlow logicFlow = LogicFlow.newInstance(loopBody);
        List<AbsNode> nodes = logicFlow.getNodes().stream()
                .map(nodeBuilder::getNode)
                .filter(Objects::nonNull)
                .toList();
        LoopParams loopParams = new LoopParams(ctx.currentIndex, items.get(ctx.currentIndex));

        // chat 系订阅子工作流输出（knowledge 系返回 empty，无输出流）
        Optional<LoopMessageForwarder.LoopSubscription> subscription =
                messageForwarder.subscribe(workflow, loopParams, ctx, node);

        // 统一经工厂构建循环子工作流，隔离 Chat/Knowledge 变体细节
        WorkflowSpec.Builder spec = WorkflowSpec.loop(workflow, nodes, logicFlow.getEdges(), loopParams)
                .details(ctx.getCurrentDetails());
        subscription.ifPresent(s -> spec.sink(s.getSink()));
        IWorkflow loopWorkflow = workflowFactory.create(spec.build());

        workFlowActuator.execute(loopWorkflow);

        // 发送单次结束标记
        subscription.ifPresent(s -> messageForwarder.emitIteration(workflow, node, s.getChildNodeRef().get(), false));
        // 更新状态
        updateIterationState(node, loopWorkflow, ctx);
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
        removePreviousIterationData(ctx);
        ctx.loopDetails.add(runtimeDetails);
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
