package com.maxkb4j.trigger.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.trigger.dto.EventTaskQuery;
import com.maxkb4j.trigger.entity.EventTriggerTaskRecordEntity;
import com.maxkb4j.trigger.mapper.EventTriggerTaskRecordMapper;
import com.maxkb4j.trigger.vo.EventTriggerTaskRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class EventTriggerTaskRecordService extends ServiceImpl<EventTriggerTaskRecordMapper, EventTriggerTaskRecordEntity> implements IEventTriggerTaskRecordService {

    @Override
    public IPage<EventTriggerTaskRecordVO> pageList(String id, int current, int size, EventTaskQuery query) {
        Page<EventTriggerTaskRecordVO> page = new Page<>(current, size);
        return this.baseMapper.pageListWithSourceName(page, id, query);
    }

    @Override
    public EventTriggerTaskRecordEntity get(String id, String taskId, String recordId) {
        return this.getById(recordId);
    }
}
