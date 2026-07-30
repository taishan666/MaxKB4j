package com.maxkb4j.common.domain;

import lombok.Data;

/**
 * RAG 注入内容的最小契约。
 *
 * <p>RagContentInjector 仅依赖标题与正文两个属性完成 prompt 拼装，
 * 通过该接口与具体业务 VO（如 knowledge 的 {@code ParagraphRagVO}）解耦，
 * 使 core 无需依赖任何业务 api 模块。
 */
@Data
public abstract class RagContent {
    private String title;
    private String content;
    private Integer position;
    private String documentName;
}
