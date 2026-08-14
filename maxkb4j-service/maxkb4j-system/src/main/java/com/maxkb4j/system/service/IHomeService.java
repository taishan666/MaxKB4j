package com.maxkb4j.system.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.maxkb4j.system.dto.AgentStatDTO;
import com.maxkb4j.system.dto.ChatUserStatDTO;
import com.maxkb4j.system.dto.DailyStatDTO;
import com.maxkb4j.system.dto.HomeQuery;

import java.util.List;

public interface IHomeService {
    JSONObject aggregation(String type);
    List<DailyStatDTO> monitoring(HomeQuery query);
    int chatRecordCount(HomeQuery query);
    int tokensCount(HomeQuery query);
    IPage<AgentStatDTO> tokensRanking(int current, int size, HomeQuery query);
    IPage<AgentStatDTO> questionRanking(int current, int size, HomeQuery query);
    IPage<ChatUserStatDTO> userTokensRanking(int current, int size, HomeQuery query);
}
