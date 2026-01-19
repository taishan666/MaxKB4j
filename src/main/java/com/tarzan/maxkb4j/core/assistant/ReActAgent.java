package com.tarzan.maxkb4j.core.assistant;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public class ReActAgent {
    private final int maxIterations;
    private final ReActDecisionMaker decisionMaker;
    private boolean running;

    /**
     * 构造函数：初始化智能体的环境和参数
     */
    public ReActAgent(int maxIterations) {
        // 初始化语言模型
        // 需要设置环境变量
        ChatModel model = QwenChatModel.builder()
                .apiKey("sk-api-key") // 需要设置环境变量
                .modelName("qwen-plus")
                .build();
        // 初始化工具
        AgentTools tools = new AgentTools();
        // 初始化聊天记忆
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(20);
        this.decisionMaker = AiServices.builder(ReActDecisionMaker.class)
                .chatModel(model)
                .chatMemory(chatMemory)
                .tools(tools) // 复用AiServices的工具系统
                .build();
        // 设置最大迭代次数
        this.maxIterations = maxIterations;
        // 设置运行状态
        this.running = false;
        System.out.println("ReActAgent 已初始化完成");
    }

    /**
     * 决策制定：基于当前状态选择最优行动
     * 使用AiServices实现推理和决策，并复用其工具系统
     */
    public interface ReActDecisionMaker {
        @SystemMessage({
                "你是一个ReAct（Reasoning and Acting）智能体。",
                "你的任务是分析问题，决定是否需要使用工具，以及如何组合多个步骤来解决问题。",
                "你的回应必须严格按照以下格式：",
                "Thought: [在此处写下你的推理过程]",
                "Action: {\"name\": \"工具名称\", \"arguments\": {\"参数名\": \"参数值\"}}",
                "Observation: [工具执行结果会自动提供]",
                "如果需要继续使用工具，重复Thought->Action->Observation循环",
                "当你准备好提供最终答案时，输出:",
                "Final Answer: [你的最终答案]"
        })
        String decideNextAction(@UserMessage String input);
    }

    /**
     * 行动执行：将选定的行动应用于环境
     * 这里我们复用AiServices的工具执行机制
     *
     * @param input 当前输入
     * @return AI的决策和工具执行结果
     */
    public String executeStep(String input, int step) {
        System.out.println("\n--- 执行步骤-" + step + "---");
        System.out.println("输入: " + input);
        // 获取AI的决策
        String decision = decisionMaker.decideNextAction(input);
        System.out.println("输出: " + decision);
        return decision;
    }


    /**
     * 智能体主循环：持续感知、思考和行动
     *
     * @param initialInput 初始输入
     */
    public String runAgent(String initialInput) {
        System.out.println("\n>>> 启动 ReActAgent 主循环 <<<");
        running = true;
        int iteration = 0;
        while (running && iteration < maxIterations) {
            // 执行一步决策和工具调用
            String decision = executeStep(initialInput, (iteration + 1));
            // 将AI的回应添加到记忆中
            //    chatMemory.add(AiMessage.from(decision));
            // 检查是否包含最终答案
            if (decision.contains("Final Answer:")) {
                String finalAnswer = decision.substring(decision.indexOf("Final Answer:") + 13).trim();
                System.out.println("\n>>> 智能体任务完成 <<<");
                System.out.println("最终答案: " + finalAnswer);
                running = false;
                return finalAnswer;
            }

            // 检查是否包含行动
            if (decision.contains("Action:")) {
                System.out.println("行动已由AiServices自动执行");
            }
            iteration++;
        }
        if (iteration >= maxIterations) {
            System.out.println("\n>>> 达到最大迭代次数，任务结束 <<<");
        }
        running = false;
        return "达到最大迭代次数或未能生成最终答案";
    }


    /**
     * 主函数：创建ReActAgent实例并启动循环
     */
    public static void main(String[] args) {
        System.out.println("=== ReActAgent 演示程序 ===\n");

        // 创建ReActAgent实例，最大迭代次数设为5
         ReActAgent agent = new ReActAgent(5);

        // 测试用例1：简单的信息检索
        System.out.println("测试用例1: 查询法国首都");
        String result1 = agent.runAgent("法国的首都是什么？");
        System.out.println("结果1: " + result1);

        System.out.println("\n" + "=".repeat(50) + "\n");

        // 重新创建agent以避免记忆冲突
        ReActAgent agent2 = new ReActAgent(5);

        // 测试用例2：多步推理
        System.out.println("测试用例2: 多步计算");
        String result2 = agent2.runAgent("先计算15乘以4，然后加上20，结果是多少？");
        System.out.println("结果2: " + result2);

        System.out.println("\n" + "=".repeat(50) + "\n");

        // 重新创建agent以避免记忆冲突
        ReActAgent agent3 = new ReActAgent(5);

        // 测试用例3：知识检索+计算
        System.out.println("测试用例3: 综合任务");
        String result3 = agent3.runAgent("美国的人口是多少？中国的呢？请计算两国人口总数。");
        System.out.println("结果3: " + result3);

        System.out.println("\n=== 演示完成 ===");
    }
}
