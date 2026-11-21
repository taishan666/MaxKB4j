package com.maxkb4j.model.custom.params;

import com.maxkb4j.model.form.BaseField;
import com.maxkb4j.model.form.SliderField;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.maxkb4j.model.consts.ModelConstants.ParamKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiChatModelParamsTest {

    private final List<BaseField> form = new OpenAiChatModelParams().toForm();

    @Test
    void containsTemperatureMaxTokensAndThinkingSwitch() {
        assertEquals(List.of(ParamKey.TEMPERATURE, ParamKey.MAX_TOKENS, ParamKey.ENABLE_THINKING),
                form.stream().map(BaseField::getField).toList());
    }

    @Test
    void temperatureSlider_hasRangeAndDefault() {
        SliderField temperature = (SliderField) form.get(0);
        assertEquals(0.0F, temperature.getAttrs().getFloat("min"));
        assertEquals(1.5F, temperature.getAttrs().getFloat("max"));
        assertEquals(0.1F, temperature.getAttrs().getFloat("step"));
        assertEquals(1.0F, ((Number) temperature.getDefault_value()).floatValue());
        assertTrue(temperature.getRequired());
    }

    @Test
    void maxTokensSlider_defaultsTo1024() {
        SliderField maxTokens = (SliderField) form.get(1);
        assertEquals(1.0F, maxTokens.getAttrs().getFloat("min"));
        assertEquals(1024000.0F, maxTokens.getAttrs().getFloat("max"));
        assertEquals(1024, ((Number) maxTokens.getDefault_value()).intValue());
    }

    @Test
    void thinkingSwitch_defaultsToTrue() {
        BaseField thinking = form.get(2);
        assertEquals("SwitchInput", thinking.getInput_type());
        assertInstanceOf(Boolean.class, thinking.getDefault_value());
        assertTrue((Boolean) thinking.getDefault_value());
    }
}
