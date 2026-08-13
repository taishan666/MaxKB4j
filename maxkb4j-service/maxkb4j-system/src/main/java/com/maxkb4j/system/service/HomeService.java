package com.maxkb4j.system.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.maxkb4j.common.util.DateTimeUtil;
import com.maxkb4j.system.dto.AgentStatDTO;
import com.maxkb4j.system.dto.ChatUserStatDTO;
import com.maxkb4j.system.dto.DailyStatDTO;
import com.maxkb4j.system.dto.HomeQuery;
import com.maxkb4j.system.mapper.HomeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 首页仪表盘统计服务。
 *
 * <p>聚合 {@link HomeMapper} 查询共享 PostgreSQL 库的结果，并补齐监控按天数据。
 * 监控逻辑与 {@code com.maxkb4j.application.service.ApplicationStatsService#applicationStats}
 * 同构：遍历时间范围内的每一天，命中则用查询值、未命中则零填充，再将每日新增客户数
 * 合并进同一天的记录。
 *
 * @author tarzan
 */
@RequiredArgsConstructor
@Service
public class HomeService {

    private final HomeMapper homeMapper;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /* ==================== 资源数量聚合 ==================== */

    /**
     * 按 type 分派到对应的聚合查询，返回 total / 各分类计数。
     *
     * @param type application / knowledge / tool / model
     */
    public JSONObject aggregation(String type) {
        if (type == null) {
            return new JSONObject();
        }
        Map<String, Object> data = switch (type) {
            case "application" -> homeMapper.applicationAggregation();
            case "knowledge"   -> homeMapper.knowledgeAggregation();
            case "tool"         -> homeMapper.toolAggregation();
            case "model"        -> homeMapper.modelAggregation();
            default             -> Map.of();
        };
        return data == null ? new JSONObject() : new JSONObject(data);
    }

    /* ==================== 监控（按天） ==================== */

    /**
     * 全局监控：按天聚合 star/trample/tokens/chatRecord/customer，并合并每日新增客户数。
     * 缺失的日期以零值补齐，保证返回连续的按天序列。
     */
    public List<DailyStatDTO> monitoring(HomeQuery query) {
        List<DailyStatDTO> result = new ArrayList<>();
        if (Objects.isNull(query.getStartTime()) || Objects.isNull(query.getEndTime())) {
            return result;
        }
        List<DailyStatDTO> dailyStats = homeMapper.dailyStats(query);
        List<DailyStatDTO> dailyNewCustomers = homeMapper.dailyNewCustomers(query);
        LocalDate startDate = DateTimeUtil.parseDate(query.getStartTime());
        LocalDate endDate = DateTimeUtil.parseDate(query.getEndTime());
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            String day = date.format(formatter);
            DailyStatDTO dto = getDailyStat(dailyStats, day);
            dto.setCustomerAddedCount(getCustomerAddedCount(dailyNewCustomers, day));
            result.add(dto);
        }
        return result;
    }

    /** 命中则返回当天的聚合记录，未命中则返回零填充的占位记录。 */
    private DailyStatDTO getDailyStat(List<DailyStatDTO> list, String day) {
        if (!CollectionUtils.isEmpty(list)) {
            Optional<DailyStatDTO> optional = list.stream().filter(e -> e.getDay().equals(day)).findFirst();
            if (optional.isPresent()) {
                return optional.get();
            }
        }
        DailyStatDTO dto = new DailyStatDTO();
        dto.setDay(day);
        dto.setStarNum(0);
        dto.setTokensNum(0);
        dto.setCustomerNum(0);
        dto.setChatRecordCount(0);
        dto.setTrampleNum(0);
        return dto;
    }

    /** 命中则返回当天的新增客户数，未命中则返回 0。 */
    private int getCustomerAddedCount(List<DailyStatDTO> list, String day) {
        if (!CollectionUtils.isEmpty(list)) {
            Optional<DailyStatDTO> optional = list.stream().filter(e -> e.getDay().equals(day)).findFirst();
            if (optional.isPresent()) {
                Integer count = optional.get().getCustomerAddedCount();
                return count == null ? 0 : count;
            }
        }
        return 0;
    }

    /* ==================== 总量 ==================== */

    /** 时间范围内的聊天记录总数（null -> 0）。 */
    public int chatRecordCount(HomeQuery query) {
        Integer count = homeMapper.chatRecordCount(query);
        return count == null ? 0 : count;
    }

    /** 时间范围内的 Token 总数（null -> 0）。 */
    public int tokensCount(HomeQuery query) {
        Integer count = homeMapper.tokensCount(query);
        return count == null ? 0 : count;
    }

    /* ==================== 排行榜（分页） ==================== */

    /** 应用 Token 排行（按 total_tokens 降序）。 */
    public IPage<AgentStatDTO> tokensRanking(int current, int size, HomeQuery query) {
        return homeMapper.tokensRanking(new Page<>(current, size), query);
    }

    /** 应用问题数排行（按 chat_record_count 降序）。 */
    public IPage<AgentStatDTO> questionRanking(int current, int size, HomeQuery query) {
        return homeMapper.questionRanking(new Page<>(current, size), query);
    }

    /** 用户 Token 排行（按 total_tokens 降序）。 */
    public IPage<ChatUserStatDTO> userTokensRanking(int current, int size, HomeQuery query) {
        return homeMapper.userTokensRanking(new Page<>(current, size), query);
    }
}
