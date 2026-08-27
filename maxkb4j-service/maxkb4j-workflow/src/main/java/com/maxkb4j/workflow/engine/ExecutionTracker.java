package com.maxkb4j.workflow.engine;

import com.maxkb4j.workflow.node.AbsNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 执行追踪器
 * <p>
 * 记录已执行节点的 runtimeNodeId 顺序与开始执行时间戳，独立于执行控制逻辑。
 * </p>
 */
@Getter
@Slf4j
public class ExecutionTracker {

    /**
     * 执行路径记录
     * 记录已执行节点的 runtimeNodeId 顺序
     * -- GETTER --
     *  获取执行路径记录
     *
     * @return 已执行节点的 runtimeNodeId 顺序

     */
    private final List<String> executionPath;

    /**
     * 执行时间戳记录
     * Key: runtimeNodeId, Value: 执行开始时间戳（毫秒）
     * -- GETTER --
     *  获取执行时间戳记录
     *
     * @return runtimeNodeId 到执行开始时间戳的映射

     */
    private final Map<String, Long> executionTimestamps;

    public ExecutionTracker() {
        this.executionPath = new ArrayList<>();
        this.executionTimestamps = new LinkedHashMap<>();
    }

    /**
     * 记录节点执行
     *
     * @param node 正在执行的节点
     */
    public void recordExecution(AbsNode node) {
        if (node == null || node.getRuntimeNodeId() == null) {
            return;
        }
        String runtimeNodeId = node.getRuntimeNodeId();
        executionPath.add(runtimeNodeId);
        executionTimestamps.put(runtimeNodeId, System.currentTimeMillis());
        log.debug("Recorded execution: {} at {}", runtimeNodeId, System.currentTimeMillis());
    }

}
