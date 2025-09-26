package com.tarzan.maxkb4j.common.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Objects;

// 自定义的序列化器类
public class NullRootSerializer extends JsonSerializer<String> {
    final static String ROOT_NAME = "root";

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(Objects.requireNonNullElse(value, ROOT_NAME));
    }
}