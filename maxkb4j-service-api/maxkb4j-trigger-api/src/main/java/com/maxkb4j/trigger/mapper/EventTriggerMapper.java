package com.maxkb4j.trigger.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.maxkb4j.trigger.dto.EventQuery;
import com.maxkb4j.trigger.entity.EventTriggerEntity;
import com.maxkb4j.trigger.vo.EventTriggerVO;
import org.apache.ibatis.annotations.Param;

public interface EventTriggerMapper extends BaseMapper<EventTriggerEntity> {

    IPage<EventTriggerVO> selectEventTriggerPage(Page<EventTriggerVO> page, @Param("query") EventQuery query);
}
