package com.maxkb4j.workflow.model;

import lombok.Data;

/**
 * 节点结果封装
 * 携带执行状态、结果与异常信息，供异步执行链路传递
 */
@Data
public class NodeResultFuture {
    private Integer status;
    private NodeResult result;
    private Exception exception;

    /**
     * 构造器
     *
     * @param status    节点状态码（参考 {@link com.maxkb4j.workflow.enums.NodeStatus}）
     * @param result    节点结果，无结果时为 null
     * @param exception 执行异常，无异常时为 null
     */
    public NodeResultFuture(Integer status, NodeResult result, Exception exception) {
        this.status = status;
        this.result = result;
        this.exception = exception;
    }
}
