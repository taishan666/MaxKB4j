package com.maxkb4j.system.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.maxkb4j.common.util.DateTimeUtil;
import com.maxkb4j.core.support.permission.DataPermissionScope;
import com.maxkb4j.core.support.permission.DataPermissionSupport;
import com.maxkb4j.system.constant.AuthTargetType;
import com.maxkb4j.system.dto.AgentStatDTO;
import com.maxkb4j.system.dto.ChatUserStatDTO;
import com.maxkb4j.system.dto.DailyStatDTO;
import com.maxkb4j.system.dto.HomeQuery;
import com.maxkb4j.system.excel.QuestionRankingExcel;
import com.maxkb4j.system.excel.TokensRankingExcel;
import com.maxkb4j.system.excel.UserTokensRankingExcel;
import com.maxkb4j.system.mapper.HomeMapper;
import com.maxkb4j.system.service.IHomeService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
public class HomeServiceImpl implements IHomeService {

    /**
     * 问题数排行 Excel 文件名（同时用作 sheet 名，长度不超过 31）。
     */
    private static final String FILE_NAME = "问题数排行";
    /**
     * Token 数排行 Excel 文件名（同时用作 sheet 名，长度不超过 31）。
     */
    private static final String TOKENS_FILE_NAME = "Token数排行";
    /**
     * 用户 Token 数排行 Excel 文件名（同时用作 sheet 名，长度不超过 31）。
     */
    private static final String USER_TOKENS_FILE_NAME = "用户Token数排行";
    /**
     * Excel 下载响应的 Content-Type。
     */
    private static final String EXCEL_CONTENT_TYPE = "application/vnd.ms-excel";
    /**
     * Excel 下载文件名后缀。
     */
    private static final String EXCEL_FILE_SUFFIX = ".xlsx";
    /**
     * asker JSONB 中的用户名字段，与 {@code HomeMapper.xml} 中 {@code asker ->> 'username'} 对应。
     */
    private static final String ASKER_USERNAME_FIELD = "username";
    /**
     * 用户名缺失（未登录提问）时的展示占位符。
     */
    private static final String DEFAULT_USERNAME = "-";
    /**
     * 百分比保留一位小数，零值占位由同一格式推导，保证与格式化结果一致。
     */
    private static final String RATIO_PATTERN = "%.1f%%";
    /**
     * 均值保留一位小数，零值占位由同一格式推导，保证与格式化结果一致。
     */
    private static final String AVG_PATTERN = "%.1f";
    private final HomeMapper homeMapper;
    private final DataPermissionSupport dataPermissionSupport;

    /* ==================== 资源数量聚合 ==================== */

    /**
     * 按 type 分派到对应的聚合查询，返回 total / 各分类计数。
     *
     * <p>数据权限：admin 不附加过滤；普通用户仅统计其已授权（含自建）资源。
     *
     * @param type application / knowledge / tool / model
     */
    @Override
    public JSONObject aggregation(String type) {
        if (type == null) {
            return new JSONObject();
        }
        String authTargetType = switch (type) {
            case "application" -> AuthTargetType.APPLICATION;
            case "knowledge" -> AuthTargetType.KNOWLEDGE;
            case "tool" -> AuthTargetType.TOOL;
            case "model" -> AuthTargetType.MODEL;
            default -> null;
        };
        if (authTargetType == null) {
            return new JSONObject();
        }
        DataPermissionScope scope = dataPermissionSupport.resolve(authTargetType);
        boolean isAdmin = scope.isAdmin();
        List<String> targetIds = scope.getTargetIds();
        // authTargetType 与合法 type 一一对应，此处复用它分发；default 仅为防御
        Map<String, Object> data = switch (authTargetType) {
            case AuthTargetType.APPLICATION -> firstRow(homeMapper.applicationAggregation(isAdmin, targetIds));
            case AuthTargetType.KNOWLEDGE -> firstRow(homeMapper.knowledgeAggregation(isAdmin, targetIds));
            case AuthTargetType.TOOL -> firstRow(homeMapper.toolAggregation(isAdmin, targetIds));
            case AuthTargetType.MODEL -> firstRow(homeMapper.modelAggregation(isAdmin, targetIds));
            default -> Map.of();
        };
        return new JSONObject(data);
    }

    /**
     * 取单行聚合查询的首行结果，空结果返回空 Map。
     */
    private static Map<String, Object> firstRow(List<Map<String, Object>> rows) {
        return rows == null || rows.isEmpty() ? Map.of() : rows.getFirst();
    }

    /* ==================== 监控（按天） ==================== */

    /**
     * 全局监控：按天聚合 star/trample/tokens/chatRecord/customer，并合并每日新增客户数。
     * 缺失的日期以零值补齐，保证返回连续的按天序列。
     */
    @Override
    public List<DailyStatDTO> monitoring(HomeQuery query) {
        List<DailyStatDTO> result = new ArrayList<>();
        if (Objects.isNull(query.getStartTime()) || Objects.isNull(query.getEndTime())) {
            return result;
        }
        dataPermissionSupport.fill(query, AuthTargetType.APPLICATION);
        List<DailyStatDTO> dailyStats = homeMapper.dailyStats(query);
        List<DailyStatDTO> dailyNewCustomers = homeMapper.dailyNewCustomers(query);
        LocalDate startDate = DateTimeUtil.parseDate(query.getStartTime());
        LocalDate endDate = DateTimeUtil.parseDate(query.getEndTime());
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            String day = date.format(DateTimeUtil.DATE_FORMAT);
            DailyStatDTO dto = getDailyStat(dailyStats, day);
            dto.setCustomerAddedCount(getCustomerAddedCount(dailyNewCustomers, day));
            result.add(dto);
        }
        return result;
    }

    /**
     * 命中则返回当天的聚合记录，未命中则返回零填充的占位记录。
     */
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

    /**
     * 命中则返回当天的新增客户数，未命中则返回 0。
     */
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

    /**
     * 时间范围内的聊天记录总数（null -> 0）。
     */
    @Override
    public int chatRecordCount(HomeQuery query) {
        dataPermissionSupport.fill(query, AuthTargetType.APPLICATION);
        Integer count = homeMapper.chatRecordCount(query);
        return count == null ? 0 : count;
    }

    /**
     * 时间范围内的 Token 总数（null -> 0）。
     */
    @Override
    public int tokensCount(HomeQuery query) {
        dataPermissionSupport.fill(query, AuthTargetType.APPLICATION);
        Integer count = homeMapper.tokensCount(query);
        return count == null ? 0 : count;
    }

    /* ==================== 排行榜（分页） ==================== */

    /**
     * 应用 Token 排行（按 total_tokens 降序）。
     */
    @Override
    public IPage<AgentStatDTO> tokensRanking(int current, int size, HomeQuery query) {
        dataPermissionSupport.fill(query, AuthTargetType.APPLICATION);
        return homeMapper.tokensRanking(new Page<>(current, size), query);
    }

    /**
     * 应用 Token 排行全量数据导出为 Excel（按 total_tokens 降序）。
     */
    @Override
    public void exportTokensRanking(HomeQuery query, HttpServletResponse response) throws IOException {
        dataPermissionSupport.fill(query, AuthTargetType.APPLICATION);
        List<AgentStatDTO> records = homeMapper.tokensRankingExport(query);
        int tokensTotal = tokensCount(query);
        prepareExcelResponse(response, TOKENS_FILE_NAME);

        List<TokensRankingExcel> rows = new ArrayList<>(records.size());
        int rank = 1;
        for (AgentStatDTO dto : records) {
            TokensRankingExcel row = new TokensRankingExcel();
            row.setRank(rank++);
            row.setName(dto.getName());
            row.setTotalTokens(dto.getTotalTokens());
            row.setTokenRatio(formatRatio(dto.getTotalTokens(), tokensTotal));
            row.setChatRecordCount(dto.getChatRecordCount());
            row.setAvgTokensPerChat(formatAvg(dto.getTotalTokens(), dto.getChatRecordCount()));
            row.setChatUserCount(dto.getChatUserCount());
            rows.add(row);
        }
        EasyExcel.write(response.getOutputStream(), TokensRankingExcel.class)
                .sheet(TOKENS_FILE_NAME)
                .doWrite(rows);
    }

    /**
     * 应用问题数排行（按 chat_record_count 降序）。
     */
    @Override
    public IPage<AgentStatDTO> questionRanking(int current, int size, HomeQuery query) {
        dataPermissionSupport.fill(query, AuthTargetType.APPLICATION);
        return homeMapper.questionRanking(new Page<>(current, size), query);
    }

    /**
     * 应用问题数排行全量数据导出为 Excel（按 chat_record_count 降序）。
     */
    @Override
    public void exportQuestionRanking(HomeQuery query, HttpServletResponse response) throws IOException {
        dataPermissionSupport.fill(query, AuthTargetType.APPLICATION);
        List<AgentStatDTO> records = homeMapper.questionRankingExport(query);
        int tokensTotal = tokensCount(query);
        int chatRecordTotal = chatRecordCount(query);

        prepareExcelResponse(response, FILE_NAME);

        List<QuestionRankingExcel> rows = new ArrayList<>(records.size());
        int rank = 1;
        for (AgentStatDTO dto : records) {
            QuestionRankingExcel row = new QuestionRankingExcel();
            row.setRank(rank++);
            row.setName(dto.getName());
            row.setChatRecordCount(dto.getChatRecordCount());
            row.setChatRatio(formatRatio(dto.getChatRecordCount(), chatRecordTotal));
            row.setChatUserCount(dto.getChatUserCount());
            row.setAvgChatPerUser(formatAvg(dto.getChatRecordCount(), dto.getChatUserCount()));
            rows.add(row);
        }
        EasyExcel.write(response.getOutputStream(), QuestionRankingExcel.class)
                .sheet(FILE_NAME)
                .doWrite(rows);
    }

    /**
     * 统一设置 Excel 下载响应头：Content-Type、响应编码与 URL 编码后的附件文件名。
     */
    private static void prepareExcelResponse(HttpServletResponse response, String fileName) {
        response.setContentType(EXCEL_CONTENT_TYPE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
        response.setHeader("Content-disposition", "attachment;filename=" + encodedFileName + EXCEL_FILE_SUFFIX);
    }

    /**
     * 单个应用的指标值占该检索条件下对应总数的百分比，保留一位小数。
     */
    private String formatRatio(Integer value, int total) {
        if (value == null || value == 0 || total <= 0) {
            return String.format(Locale.ROOT, RATIO_PATTERN, 0.0);
        }
        double ratio = value * 100.0 / total;
        return String.format(Locale.ROOT, RATIO_PATTERN, ratio);
    }

    /**
     * 均值保留一位小数：numerator / denominator，denominator 为 0/空 或 numerator 为空时返回 0.0。
     */
    private String formatAvg(Integer numerator, Integer denominator) {
        if (numerator == null || denominator == null || denominator == 0) {
            return String.format(Locale.ROOT, AVG_PATTERN, 0.0);
        }
        double avg = numerator * 1.0 / denominator;
        return String.format(Locale.ROOT, AVG_PATTERN, avg);
    }

    /**
     * 用户 Token 排行（按 total_tokens 降序）。
     */
    @Override
    public IPage<ChatUserStatDTO> userTokensRanking(int current, int size, HomeQuery query) {
        dataPermissionSupport.fill(query, AuthTargetType.APPLICATION);
        return homeMapper.userTokensRanking(new Page<>(current, size), query);
    }

    /**
     * 用户 Token 排行全量数据导出为 Excel（按 total_tokens 降序）。
     */
    @Override
    public void exportUserTokensRanking(HomeQuery query, HttpServletResponse response) throws IOException {
        dataPermissionSupport.fill(query, AuthTargetType.APPLICATION);
        List<ChatUserStatDTO> records = homeMapper.userTokensRankingExport(query);
        int tokensTotal = tokensCount(query);

        prepareExcelResponse(response, USER_TOKENS_FILE_NAME);

        List<UserTokensRankingExcel> rows = new ArrayList<>(records.size());
        int rank = 1;
        for (ChatUserStatDTO dto : records) {
            UserTokensRankingExcel row = new UserTokensRankingExcel();
            row.setRank(rank++);
            JSONObject asker = dto.getAsker();
            String username = asker == null ? null : asker.getString(ASKER_USERNAME_FIELD);
            row.setUsername(username == null || username.isEmpty() ? DEFAULT_USERNAME : username);
            row.setTotalTokens(dto.getTotalTokens());
            row.setTokenRatio(formatRatio(dto.getTotalTokens(), tokensTotal));
            row.setChatRecordCount(dto.getChatRecordCount());
            row.setAvgTokensPerChat(formatAvg(dto.getTotalTokens(), dto.getChatRecordCount()));
            rows.add(row);
        }
        EasyExcel.write(response.getOutputStream(), UserTokensRankingExcel.class)
                .sheet(USER_TOKENS_FILE_NAME)
                .doWrite(rows);
    }
}
