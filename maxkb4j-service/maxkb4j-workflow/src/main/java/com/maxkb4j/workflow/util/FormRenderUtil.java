package com.maxkb4j.workflow.util;

import com.alibaba.fastjson.JSONObject;

import static com.maxkb4j.workflow.consts.WorkflowConstants.FormField;

/**
 * 表单/用户选择节点渲染字符串组装工具。
 * <p>
 * 将节点运行时详情中的表单字段组装为以渲染标签（form_render / card_selection_render）
 * 包裹的 JSON 字符串，供前端解析渲染交互组件。
 */
public final class FormRenderUtil {

    private FormRenderUtil() {
    }

    /**
     * 组装以渲染标签包裹的表单设置字符串。
     *
     * @param nodeDetail    节点运行时详情
     * @param renderTag     渲染标签（form_render / card_selection_render）
     * @return 形如 {@code <renderTag>{...}</renderTag>} 的字符串
     */
    public static String buildFormRender(JSONObject nodeDetail, String renderTag) {
        JSONObject formSetting = new JSONObject();
        formSetting.put(FormField.FORM_FIELD_LIST, nodeDetail.getJSONArray(FormField.FORM_FIELD_LIST));
        formSetting.put(FormField.IS_SUBMIT, nodeDetail.getBooleanValue(FormField.IS_SUBMIT));
        formSetting.put(FormField.FORM_DATA, nodeDetail.getJSONObject(FormField.FORM_DATA));
        return "<" + renderTag + ">" + formSetting + "</" + renderTag + ">";
    }
}
