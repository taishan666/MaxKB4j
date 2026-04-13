package com.maxkb4j.application.pipeline;


import com.alibaba.fastjson.JSONObject;

import java.util.Map;

public abstract class AbsStep {
    protected Map<String, Object> context = new JSONObject();

    public void run(PipelineManage manage) throws Exception{
        long startTime = System.currentTimeMillis();
        _run(manage);
        context.put("runTime", (System.currentTimeMillis()-startTime)/1000F);
    }

    protected abstract void _run(PipelineManage manage) throws Exception;


    public abstract JSONObject getDetails();
}
