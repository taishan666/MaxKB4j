package com.maxkb4j.workflow.model;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;
import static com.maxkb4j.workflow.consts.WorkflowConstants.ModelField;

@Data
public class ModelConfig {
    private String modelId;
    private JSONObject modelParamsSetting;

    /**
     * 将引用字段值安全转换为 ModelConfig。
     * <p>
     * 工作流中引用的模型配置来源于节点/全局上下文的 {@code Map<String, Object>}，
     * 经过 JSON 反序列化后通常是 {@code LinkedHashMap} 或 fastjson {@code JSONObject}，
     * 而不是 {@code ModelConfig} 实例，直接强转会抛出 {@link ClassCastException}。
     * 这里按字段重建，同时保证 {@code modelParamsSetting} 为可变的 {@link JSONObject}。
     *
     * @param value 引用字段原始值，可能为 null
     * @return 转换后的 ModelConfig；输入为 null 或无法识别时返回 null
     */
    public static ModelConfig from(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof ModelConfig config) {
            return config;
        }
        Map<String, Object> map = asStringKeyedMap(value);
        if (map == null) {
            return null;
        }
        ModelConfig config = new ModelConfig();
        Object modelId = map.get(ModelField.MODEL_ID);
        config.setModelId(modelId == null ? null : modelId.toString());
        config.setModelParamsSetting(toJSONObject(map.get(ModelField.MODEL_PARAMS_SETTING)));
        return config;
    }

    private static Map<String, Object> asStringKeyedMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>(map.size());
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return result;
        }
        return null;
    }

    private static JSONObject toJSONObject(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> map) {
            JSONObject jsonObject = new JSONObject(map.size());
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                jsonObject.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return jsonObject;
        }
        return null;
    }
}
