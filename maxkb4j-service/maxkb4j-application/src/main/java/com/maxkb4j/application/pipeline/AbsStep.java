package com.maxkb4j.application.pipeline;


import com.alibaba.fastjson.JSONObject;

import java.util.Map;

public abstract class AbsStep {

    public void run(PipelineManage manage) throws Exception {
        long startTime = System.currentTimeMillis();
        _run(manage);
        manage.stepContext(this).put("runTime", (System.currentTimeMillis() - startTime) / 1000F);
    }

    protected abstract void _run(PipelineManage manage) throws Exception;

    /**
     * 获取本步骤在本次对话中的执行上下文。
     * <p>
     * Step 为 Spring 单例，实例字段会在并发会话间互相串扰（段落/消息/token 等被其他请求覆盖），
     * 因此步骤级状态一律保存在随请求生命周期隔离的 {@link PipelineManage} 上。
     * </p>
     */
    protected Map<String, Object> stepContext(PipelineManage manage) {
        return manage.stepContext(this);
    }

    /**
     * 输出本步骤的执行详情，数据从本次对话的 stepContext 中读取。
     */
    public abstract JSONObject getDetails(PipelineManage manage);
}
