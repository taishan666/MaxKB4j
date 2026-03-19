package com.maxkb4j.trigger.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.trigger.dto.EventQuery;
import com.maxkb4j.trigger.dto.EventTriggerDTO;
import com.maxkb4j.trigger.entity.EventTriggerEntity;
import com.maxkb4j.trigger.vo.EventTriggerVO;
import com.maxkb4j.trigger.vo.SourceEventTriggerVO;

import java.util.List;

public interface IEventTriggerService extends IService<EventTriggerEntity> {
    IPage<EventTriggerVO> pageList(int current, int size, EventQuery query);

    void saveTrigger(EventTriggerDTO dto, Boolean isEdit);

    boolean batchActivate(String id, Boolean isActive);

    boolean deleteTrigger(String id);

    EventTriggerVO getDetailById(String id);

    List<EventTriggerEntity> listBySource(String sourceType, String sourceId);

    SourceEventTriggerVO getDetailBySourceId(String id,String sourceType,String sourceId);

    Boolean webhook(String triggerId, JSONObject params);
}
