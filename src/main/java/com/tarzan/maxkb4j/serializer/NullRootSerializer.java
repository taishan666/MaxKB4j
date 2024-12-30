package com.tarzan.maxkb4j.serializer;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.UUID;

// 自定义的序列化器类
public class NullRootSerializer extends JsonSerializer<UUID> {
    final static String ROOT_NAME = "root";
    @Override
    public void serialize(UUID value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeString(ROOT_NAME);
        } else {
            gen.writeString(value.toString());
        }
    }
}