package com.maxkb4j.system.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.maxkb4j.system.dto.AgentStatDTO;
import com.maxkb4j.system.dto.ChatUserStatDTO;
import com.maxkb4j.system.dto.DailyStatDTO;
import com.maxkb4j.system.dto.HomeQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 首页仪表盘统计 Mapper。
 *
 * <p>位于 {@code maxkb4j-system} 模块，直接以裸 SQL 查询共享 PostgreSQL 库中的
 * application / knowledge / document / tool / model / application_chat /
 * application_chat_record / application_chat_user_stats 等表，并将结果映射到
 * {@code maxkb4j-system} 自身的 DTO。与 {@link UserMapper} 查询 {@code application} 表
 * 的既有做法一致，不引入对兄弟 impl 模块的编译期依赖。
 *
 * @author tarzan
 */
@Mapper
public interface HomeMapper {

    /* ==================== 资源数量聚合 ==================== */

    /**
     * 应用统计：total / publish_count / un_publish_count。
     * <p>非 admin 用户按 {@code targetIds}（已授权应用，含自建）过滤；admin 不附加过滤。
     */
    Map<String, Object> applicationAggregation(@Param("isAdmin") boolean isAdmin,
                                              @Param("targetIds") List<String> targetIds);

    /**
     * 知识库统计：total / document_count / failure_count。
     * <p>非 admin 用户按 {@code targetIds}（已授权知识库，含自建）过滤 knowledge 与 document。
     */
    Map<String, Object> knowledgeAggregation(@Param("isAdmin") boolean isAdmin,
                                             @Param("targetIds") List<String> targetIds);

    /** 工具统计：total / custom_count / data_source_count / mcp_count / skill_count / workflow_count。 */
    Map<String, Object> toolAggregation(@Param("isAdmin") boolean isAdmin,
                                        @Param("targetIds") List<String> targetIds);

    /** 模型统计：total / embedding_count / llm_count。 */
    Map<String, Object> modelAggregation(@Param("isAdmin") boolean isAdmin,
                                         @Param("targetIds") List<String> targetIds);

    /* ==================== 监控（按天） ==================== */

    /** 全局按天聚合：star/trample/tokens/chatRecord/customer（跨所有应用）。 */
    List<DailyStatDTO> dailyStats(@Param("query") HomeQuery query);

    /** 全局每日新增客户数（沿用 application_chat_user_stats 语义）。 */
    List<DailyStatDTO> dailyNewCustomers(@Param("query") HomeQuery query);

    /* ==================== 总量 ==================== */

    /** 时间范围内的聊天记录总数。 */
    Integer chatRecordCount(@Param("query") HomeQuery query);

    /** 时间范围内的 Token 总数（message_tokens + answer_tokens）。 */
    Integer tokensCount(@Param("query") HomeQuery query);

    /* ==================== 排行榜（分页） ==================== */

    /** 应用 Token 排行（按 total_tokens 降序，分页）。 */
    IPage<AgentStatDTO> tokensRanking(Page<AgentStatDTO> page, @Param("query") HomeQuery query);

    /** 应用 Token 排行全量数据（导出 Excel 用，免分页，按 total_tokens 降序）。 */
    List<AgentStatDTO> tokensRankingExport(@Param("query") HomeQuery query);

    /** 应用问题数排行（按 chat_record_count 降序，分页）。 */
    IPage<AgentStatDTO> questionRanking(Page<AgentStatDTO> page, @Param("query") HomeQuery query);

    /** 应用问题数排行全量数据（导出 Excel 用，免分页，按 chat_record_count 降序）。 */
    List<AgentStatDTO> questionRankingExport(@Param("query") HomeQuery query);

    /** 用户 Token 排行（按 total_tokens 降序，分页）。 */
    IPage<ChatUserStatDTO> userTokensRanking(Page<ChatUserStatDTO> page, @Param("query") HomeQuery query);

    /** 用户 Token 排行全量数据（导出 Excel 用，免分页，按 total_tokens 降序）。 */
    List<ChatUserStatDTO> userTokensRankingExport(@Param("query") HomeQuery query);
}
