package com.maxkb4j.common.util;

import java.util.regex.Pattern;

/**
 * 聊天内容渲染标签的统一管理：{@code <tool_calls_render>} / {@code <form_render>}。
 * 历史消息转换（{@code MessageConverter}）与最终答案清理（{@code AbstractChatStreamNodeHandler}）共用，
 * 避免同一正则在多处重复定义。
 *
 * @author tarzan
 */
public final class RenderTags {

    public static final Pattern TOOL_CALLS_RENDER =
            Pattern.compile("<tool_calls_render>(.*?)</tool_calls_render>", Pattern.DOTALL);

    public static final Pattern FORM_RENDER =
            Pattern.compile("<form_render>(.*?)</form_render>", Pattern.DOTALL);

    private RenderTags() {
    }

    /**
     * 移除 {@code <tool_calls_render>} 标签内容；null 安全（null -> ""）。
     *
     * @param text 原始文本，可为 null
     * @return 清理后的文本
     */
    public static String stripToolCallsRender(String text) {
        return text == null ? "" : TOOL_CALLS_RENDER.matcher(text).replaceAll("");
    }

    /**
     * 是否包含 {@code <form_render>} 表单渲染标签。
     * 此类消息为 UI 控件，不应进入 LLM 上下文。
     *
     * @param text 原始文本，可为 null
     * @return true 表示包含表单渲染标签
     */
    public static boolean containsFormRender(String text) {
        return text != null && FORM_RENDER.matcher(text).find();
    }
}
