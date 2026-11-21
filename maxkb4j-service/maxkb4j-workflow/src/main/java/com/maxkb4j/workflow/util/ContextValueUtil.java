package com.maxkb4j.workflow.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 上下文/详情取值的安全类型转换工具
 * <p>
 * 恢复执行时（{@code NodeStateLoader#loadNodeState}），节点上下文与详情来自
 * 持久化 JSON 反序列化的结果：列表为 {@code JSONArray}（元素 {@code JSONObject}），
 * 对象为 {@code JSONObject}。直接强转 {@code List<POJO>} 会因泛型擦除通过编译与
 * 运行期 cast，但在迭代取元素时抛 {@code ClassCastException}。
 * 本工具统一把此类"JSON 形态"的值重建为目标 POJO 列表，兼容运行期原始 POJO 值。
 * </p>
 */
public final class ContextValueUtil {

    private ContextValueUtil() {
    }

    /**
     * 将上下文/详情中的列表值安全转换为目标类型列表
     * <p>
     * 兼容两种输入形态：正常执行写入的 {@code List<POJO>}（原样保留元素），
     * 以及恢复执行写入的 {@code JSONArray}（元素经 fastjson 重建为 POJO）。
     * null 或非列表输入返回空列表，避免调用方 NPE。
     * </p>
     *
     * @param value       上下文/详情中的原始值，可能为 null
     * @param elementType 目标元素类型
     * @return 转换后的列表，不含 null 元素；输入为 null 或非列表时为空列表
     */
    public static <T> List<T> toList(Object value, Class<T> elementType) {
        List<T> result = new ArrayList<>();
        if (!(value instanceof List<?> source)) {
            return result;
        }
        for (Object element : source) {
            if (element != null) {
                result.add(convertElement(element, elementType));
            }
        }
        return result;
    }

    /**
     * 单元素转换：已是目标类型直接复用，JSON 形态（JSONObject/Map）经 fastjson 重建
     */
    private static <T> T convertElement(Object element, Class<T> elementType) {
        if (elementType.isInstance(element)) {
            return elementType.cast(element);
        }
        if (element instanceof JSONObject) {
            return ((JSONObject) element).toJavaObject(elementType);
        }
        if (element instanceof Map) {
            @SuppressWarnings("unchecked")
            JSONObject jsonObject = new JSONObject((Map<String, Object>) element);
            return jsonObject.toJavaObject(elementType);
        }
        return JSON.parseObject(JSON.toJSONString(element), elementType);
    }
}
