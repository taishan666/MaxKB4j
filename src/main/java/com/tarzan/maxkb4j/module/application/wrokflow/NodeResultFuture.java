package com.tarzan.maxkb4j.module.application.wrokflow;

import lombok.Data;

@Data
public class NodeResultFuture {
    private Integer status;
    private NodeResult result;
    private Exception exception;

    public NodeResultFuture(NodeResult result, Exception exception,Integer status) {
        this.result = result;
        this.exception = exception;
        this.status = status;
    }
}
