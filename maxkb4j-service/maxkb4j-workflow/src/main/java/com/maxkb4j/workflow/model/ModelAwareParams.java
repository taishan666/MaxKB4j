package com.maxkb4j.workflow.model;

import com.alibaba.fastjson.JSONObject;

import java.util.List;

/**
 * 节点参数中模型配置相关字段的统一访问接口。
 *
 * <p>工作流中各类节点（LLM、Question、IntentClassify、NL2Sql、ImageUnderstand 等）
 * 的 NodeParams 都包含相同的模型配置字段（modelId、modelIdType、modelParamsSetting、
 * modelIdReference）。通过该接口，可在 {@code AbsNodeHandler} 中以统一方式解析模型配置，
 * 消除各 Handler 中重复的 "判断 reference 类型并重建 ModelConfig" 逻辑。</p>
 *
 * <p>实现类只需声明对应字段并配合 lombok {@code @Data} 生成的 getter 即可，
 * 无需手写方法体。</p>
 */
public interface ModelAwareParams {

    /**
     * 模型 ID（直接指定时使用）
     */
    String getModelId();

    /**
     * 模型 ID 来源类型：{@code "reference"} 表示从引用字段获取，其他值表示直接使用 modelId
     */
    String getModelIdType();

    /**
     * 模型参数配置（temperature、topP 等）
     */
    JSONObject getModelParamsSetting();

    /**
     * 模型 ID 引用字段路径（当 {@link #getModelIdType()} 为 {@code "reference"} 时使用）
     */
    List<String> getModelIdReference();
}
