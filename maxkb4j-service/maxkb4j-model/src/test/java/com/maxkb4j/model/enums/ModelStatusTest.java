package com.maxkb4j.model.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ModelStatusTest {

    @Test
    void getByKey_resolvesAllDeclaredStatuses() {
        assertSame(ModelStatus.SUCCESS, ModelStatus.getByKey("SUCCESS"));
        assertSame(ModelStatus.ERROR, ModelStatus.getByKey("ERROR"));
        assertSame(ModelStatus.DOWNLOAD, ModelStatus.getByKey("DOWNLOAD"));
        assertSame(ModelStatus.PAUSE_DOWNLOAD, ModelStatus.getByKey("PAUSE_DOWNLOAD"));
    }

    @Test
    void getByKey_unknownOrNull_returnsNull() {
        assertNull(ModelStatus.getByKey("PAUSED"));
        assertNull(ModelStatus.getByKey(null));
    }

    @Test
    void keyMatchesEnumName_andDisplayNamePresent() {
        for (ModelStatus status : ModelStatus.values()) {
            assertEquals(status.name(), status.getKey());
        }
        assertEquals("可用", ModelStatus.SUCCESS.getName());
        assertEquals("暂停下载", ModelStatus.PAUSE_DOWNLOAD.getName());
    }
}
