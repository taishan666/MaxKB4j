package com.maxkb4j.system.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.maxkb4j.system.dto.AgentStatDTO;
import com.maxkb4j.system.dto.ChatUserStatDTO;
import com.maxkb4j.system.dto.DailyStatDTO;
import com.maxkb4j.system.dto.HomeQuery;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

public interface IHomeService {
    JSONObject aggregation(String type);
    List<DailyStatDTO> monitoring(HomeQuery query);
    int chatRecordCount(HomeQuery query);
    int tokensCount(HomeQuery query);
    IPage<AgentStatDTO> tokensRanking(int current, int size, HomeQuery query);

    /** 应用 Token 排行全量数据导出为 Excel。 */
    void exportTokensRanking(HomeQuery query, HttpServletResponse response) throws IOException;
    IPage<AgentStatDTO> questionRanking(int current, int size, HomeQuery query);

    /** 应用问题数排行全量数据导出为 Excel。 */
    void exportQuestionRanking(HomeQuery query, HttpServletResponse response) throws IOException;
    IPage<ChatUserStatDTO> userTokensRanking(int current, int size, HomeQuery query);

    /** 用户 Token 排行全量数据导出为 Excel。 */
    void exportUserTokensRanking(HomeQuery query, HttpServletResponse response) throws IOException;
}
