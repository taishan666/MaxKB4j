package com.tarzan.maxkb4j.core.assistant;

import cn.hutool.core.date.DateUtil;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

public class SystemTools {

    @Tool("获取当前时间")
    public String getCurrentTime() {
        return DateUtil.now();
    }

    @Tool("用于执行数学表达式的工具，通过 js 的 expr-eval 库运行表达式并返回结果。")
    public String mathematicalExpression(@P("expression") String expression) {
        Expression engine = new ExpressionBuilder(expression)
                .build();
        double result = engine.evaluate();
        return "result is " + result;
    }

/*    @Tool("海报生成")
    public String posterGenerate(String title,String bodyText) {
        String taskId =createAsyncTask(title,bodyText);
        return waitAsyncTask(taskId);
    }

    *//**
     * 创建异步任务
     * @return taskId
     *//*
    public String createAsyncTask(String title,String bodyText) {
        ImageSynthesisParam param = ImageSynthesisParam.builder()
                .apiKey("")
                .model("wanx-poster-generation-v1")
                .prompt("灯笼，小猫，梅花")
                .extraInput("title", title)
                .extraInput("sub_title", "")
                .extraInput("body_text", bodyText)
                .extraInput("prompt_text_zh", "灯笼，小猫，梅花")
                .extraInput("wh_ratios", "竖版")
                .extraInput("lora_name", "童话油画")
                .extraInput("lora_weight", 0.8)
                .extraInput("ctrl_ratio", 0.7)
                .extraInput("ctrl_step", 0.7)
                .extraInput("generate_mode", "generate")
                .extraInput("generate_num", 1)
                .build();

        ImageSynthesis imageSynthesis = new ImageSynthesis();
        ImageSynthesisResult result = null;
        try {
            result = imageSynthesis.asyncCall(param);
        } catch (Exception e){
            throw new RuntimeException(e.getMessage());
        }
        String taskId = result.getOutput().getTaskId();
        System.out.println("taskId=" + taskId);
        return taskId;
    }

    *//**
     * 等待异步任务结束
     * @param taskId 任务id
     * *//*
    public String waitAsyncTask(String taskId) {
        while (true){
            HttpResponse response = HttpUtil.createGet("https://dashscope.aliyuncs.com/api/v1/tasks/" + taskId)
                    .bearerAuth("sk-3600f64265cc4c7f9c8bbc98bcc3b7cc").execute();
            JSONObject result=JSONObject.parseObject(response.body());
            JSONObject output=result.getJSONObject("output");
            System.out.println(output);
            String status=output.getString("task_status");
            if ("SUCCEEDED".equals(status)){
                return output.getJSONArray("render_urls").getString(0);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if ("FAILED".equals(status)){
                return output.toString();
            }
        }
    }*/



}
