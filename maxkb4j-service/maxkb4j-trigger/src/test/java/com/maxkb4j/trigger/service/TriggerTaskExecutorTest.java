package com.maxkb4j.trigger.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.maxkb4j.application.dto.ChatResponse;
import com.maxkb4j.application.service.IApplicationChatService;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.dto.ChatState;
import com.maxkb4j.common.domain.vo.Answer;
import com.maxkb4j.common.enums.ChatSource;
import com.maxkb4j.common.enums.ChatUserType;
import com.maxkb4j.tool.service.IToolExecuteService;
import com.maxkb4j.tool.service.IToolService;
import com.maxkb4j.trigger.entity.EventTriggerTaskEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 触发器执行应用任务的回归测试：chatUserId 必须取触发器 id（稳定身份），
 * 同一触发器的执行共享同一统计行，accessNum 每日上限才能对触发器生效；
 * 若依赖 ChatState 懒生成随机 id，统计表会每次执行新增一行且客户数趋势虚高。
 */
class TriggerTaskExecutorTest {

    private IEventTriggerTaskService taskService;
    private IEventTriggerTaskRecordService recordService;
    private IApplicationChatService applicationChatService;
    private TriggerTaskExecutor executor;

    @BeforeEach
    void setUp() {
        taskService = mock(IEventTriggerTaskService.class);
        recordService = mock(IEventTriggerTaskRecordService.class);
        applicationChatService = mock(IApplicationChatService.class);
        executor = new TriggerTaskExecutor(
                taskService,
                recordService,
                applicationChatService,
                mock(IToolService.class),
                mock(IToolExecuteService.class));
    }

    @Test
    void applicationTaskCarriesStableTriggerUserId() {
        EventTriggerTaskEntity task = new EventTriggerTaskEntity();
        task.setId("task-1");
        task.setTriggerId("trigger-1");
        task.setSourceType("APPLICATION");
        task.setSourceId("app-1");
        JSONObject parameter = new JSONObject();
        parameter.put("question", "hi");
        task.setParameter(parameter);
        when(taskService.list(any(Wrapper.class))).thenReturn(List.of(task));
        when(applicationChatService.chatOpen("app-1", true)).thenReturn("chat-1");
        when(applicationChatService.chatMessage(any(ChatParams.class), any(ChatState.class), any()))
                .thenReturn(new ChatResponse(
                        List.of(Answer.builder().content("done").build()), new JSONObject()));

        executor.execute("trigger-1", new JSONObject());

        ArgumentCaptor<ChatState> captor = ArgumentCaptor.forClass(ChatState.class);
        verify(applicationChatService).chatOpen(eq("app-1"), eq(true));
        verify(applicationChatService).chatMessage(any(ChatParams.class), captor.capture(), any());
        assertEquals("app-1", captor.getValue().getAppId());
        assertEquals("trigger-1", captor.getValue().getChatUserId());
        assertEquals(ChatUserType.TRIGGER_USER, captor.getValue().getChatUserType());
        assertEquals(ChatSource.TRIGGER, captor.getValue().getSource());
    }
}
