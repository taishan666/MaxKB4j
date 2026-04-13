package com.maxkb4j.application.pipeline;


import com.alibaba.fastjson.JSONObject;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public abstract class AbsStep {
    protected Map<String, Object> context = new JSONObject();

    public void run(PipelineManage manage) {
        long startTime = System.currentTimeMillis();
        _run(manage);
        context.put("runTime", (System.currentTimeMillis()-startTime)/1000F);
    }

    protected abstract void _run(PipelineManage manage) throws ExecutionException, InterruptedException, TimeoutException;


    public abstract JSONObject getDetails();
}
