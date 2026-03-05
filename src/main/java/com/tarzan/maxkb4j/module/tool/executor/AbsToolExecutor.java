package com.tarzan.maxkb4j.module.tool.executor;

import dev.langchain4j.internal.Json;
import dev.langchain4j.internal.Utils;
import dev.langchain4j.service.tool.ToolExecutor;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbsToolExecutor implements ToolExecutor {

    private static final Pattern TRAILING_COMMA_PATTERN = Pattern.compile(",(\\s*[}\\]])");
    private static final Pattern LEADING_TRAILING_QUOTE_PATTERN = Pattern.compile("^\"|\"$");
    private static final Pattern ESCAPED_QUOTE_PATTERN = Pattern.compile("\\\\\"");
    private static final Type MAP_TYPE = new ParameterizedType() {
        public Type[] getActualTypeArguments() {
            return new Type[]{String.class, Object.class};
        }

        public Type getRawType() {
            return Map.class;
        }

        public Type getOwnerType() {
            return null;
        }
    };


    static Map<String, Object> argumentsAsMap(String arguments) {
        if (Utils.isNullOrBlank(arguments)) {
            return Map.of();
        } else {
            try {
                return  Json.fromJson(arguments, MAP_TYPE);
            } catch (Exception var3) {
                String normalizedArguments = removeTrailingComma(normalizeJsonString(arguments));
                return Json.fromJson(normalizedArguments, MAP_TYPE);
            }
        }
    }

    static String removeTrailingComma(String json) {
        if (json != null && !json.isEmpty()) {
            Matcher matcher = TRAILING_COMMA_PATTERN.matcher(json);
            return matcher.replaceAll("$1");
        } else {
            return json;
        }
    }

    static String normalizeJsonString(String arguments) {
        if (arguments != null && !arguments.isEmpty()) {
            Matcher leadingTrailingMatcher = LEADING_TRAILING_QUOTE_PATTERN.matcher(arguments);
            String normalizedJson = leadingTrailingMatcher.replaceAll("");
            Matcher escapedQuoteMatcher = ESCAPED_QUOTE_PATTERN.matcher(normalizedJson);
            return escapedQuoteMatcher.replaceAll("\"");
        } else {
            return arguments;
        }
    }
}
