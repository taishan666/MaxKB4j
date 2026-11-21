package com.maxkb4j.model.enums;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ModelTypeTest {

    @Test
    void getByKey_resolvesAllDeclaredTypes() {
        for (ModelType type : ModelType.values()) {
            assertSame(type, ModelType.getByKey(type.getKey()));
        }
    }

    @Test
    void getByKey_unknownOrNull_returnsNull() {
        assertNull(ModelType.getByKey("UNKNOWN"));
        assertNull(ModelType.getByKey("llm"));
        assertNull(ModelType.getByKey(null));
    }

    @Test
    void getModelTypeList_containsAllValuesInDeclarationOrder() {
        List<ModelType> list = ModelType.getModelTypeList();
        assertEquals(ModelType.values().length, list.size());
        assertEquals(List.of(ModelType.values()), list);
    }

    @Test
    void keyMatchesEnumName_andDisplayNamePresent() {
        for (ModelType type : ModelType.values()) {
            assertEquals(type.name(), type.getKey());
        }
        assertEquals("大语言模型", ModelType.LLM.getName());
        assertEquals("向量模型", ModelType.EMBEDDING.getName());
    }
}
