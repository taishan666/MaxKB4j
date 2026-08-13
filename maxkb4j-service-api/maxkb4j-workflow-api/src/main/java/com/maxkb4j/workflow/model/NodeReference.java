package com.maxkb4j.workflow.model;

import java.util.List;
import java.util.Optional;

/**
 * 工作流节点引用类型化对象（第 3 期引用类型化）。
 * <p>引用在持久化/配置中以 {@code [nodeId, field]} 列表形式存在；
 * {@link #parse(List)} 一次性完成校验与转换，非法引用显式返回
 * {@link Optional#empty()}，不再以隐式 null 在调用链中传播。</p>
 *
 * @param nodeId 被引用节点 ID（或作用域名 global/chat/loop）
 * @param field  节点下的字段名
 */
public record NodeReference(String nodeId, String field) {

    /**
     * 解析 List 形式引用；null、元素不足两个或含空白元素时返回 empty。
     */
    public static Optional<NodeReference> parse(List<String> reference) {
        if (reference == null || reference.size() < 2) {
            return Optional.empty();
        }
        String nodeId = reference.get(0);
        String field = reference.get(1);
        if (nodeId == null || nodeId.isBlank() || field == null || field.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new NodeReference(nodeId, field));
    }

    /**
     * 解析 Object 形式引用（内部须为 {@code List<String>}），非法形态返回 empty。
     */
    public static Optional<NodeReference> parse(Object value) {
        if (value instanceof List<?> list && list.stream().allMatch(String.class::isInstance)) {
            @SuppressWarnings("unchecked")
            List<String> strings = (List<String>) list;
            return parse(strings);
        }
        return Optional.empty();
    }
}