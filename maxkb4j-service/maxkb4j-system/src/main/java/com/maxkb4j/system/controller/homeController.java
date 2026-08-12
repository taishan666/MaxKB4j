package com.maxkb4j.system.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.maxkb4j.common.api.R;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.enums.ChatUserType;
import com.maxkb4j.system.dto.AgentStatDTO;
import com.maxkb4j.system.dto.ChatUserStatDTO;
import com.maxkb4j.system.dto.DailyStatDTO;
import com.maxkb4j.system.dto.HomeQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(AppConst.ADMIN_WORKSPACE_API)
@RequiredArgsConstructor
public class homeController {

    @GetMapping("/homepage/{type}/aggregation")
    public R<JSONObject> aggregation(@PathVariable String type) {
        if ("application".equals(type)){
            return R.data(new JSONObject(Map.of(
                    "total",16,
                    "publish_count", 6,
                    "un_publish_count", 10
            )));
        }else if ("knowledge".equals(type)){
            return R.data(new JSONObject(Map.of(
                    "total",16,
                    "document_count", 10,
                    "failure_count", 10
            )));
        }else if ("tool".equals(type)){
            return R.data(new JSONObject(Map.of(
                    "total",16,
                    "custom_count", 10,
                    "data_source_count", 10,
                    "mcp_count", 10,
                    "skill_count", 10,
                    "workflow_count", 10
            )));
        }else if ("model".equals(type)){
            return R.data(new JSONObject(Map.of(
                    "total",16,
                    "embedding_count", 10,
                    "llm_count", 10
            )));
        }
        return R.data(new JSONObject());
    }

    @GetMapping("/homepage/monitoring/aggregation")
    public R<List<DailyStatDTO>> monitoring(HomeQuery query) {
        return R.data(buildMockData());
    }

    @GetMapping("/homepage/chat_record/aggregation")
    public R<Integer> chatRecordAggregation(HomeQuery query) {
        return R.data(1);
    }

    @GetMapping("/homepage/tokens/aggregation")
    public R<Integer> tokensAggregation(HomeQuery query) {
        return R.data(742);
    }

    private static List<DailyStatDTO> buildMockData() {
        String[] days = {
                "2026-08-05", "2026-08-06", "2026-08-07", "2026-08-08",
                "2026-08-09", "2026-08-10", "2026-08-11", "2026-08-12"
        };

        List<DailyStatDTO> list = new ArrayList<>();
        for (String day : days) {
            DailyStatDTO dto = new DailyStatDTO();
            dto.setStarNum(0);
            dto.setTrampleNum(0);
            dto.setTokensNum(0);
            dto.setChatRecordCount(0);
            dto.setCustomerNum(0);
            dto.setDay(day);
            dto.setCustomerAddedCount(0);
            list.add(dto);
        }

        // 单独设置 2026-08-10 的特殊数据
        DailyStatDTO aug10 = list.get(5); // index=5 对应 2026-08-10
        aug10.setTokensNum(742);
        aug10.setChatRecordCount(1);
        aug10.setCustomerNum(1);
        aug10.setCustomerAddedCount(1);

        return list;
    }

    @GetMapping("/homepage/application/tokens_ranking/{current}/{size}")
    public R<IPage<AgentStatDTO>> tokensRanking(@PathVariable("current") int current, @PathVariable("size") int size,HomeQuery query) {
        Page<AgentStatDTO> page=new Page<>(current,size);
        page.setRecords(buildAgentMockData());
        return R.data(page);
    }

    @GetMapping("/homepage/application/question_ranking/{current}/{size}")
    public R<IPage<AgentStatDTO>> questionRanking(@PathVariable("current") int current, @PathVariable("size") int size,HomeQuery query) {
        Page<AgentStatDTO> page=new Page<>(current,size);
        page.setRecords(buildAgentMockData());
        return R.data(page);
    }

    @GetMapping("/homepage/application/user_tokens_ranking/{current}/{size}")
    public R<IPage<JSONObject>> userTokensRanking(@PathVariable("current") int current, @PathVariable("size") int size,HomeQuery query) {
        Page<JSONObject> page=new Page<>(current,size);
        ChatUserStatDTO dto = new ChatUserStatDTO();
        dto.setChatUserId(IdWorker.get32UUID());
        dto.setChatUsertype(ChatUserType.ANONYMOUS_USER.getKey());
        dto.setAsker(new JSONObject(Map.of("username","游客")));
        dto.setTotalTokens(742);
        dto.setChatRecordCount(1);
        return R.data(page);
    }


    private static List<AgentStatDTO> buildAgentMockData() {
        // 原始数据定义（name 直接使用中文，Fastjson 序列化时会自动处理 Unicode）
        String[][] rawData = {
                {"019daab0-8657-7dd0-ab18-682594b915bf", "简易智能体",   "742", "1", "1"},
                {"019e011b-5361-7a00-a5a1-6bd02ec25f53", "sqlbot",       "0",   "0", "0"},
                {"019e632a-63c1-7bb0-8fc4-6d96facb3388", "深度研究 Agent", "0",   "0", "0"},
                {"019ed345-ba5a-77c2-9215-4490e0a72115", "空白工作流",    "0",   "0", "0"},
                {"019db981-419c-7023-8d78-aec16783cb85", "知识库助手",    "0",   "0", "0"}
        };

        List<AgentStatDTO> list = new ArrayList<>();
        for (String[] row : rawData) {
            AgentStatDTO dto = new AgentStatDTO();
            dto.setId(row[0]);
            dto.setName(row[1]);
            dto.setTotalTokens(Integer.parseInt(row[2]));
            dto.setChatRecordCount(Integer.parseInt(row[3]));
            dto.setChatUserCount(Integer.parseInt(row[4]));
            list.add(dto);
        }
        return list;
    }
}
