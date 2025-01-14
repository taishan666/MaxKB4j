package com.tarzan.maxkb4j.module.chatpipeline;

import com.alibaba.fastjson.JSONObject;

public abstract class IBaseChatPipelineStep {
    protected JSONObject context = new JSONObject();

   // public abstract void validArgs(PipelineManage manage);

    public Object run(PipelineManage manage) {
        long startTime = System.currentTimeMillis();
        context.put("start_time", startTime);
      //  validArgs(manage);
        return _run(manage);
       // context.put("run_time", (System.currentTimeMillis()-startTime)/1000F);
    }

    protected abstract Object _run(PipelineManage manage);

    public void execute() {}

    public abstract JSONObject getDetails();
}
