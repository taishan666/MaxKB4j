package com.tarzan.maxkb4j.core.pipeline;


import com.alibaba.fastjson.JSONObject;

import java.util.Map;

public abstract class AbsStep {
    protected Map<String, Object> context = new JSONObject();

    public void run(PipelineManage manage) {
        long startTime = System.currentTimeMillis();
        _run(manage);
        context.put("runTime", (System.currentTimeMillis()-startTime)/1000F);
    }

    protected abstract void _run(PipelineManage manage);


    public abstract JSONObject getDetails();
}
