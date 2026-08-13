package com.maxkb4j.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.maxkb4j.common.api.R;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.constant.LoginType;
import com.maxkb4j.system.dto.AgentStatDTO;
import com.maxkb4j.system.dto.ChatUserStatDTO;
import com.maxkb4j.system.dto.DailyStatDTO;
import com.maxkb4j.system.dto.HomeQuery;
import com.maxkb4j.system.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 首页仪表盘统计接口。数据均来自共享 PostgreSQL 库的实时聚合查询。
 *
 * @author tarzan
 */
@RestController
@RequestMapping(AppConst.ADMIN_WORKSPACE_API)
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @SaCheckLogin(type = LoginType.ADMIN)
    @GetMapping("/homepage/{type}/aggregation")
    public R<JSONObject> aggregation(@PathVariable String type) {
        return R.data(homeService.aggregation(type));
    }

    @SaCheckLogin(type = LoginType.ADMIN)
    @GetMapping("/homepage/monitoring/aggregation")
    public R<List<DailyStatDTO>> monitoring(HomeQuery query) {
        return R.data(homeService.monitoring(query));
    }

    @SaCheckLogin(type = LoginType.ADMIN)
    @GetMapping("/homepage/chat_record/aggregation")
    public R<Integer> chatRecordAggregation(HomeQuery query) {
        return R.data(homeService.chatRecordCount(query));
    }

    @SaCheckLogin(type = LoginType.ADMIN)
    @GetMapping("/homepage/tokens/aggregation")
    public R<Integer> tokensAggregation(HomeQuery query) {
        return R.data(homeService.tokensCount(query));
    }

    @SaCheckLogin(type = LoginType.ADMIN)
    @GetMapping("/homepage/application/tokens_ranking/{current}/{size}")
    public R<IPage<AgentStatDTO>> tokensRanking(@PathVariable("current") int current,
                                                 @PathVariable("size") int size,
                                                 HomeQuery query) {
        return R.data(homeService.tokensRanking(current, size, query));
    }

    @SaCheckLogin(type = LoginType.ADMIN)
    @GetMapping("/homepage/application/question_ranking/{current}/{size}")
    public R<IPage<AgentStatDTO>> questionRanking(@PathVariable("current") int current,
                                                  @PathVariable("size") int size,
                                                  HomeQuery query) {
        return R.data(homeService.questionRanking(current, size, query));
    }

    @SaCheckLogin(type = LoginType.ADMIN)
    @GetMapping("/homepage/application/user_tokens_ranking/{current}/{size}")
    public R<IPage<ChatUserStatDTO>> userTokensRanking(@PathVariable("current") int current,
                                                        @PathVariable("size") int size,
                                                        HomeQuery query) {
        return R.data(homeService.userTokensRanking(current, size, query));
    }
}
