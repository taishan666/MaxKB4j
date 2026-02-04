package com.tarzan.maxkb4j.module.application.service;

import com.tarzan.maxkb4j.common.util.DateTimeUtil;
import com.tarzan.maxkb4j.module.application.domain.dto.ChatQueryDTO;
import com.tarzan.maxkb4j.module.application.domain.vo.ApplicationStatisticsVO;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationChatMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class ApplicationStatsService {

    private final ApplicationChatMapper applicationChatMapper;
    private final ApplicationChatUserStatsService chatUserStatsService;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public List<ApplicationStatisticsVO> applicationStats(String appId, ChatQueryDTO query) {
        List<ApplicationStatisticsVO> result = new ArrayList<>();
        if (Objects.isNull(query.getStartTime()) || Objects.isNull(query.getEndTime())) {
            return result;
        }
        List<ApplicationStatisticsVO> list = applicationChatMapper.statistics(appId, query);
        List<ApplicationStatisticsVO> accessClientList = chatUserStatsService.getCustomerCountTrend(appId, query);
        // 将字符串解析为LocalDate对象
        LocalDate startDate = DateTimeUtil.parseDate(query.getStartTime());
        LocalDate endDate = DateTimeUtil.parseDate(query.getEndTime());
        // 遍历从开始日期到结束日期之间的所有日期
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            String day = date.format(formatter);
            ApplicationStatisticsVO vo = getApplicationStatisticsVO(list, day);
            vo.setCustomerAddedCount(getCustomerAddedCount(accessClientList, day));
            result.add(vo);
        }
        return result;
    }

    public ApplicationStatisticsVO getApplicationStatisticsVO(List<ApplicationStatisticsVO> list, String day) {
        if (!CollectionUtils.isEmpty(list)) {
            Optional<ApplicationStatisticsVO> optional = list.stream().filter(e -> e.getDay().equals(day)).findFirst();
            if (optional.isPresent()) {
                return optional.get();
            }
        }
        ApplicationStatisticsVO vo = new ApplicationStatisticsVO();
        vo.setDay(day);
        vo.setStarNum(0);
        vo.setTokensNum(0);
        vo.setCustomerNum(0);
        vo.setChatRecordCount(0);
        vo.setTrampleNum(0);
        return vo;
    }

    public int getCustomerAddedCount(List<ApplicationStatisticsVO> list, String day) {
        if (!CollectionUtils.isEmpty(list)) {
            Optional<ApplicationStatisticsVO> optional = list.stream().filter(e -> e.getDay().equals(day)).findFirst();
            if (optional.isPresent()) {
                return optional.get().getCustomerAddedCount();
            }
        }
        return 0;
    }

    public List<ApplicationStatisticsVO> getTokenUsage(String appId, ChatQueryDTO query) {
        List<ApplicationStatisticsVO> result = new ArrayList<>();
        if (Objects.isNull(query.getStartTime()) || Objects.isNull(query.getEndTime())) {
            return result;
        }
        // 按用户维度统计 token 消耗
        List<ApplicationStatisticsVO> userTokenList = applicationChatMapper.userTokenUsage(appId, query);
        for (ApplicationStatisticsVO asv : userTokenList) {
            ApplicationStatisticsVO vo = new ApplicationStatisticsVO();
            vo.setUserName(asv.getUserName());
            vo.setTokenUsage(asv.getTokenUsage());
            result.add(vo);
        }
        return result;
    }

    public List<ApplicationStatisticsVO> topQuestions(String appId, ChatQueryDTO query) {
        List<ApplicationStatisticsVO> result = new ArrayList<>();
        if (Objects.isNull(query.getStartTime()) || Objects.isNull(query.getEndTime())) {
            return result;
        }
        List<ApplicationStatisticsVO> userTokenList = applicationChatMapper.topQuestions(appId, query);
        for (ApplicationStatisticsVO asv : userTokenList) {
            ApplicationStatisticsVO vo = new ApplicationStatisticsVO();
            vo.setUserName(asv.getUserName());
            vo.setChatRecordCount(asv.getChatRecordCount());
            result.add(vo);
        }
        return result;
    }
}
