package com.maxkb4j.application.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.maxkb4j.application.dto.ChatQueryDTO;
import com.maxkb4j.application.entity.ApplicationChatEntity;
import com.maxkb4j.application.vo.ApplicationStatisticsVO;
import com.maxkb4j.application.vo.ChatRecordDetailVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-26 09:50:23
 */
@Mapper
public interface ApplicationChatMapper extends BaseMapper<ApplicationChatEntity> {

    IPage<ApplicationChatEntity> chatLogs(IPage<ApplicationChatEntity> page, @Param("appId") String appId, @Param("query") ChatQueryDTO query);

    List<ChatRecordDetailVO> chatRecordDetail(List<String> ids);

    List<ApplicationStatisticsVO> statistics(String appId, ChatQueryDTO query);

    List<ApplicationStatisticsVO> userTokenUsage(String appId, ChatQueryDTO query);

    List<ApplicationStatisticsVO> topQuestions(String appId, ChatQueryDTO query);

    /**
     * 原子 upsert 会话：不存在则插入（chat_record_count = 1），已存在则 chat_record_count 原子 +1。
     * <p>
     * 依赖主键 id 的唯一约束。并发创建会话不能用"先插、唯一键冲突再回退 UPDATE"的写法：
     * PostgreSQL 中语句失败会中止整个事务，同一事务内的回退 UPDATE 无法执行。
     * </p>
     *
     * @return 1 表示插入或更新了会话行
     */
    int insertOrIncrementRecordCount(ApplicationChatEntity entity);
}
