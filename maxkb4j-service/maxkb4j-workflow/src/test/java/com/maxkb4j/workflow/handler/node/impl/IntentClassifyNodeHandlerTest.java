package com.maxkb4j.workflow.handler.node.impl;

import com.maxkb4j.common.exception.ApiException;
import com.maxkb4j.model.service.IModelProviderService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.maxkb4j.workflow.consts.WorkflowConstants.MessageCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * 意图分类分支解析的回归测试：
 * <ul>
 *   <li>模型输出越界（序号超出分支范围）、解析失败（无有效整数）时按失败处理抛出 ApiException，
 *   而非写入 null BRANCH_ID 导致下游被静默 SKIP（BUG-48）；</li>
 *   <li>结果中先出现越界序号时跳过并取首个合法序号；</li>
 *   <li>parse 仅保留文本中的合法整数。</li>
 * </ul>
 */
class IntentClassifyNodeHandlerTest {

    private final IModelProviderService modelFactory = mock(IModelProviderService.class);
    private final IntentClassifyNodeHandler handler = new IntentClassifyNodeHandler(modelFactory);

    private Map<Integer, String> branches() {
        return Map.of(0, "branch-a", 1, "branch-b", 2, "branch-c");
    }

    @Test
    void resolveBranchId_returnsBranchIdForValidIndex() {
        assertThat(handler.resolveBranchId(branches(), List.of(1))).isEqualTo("branch-b");
    }

    @Test
    void resolveBranchId_picksFirstValidWhenOutOfRangeIdPresent() {
        // 模型输出 "5,0"：5 越界跳过，0 命中首个分支
        assertThat(handler.resolveBranchId(branches(), List.of(5, 0))).isEqualTo("branch-a");
    }

    @Test
    void resolveBranchId_throwsWhenIndexOutOfRange() {
        assertThatThrownBy(() -> handler.resolveBranchId(branches(), List.of(3)))
                .isInstanceOf(ApiException.class)
                .hasMessage(MessageCode.INTENT_NO_MATCH);
    }

    @Test
    void resolveBranchId_throwsWhenNothingParsed() {
        assertThatThrownBy(() -> handler.resolveBranchId(branches(), List.of()))
                .isInstanceOf(ApiException.class)
                .hasMessage(MessageCode.INTENT_NO_MATCH);
    }

    @Test
    void resolveBranchId_throwsForNegativeIndex() {
        assertThatThrownBy(() -> handler.resolveBranchId(branches(), List.of(-1)))
                .isInstanceOf(ApiException.class)
                .hasMessage(MessageCode.INTENT_NO_MATCH);
    }

    @Test
    void parse_keepsOnlyIntegersFromModelOutput() {
        // "2xyz" 无法整段解析为整数，与 "abc" 一样被丢弃
        assertThat(handler.parse("abc, 1, 2xyz, ,3")).containsExactly(1, 3);
    }

    @Test
    void parse_returnsEmptyForGarbageOutput() {
        assertThat(handler.parse(null)).isEmpty();
        assertThat(handler.parse("无法判断")).isEmpty();
    }
}
