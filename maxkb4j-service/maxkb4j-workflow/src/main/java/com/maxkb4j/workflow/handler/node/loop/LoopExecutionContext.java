package com.maxkb4j.workflow.handler.node.loop;

import com.alibaba.fastjson.JSONObject;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 循环迭代执行上下文（包内可见）
 *
 * <p>承载单次循环节点执行的迭代游标、历次迭代详情与中断标记，
 * 仅在 {@link LoopIterationRunner} 与 {@link LoopMessageForwarder} 之间流转。</p>
 */
class LoopExecutionContext {

    int currentIndex;
    final List<JSONObject> loopDetails;
    final AtomicBoolean isInterrupted;

    LoopExecutionContext(int startIndex, List<JSONObject> existingDetails) {
        this.currentIndex = startIndex;
        this.loopDetails = existingDetails;
        this.isInterrupted = new AtomicBoolean(false);
    }

    /**
     * 恢复当前迭代的详情（用于子工作流节点状态恢复）
     */
    JSONObject getCurrentDetails() {
        if (loopDetails.size() > currentIndex) {
            return loopDetails.get(currentIndex);
        }
        return new JSONObject();
    }
}
