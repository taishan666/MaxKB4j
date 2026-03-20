package com.maxkb4j.trigger.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.application.entity.ApplicationEntity;
import com.maxkb4j.application.service.IApplicationService;
import com.maxkb4j.common.constant.ResourceType;
import com.maxkb4j.common.util.PageUtil;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.tool.service.IToolService;
import com.maxkb4j.trigger.dto.EventTaskQuery;
import com.maxkb4j.trigger.entity.EventTriggerTaskRecordEntity;
import com.maxkb4j.trigger.mapper.EventTriggerTaskRecordMapper;
import com.maxkb4j.trigger.vo.EventTriggerTaskRecordVO;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class EventTriggerTaskRecordService extends ServiceImpl<EventTriggerTaskRecordMapper, EventTriggerTaskRecordEntity> implements IEventTriggerTaskRecordService {

    private final IApplicationService applicationService;
    private final IToolService toolService;
    @Override
    public IPage<EventTriggerTaskRecordVO> pageList(String id, int current, int size, EventTaskQuery query) {
        IPage<EventTriggerTaskRecordEntity> page = new Page<>(current, size);
        LambdaQueryWrapper<EventTriggerTaskRecordEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(EventTriggerTaskRecordEntity::getTriggerId, id);
        if (query != null) {
            if (StringUtils.isNotBlank(query.getSourceType())) {
                wrapper.like(EventTriggerTaskRecordEntity::getSourceType, query.getSourceType());
            }
            if (StringUtils.isNotBlank(query.getState())) {
                wrapper.like(EventTriggerTaskRecordEntity::getState, query.getState());
            }
        }
        wrapper.orderByDesc(EventTriggerTaskRecordEntity::getCreateTime);
        IPage<EventTriggerTaskRecordVO> pageList = PageUtil.copy(this.page(page, wrapper), EventTriggerTaskRecordVO.class);
        List<EventTriggerTaskRecordVO> records = pageList.getRecords();
        if (records.isEmpty()) {
            return pageList;
        }
        Map<String, String> sourceMap = new HashMap<>();
        List<String> appIds = records.stream().filter(record -> ResourceType.APPLICATION.equals(record.getSourceType())).map(EventTriggerTaskRecordVO::getSourceId).toList();
        if (!appIds.isEmpty()) {
            List<ApplicationEntity> apps = applicationService.lambdaQuery().select(ApplicationEntity::getId, ApplicationEntity::getName).in(ApplicationEntity::getId, appIds).list();
            sourceMap.putAll(apps.stream().collect(Collectors.toMap(ApplicationEntity::getId, ApplicationEntity::getName)));
        }
        List<String> toolIds = records.stream().filter(record -> ResourceType.TOOL.equals(record.getSourceType())).map(EventTriggerTaskRecordVO::getSourceId).toList();
        if (!toolIds.isEmpty()) {
            List<ToolEntity> tools = toolService.lambdaQuery().select(ToolEntity::getId, ToolEntity::getName).in(ToolEntity::getId, toolIds).list();
            sourceMap.putAll(tools.stream().collect(Collectors.toMap(ToolEntity::getId, ToolEntity::getName)));
        }
        records.forEach(record -> record.setSourceName(sourceMap.get(record.getSourceId())));
        return pageList;
    }

    @Override
    public EventTriggerTaskRecordEntity get(String id, String taskId, String recordId) {
        return this.getById(recordId);
    }
}
