package com.tarzan.maxkb4j.module.application.ragpipeline;

import com.alibaba.fastjson.JSONObject;

public abstract class IBaseChatPipelineStep {
    protected JSONObject context = new JSONObject();

   // public abstract void validArgs(PipelineManage manage);

    public void run(PipelineManage manage) {
        long startTime = System.currentTimeMillis();
        context.put("start_time", startTime);
      //  validArgs(manage);
        _run(manage);
        context.put("runTime", (System.currentTimeMillis()-startTime)/1000F);
    }

    protected abstract void _run(PipelineManage manage);


    public abstract JSONObject getDetails();
}
