package com.maxkb4j.workflow.handler.node;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.node.AbsNode;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.PartialThinking;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.BeforeToolExecution;
import dev.langchain4j.service.tool.ToolExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.maxkb4j.workflow.consts.WorkflowConstants.NodeField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 流式写入的结算短路行为回归测试：
 * <ul>
 *   <li>future 被引擎超时结算（取消）后，所有流回调短路——不再累积文本、不再写入
 *   token 用量、不再触发完成/失败（配合 {@code NodeLifecycleExecutor} 的超时结算，
 *   避免取消不中断底层流与双重回调）；</li>
 *   <li>未结算的正常流不受短路保护影响，照常累积并完成。</li>
 * </ul>
 */
class LLMStreamNodeHandlerTest {

    private final TokenStream tokenStream = mock(TokenStream.class);
    private final IWorkflow workflow = mock(IWorkflow.class);
    private final AbsNode node = new AbsNode("n1", new JSONObject()) {
    };

    private CompletableFuture<NodeResult> future;
    private Consumer<PartialThinking> onPartialThinking;
    private Consumer<BeforeToolExecution> beforeToolExecution;
    private Consumer<ToolExecution> onToolExecuted;
    private Consumer<String> onPartialResponse;
    private Consumer<ChatResponse> onCompleteResponse;
    private Consumer<Throwable> onError;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        when(tokenStream.onPartialThinking(any())).thenReturn(tokenStream);
        when(tokenStream.beforeToolExecution(any())).thenReturn(tokenStream);
        when(tokenStream.onToolExecuted(any())).thenReturn(tokenStream);
        when(tokenStream.onPartialResponse(any())).thenReturn(tokenStream);
        when(tokenStream.onCompleteResponse(any())).thenReturn(tokenStream);
        when(tokenStream.onError(any())).thenReturn(tokenStream);

        LLMStreamNodeHandler handler = new LLMStreamNodeHandler(null, null) {
        };
        future = handler.writeContextStreamAsync(
                LLMStreamNodeHandler.StreamOptions.of(true, false, true), tokenStream, workflow, node);

        ArgumentCaptor<Consumer<PartialThinking>> thinking = ArgumentCaptor.forClass((Class) Consumer.class);
        ArgumentCaptor<Consumer<BeforeToolExecution>> beforeTool = ArgumentCaptor.forClass((Class) Consumer.class);
        ArgumentCaptor<Consumer<ToolExecution>> toolExecuted = ArgumentCaptor.forClass((Class) Consumer.class);
        ArgumentCaptor<Consumer<String>> partial = ArgumentCaptor.forClass((Class) Consumer.class);
        ArgumentCaptor<Consumer<ChatResponse>> complete = ArgumentCaptor.forClass((Class) Consumer.class);
        ArgumentCaptor<Consumer<Throwable>> error = ArgumentCaptor.forClass((Class) Consumer.class);
        verify(tokenStream).onPartialThinking(thinking.capture());
        verify(tokenStream).beforeToolExecution(beforeTool.capture());
        verify(tokenStream).onToolExecuted(toolExecuted.capture());
        verify(tokenStream).onPartialResponse(partial.capture());
        verify(tokenStream).onCompleteResponse(complete.capture());
        verify(tokenStream).onError(error.capture());
        this.onPartialThinking = thinking.getValue();
        this.beforeToolExecution = beforeTool.getValue();
        this.onToolExecuted = toolExecuted.getValue();
        this.onPartialResponse = partial.getValue();
        this.onCompleteResponse = complete.getValue();
        this.onError = error.getValue();
    }

    @Test
    void settledStreamShouldSeverAllCallbacks() {
        // 模拟引擎超时结算：NodeLifecycleExecutor.timeout 会向上游取消本 future
        assertThat(future.cancel(true)).isTrue();

        // 结算后的流回调一律短路：不抛异常、不累积、不推送
        onPartialThinking.accept(new PartialThinking("thinking"));
        beforeToolExecution.accept(mock(BeforeToolExecution.class));
        onToolExecuted.accept(mock(ToolExecution.class));
        onPartialResponse.accept("late token");

        assertThat(future).isCancelled();
        assertThat(node.getDetail())
                .doesNotContainKeys(NodeField.MESSAGE_TOKENS, NodeField.ANSWER_TOKENS, NodeField.ANSWER);

        // 迟到的完成/失败同样不得结算 future 或写入 token 用量
        onCompleteResponse.accept(completeResponse(new TokenUsage(5, 7)));
        onError.accept(new IllegalStateException("late boom"));

        assertThat(future).isCancelled();
        assertThat(node.getDetail())
                .doesNotContainKeys(NodeField.MESSAGE_TOKENS, NodeField.ANSWER_TOKENS);
    }

    @Test
    void unSettledStreamShouldAccumulateAndComplete() {
        onPartialResponse.accept("hello ");
        onPartialResponse.accept("world");
        onPartialThinking.accept(new PartialThinking("reason"));

        onCompleteResponse.accept(completeResponse(new TokenUsage(5, 7)));

        assertThat(future).isCompleted();
        NodeResult result = future.join();
        assertThat(result.getNodeVariable())
                .containsEntry(NodeField.ANSWER, "hello world")
                .containsEntry(NodeField.REASONING_CONTENT, "reason");
        assertThat(node.getDetail())
                .containsEntry(NodeField.MESSAGE_TOKENS, 5)
                .containsEntry(NodeField.ANSWER_TOKENS, 7);
    }

    private ChatResponse completeResponse(TokenUsage tokenUsage) {
        ChatResponse response = mock(ChatResponse.class);
        when(response.tokenUsage()).thenReturn(tokenUsage);
        return response;
    }
}
