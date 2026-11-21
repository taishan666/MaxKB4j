package com.maxkb4j.knowledge.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.knowledge.entity.KnowledgeActionEntity;
import com.maxkb4j.knowledge.mapper.KnowledgeActionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link KnowledgeActionServiceImpl} 状态更新测试：正常更新字段齐全；
 * 工作流异步回调期间记录被级联删除（selectById 为 null）时不抛 NPE、不落库。
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeActionServiceImplTest {

    private static final String ACTION_ID = "action-1";

    @Mock
    private KnowledgeActionMapper knowledgeActionMapper;

    @InjectMocks
    private KnowledgeActionServiceImpl knowledgeActionService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(knowledgeActionService, "baseMapper", knowledgeActionMapper);
    }

    @Test
    void updateState_persistsDetailsStateAndRunTime() {
        KnowledgeActionEntity entity = new KnowledgeActionEntity();
        entity.setId(ACTION_ID);
        entity.setCreateTime(new Date(System.currentTimeMillis() - 5000));
        when(knowledgeActionMapper.selectById(ACTION_ID)).thenReturn(entity);

        JSONObject details = new JSONObject();
        details.put("node", "ok");
        knowledgeActionService.updateState(ACTION_ID, details, "SUCCESS");

        assertSame(details, entity.getDetails());
        assertEquals("SUCCESS", entity.getState());
        // runTime = (now - createTime) / 1000f，5 秒前创建应落在 [4, 6] 秒附近
        assertEquals(5F, entity.getRunTime(), 1F);
        verify(knowledgeActionMapper).updateById(entity);
    }

    @Test
    void updateState_skipsSilentlyWhenRecordCascadeDeleted() {
        when(knowledgeActionMapper.selectById(ACTION_ID)).thenReturn(null);

        assertDoesNotThrow(() ->
                knowledgeActionService.updateState(ACTION_ID, new JSONObject(), "SUCCESS"));

        verify(knowledgeActionMapper, never()).updateById(any(KnowledgeActionEntity.class));
    }
}
