package com.maxkb4j.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回归测试：聊天内容渲染标签的剥离与识别流程。
 */
class RenderTagsTest {

    @Test
    void stripToolCallsRender_removesTagContent() {
        assertThat(RenderTags.stripToolCallsRender("<tool_calls_render>x</tool_calls_render>abc"))
                .isEqualTo("abc");
    }

    @Test
    void stripToolCallsRender_handlesMultilineWithDotall() {
        String text = "<tool_calls_render>\nline1\nline2\n</tool_calls_render>tail";
        assertThat(RenderTags.stripToolCallsRender(text)).isEqualTo("tail");
    }

    @Test
    void stripToolCallsRender_isNullSafe() {
        assertThat(RenderTags.stripToolCallsRender(null)).isEqualTo("");
    }

    @Test
    void stripToolCallsRender_keepsPlainContent() {
        assertThat(RenderTags.stripToolCallsRender("plain text")).isEqualTo("plain text");
    }

    @Test
    void containsFormRender_detectsTag() {
        assertThat(RenderTags.containsFormRender("<form_render>...</form_render>")).isTrue();
    }

    @Test
    void containsFormRender_isNullSafeAndNegative() {
        assertThat(RenderTags.containsFormRender(null)).isFalse();
        assertThat(RenderTags.containsFormRender("plain")).isFalse();
    }
}