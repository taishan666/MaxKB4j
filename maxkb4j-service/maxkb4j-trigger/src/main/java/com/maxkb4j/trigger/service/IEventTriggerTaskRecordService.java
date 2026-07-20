package com.maxkb4j.trigger.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.trigger.dto.EventTaskQuery;
import com.maxkb4j.trigger.entity.EventTriggerTaskRecordEntity;
import com.maxkb4j.trigger.vo.EventTriggerTaskRecordVO;

public interface IEventTriggerTaskRecordService extends IService<EventTriggerTaskRecordEntity> {
    IPage<EventTriggerTaskRecordVO> pageList(String id, int current, int size, EventTaskQuery query);

    EventTriggerTaskRecordEntity get(String id, String taskId, String recordId);
}
