package com.maxkb4j.model.integration.embedding;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.model.form.BaseField;
import com.maxkb4j.model.form.SingleSelectField;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.maxkb4j.model.consts.ModelConstants.ParamKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class EmbeddingModelParamsTest {

    private final List<BaseField> form = new EmbeddingModelParams().toForm();

    @Test
    void containsSingleDimensionsField() {
        assertEquals(1, form.size());
        BaseField dimensions = form.get(0);
        assertEquals(ParamKey.DIMENSIONS, dimensions.getField());
        assertEquals("SingleSelect", dimensions.getInput_type());
        assertInstanceOf(SingleSelectField.class, dimensions);
    }

    @Test
    void dimensionsOptions_coverSupportedVectorSizes() {
        SingleSelectField dimensions = (SingleSelectField) form.get(0);
        List<Object> values = dimensions.getOption_list().stream()
                .map(option -> option.get("value")).toList();
        // Map.of 无序，选项集合按值断言而非顺序
        assertEquals(java.util.Set.of(1536, 1024, 768, 512), new java.util.HashSet<>(values));
        assertEquals("label", dimensions.getText_field());
        assertEquals("value", dimensions.getValue_field());
    }

    @Test
    void defaultDimension_is1024() {
        SingleSelectField dimensions = (SingleSelectField) form.get(0);
        assertEquals(1024, dimensions.getDefault_value());
    }

    @Test
    void optionList_pairsLabelWithValue() {
        SingleSelectField dimensions = (SingleSelectField) form.get(0);
        for (JSONObject option : dimensions.getOption_list()) {
            assertEquals(Integer.parseInt(((String) option.get("label")).replace(",", "")),
                    option.get("value"));
        }
    }
}
