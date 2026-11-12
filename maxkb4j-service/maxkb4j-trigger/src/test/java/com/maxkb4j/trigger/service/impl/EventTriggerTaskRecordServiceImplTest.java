package com.maxkb4j.trigger.service.impl;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.maxkb4j.trigger.entity.EventTriggerTaskRecordEntity;
import com.maxkb4j.trigger.mapper.EventTriggerTaskRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventTriggerTaskRecordServiceImplTest {

    @Mock
    private EventTriggerTaskRecordMapper baseMapper;
    @InjectMocks
    private EventTriggerTaskRecordServiceImpl service;

    @BeforeEach
    void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(service, "baseMapper", baseMapper);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void getMatchesTriggerAndTaskAsWellAsRecordId() {
        EventTriggerTaskRecordEntity record = new EventTriggerTaskRecordEntity();
        record.setId("record-id");
        when(baseMapper.selectOne(any())).thenReturn(record);

        EventTriggerTaskRecordEntity actual = service.get("trigger-id", "task-id", "record-id");

        assertThat(actual).isSameAs(record);
        ArgumentCaptor<AbstractWrapper> wrapperCaptor = ArgumentCaptor.forClass(AbstractWrapper.class);
        verify(baseMapper).selectOne(wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertThat(sqlSegment).contains("trigger_id", "trigger_task_id");
        assertThat(wrapperCaptor.getValue().getParamNameValuePairs().values())
                .contains("record-id", "trigger-id", "task-id");
    }
}
