package com.maxkb4j.model.custom.credential;

import com.maxkb4j.model.form.BaseField;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.maxkb4j.model.consts.ModelConstants.CredentialField;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelCredentialFormTest {

    @Test
    void apiKeyOnly_whenBaseUrlHidden() {
        List<BaseField> form = new ModelCredentialForm(false, true).toForm();

        assertEquals(1, form.size());
        assertEquals(CredentialField.API_KEY, form.get(0).getField());
        assertEquals("TextInput", form.get(0).getInput_type());
        assertTrue(form.get(0).getRequired());
    }

    @Test
    void bothFields_whenBaseUrlAndApiKeyShown() {
        List<BaseField> form = new ModelCredentialForm(true, true).toForm();

        assertEquals(List.of(CredentialField.BASE_URL, CredentialField.API_KEY),
                form.stream().map(BaseField::getField).toList());
        assertNull(form.get(0).getDefault_value());
    }

    @Test
    void baseUrlCarriesDefaultValue_whenProvided() {
        List<BaseField> form = new ModelCredentialForm(true, "https://default.example.com/v1").toForm();

        assertEquals(2, form.size());
        assertEquals("https://default.example.com/v1", form.get(0).getDefault_value());
        assertEquals("API 域名", form.get(0).getLabel());
    }
}
